package com.viwa.android.services.payment

import com.viwa.android.domain.model.AqsiConfig
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.model.CardPaymentResult
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaymentOrchestratorTest {

    private class FakeCardMethodRepo(
        var selected: CardPaymentMethod,
    ) : CardPaymentMethodRepository {
        override suspend fun getSelected(): CardPaymentMethod = selected

        override suspend fun setSelected(method: CardPaymentMethod) {
            selected = method
        }
    }

    private class FakeMockModeRepo(
        private var mode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    ) : CardPaymentMockModeRepository {
        override suspend fun getMode(): CardPaymentMockMode = mode

        override suspend fun setMode(mode: CardPaymentMockMode) {
            this.mode = mode
        }

        override suspend fun getOutcome(): CardPaymentMockOutcome = CardPaymentMockOutcome.Approved

        override suspend fun setOutcome(outcome: CardPaymentMockOutcome) = Unit
    }

    private class RecordingAqsi(
        var payResult: Result<AqsiPaymentResult> = Result.success(AqsiPaymentResult.Approved),
    ) : AqsiRepository {
        var lastKopecks: Int? = null
        var payCalls = 0
        var cancelCalls = 0

        override suspend fun loadConfig(): AqsiConfig = AqsiConfig()

        override suspend fun saveConfig(config: AqsiConfig) {}

        override suspend fun testTcpConnection(): Result<Unit> = Result.success(Unit)

        override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> {
            payCalls++
            lastKopecks = amountKopecks
            return payResult
        }

        override suspend fun cancelPayment(): Result<Unit> {
            cancelCalls++
            return Result.success(Unit)
        }
    }

    private class BlockingCancelAqsi(
        private val payEntered: CompletableDeferred<Unit>,
        private val unblock: CompletableDeferred<Unit>,
    ) : AqsiRepository {
        var cancelCalls = 0

        override suspend fun loadConfig(): AqsiConfig = AqsiConfig()

        override suspend fun saveConfig(config: AqsiConfig) {}

        override suspend fun testTcpConnection(): Result<Unit> = Result.success(Unit)

        override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> {
            payEntered.complete(Unit)
            unblock.await()
            return Result.success(AqsiPaymentResult.Approved)
        }

        override suspend fun cancelPayment(): Result<Unit> {
            cancelCalls++
            return Result.success(Unit)
        }
    }

    @Test
    fun pay_whenPaxSelected_delegatesToSendSumDoesNotTouchAqsi() =
        runBlocking {
            val terminal = mockk<PaymentTerminalService>(relaxUnitFun = true)
            coEvery { terminal.sendSumToTerminal(any(), any(), any(), any()) } returns Unit
            coEvery { terminal.waitForPaymentResult(any()) } returns PaymentTerminalResult.Approved

            val aqsi = mockk<AqsiRepository>(relaxUnitFun = true)

            val orch =
                CardPaymentOrchestrator(
                    cardPaymentMethodRepository = FakeCardMethodRepo(CardPaymentMethod.Pax),
                    paymentTerminalService = terminal,
                    aqsiRepository = aqsi,
                    cardPaymentMockModeRepository = FakeMockModeRepo(),
                    paymentEventLogger = CardPaymentEventLogger(),
                )
            val r =
                orch.pay(
                    type = TerminalProductType.Drink,
                    price = 42,
                    productNumber = 3,
                    sbp = false,
                )
            assertEquals(CardPaymentResult.Success, r)
            coVerify(exactly = 1) {
                terminal.sendSumToTerminal(TerminalProductType.Drink, 42, 3, false)
            }
            coVerify(exactly = 0) { aqsi.initiatePayment(any()) }
        }

    @Test
    fun pay_whenTwoCanMockEnabled_returnsSuccessWithoutTerminal() =
        runBlocking {
            val terminal = mockk<PaymentTerminalService>(relaxUnitFun = true)
            val aqsi = mockk<AqsiRepository>(relaxUnitFun = true)
            val orch =
                CardPaymentOrchestrator(
                    cardPaymentMethodRepository = FakeCardMethodRepo(CardPaymentMethod.Pax),
                    paymentTerminalService = terminal,
                    aqsiRepository = aqsi,
                    cardPaymentMockModeRepository = FakeMockModeRepo(CardPaymentMockMode.TwoCan),
                    paymentEventLogger = CardPaymentEventLogger(),
                )

            val r = orch.pay(TerminalProductType.Drink, 1, 1, false)

            assertEquals(CardPaymentResult.Success, r)
            coVerify(exactly = 0) { terminal.sendSumToTerminal(any(), any(), any(), any()) }
        }

    @Test
    fun pay_whenAqsiSelected_callsInitiatePaymentWithKopecks() =
        runBlocking {
            val terminal = mockk<PaymentTerminalService>(relaxUnitFun = true)
            val aqsi = RecordingAqsi()
            val orch =
                CardPaymentOrchestrator(
                    cardPaymentMethodRepository = FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    paymentTerminalService = terminal,
                    aqsiRepository = aqsi,
                    cardPaymentMockModeRepository = FakeMockModeRepo(),
                    paymentEventLogger = CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, price = 15, productNumber = 0, sbp = false)
            assertTrue(r is CardPaymentResult.Success)
            coVerify(exactly = 0) { terminal.sendSumToTerminal(any(), any(), any(), any()) }
            assertEquals(1, aqsi.payCalls)
            assertEquals(1500, aqsi.lastKopecks)
        }

    @Test
    fun pay_aqsiApproved_mapsToSuccess() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    RecordingAqsi(Result.success(AqsiPaymentResult.Approved)),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            assertEquals(CardPaymentResult.Success, orch.pay(TerminalProductType.Drink, 1, 0, false))
        }

    @Test
    fun pay_aqsiDeclined_mapsToFailed() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    RecordingAqsi(Result.success(AqsiPaymentResult.Declined("051"))),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 0, false)
            assertTrue(r is CardPaymentResult.Failed)
            assertEquals("declined:051", (r as CardPaymentResult.Failed).reason)
        }

    @Test
    fun pay_aqsiCancelled_mapsToCardCancelled() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    RecordingAqsi(Result.success(AqsiPaymentResult.Cancelled)),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            assertEquals(CardPaymentResult.Cancelled, orch.pay(TerminalProductType.Drink, 1, 0, false))
        }

    @Test
    fun pay_aqsiErrorResult_mapsToFailed() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    RecordingAqsi(Result.success(AqsiPaymentResult.Error(safeMessage = "timeout"))),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 0, false)
            assertTrue(r is CardPaymentResult.Failed)
            assertEquals("timeout", (r as CardPaymentResult.Failed).reason)
        }

    @Test
    fun pay_aqsiResultFailure_mapsToFailed() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    RecordingAqsi(Result.failure(IllegalStateException("net"))),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 0, false)
            assertTrue(r is CardPaymentResult.Failed)
        }

    @Test
    fun pay_paxThrows_mapsToFailed() =
        runBlocking {
            val terminal = mockk<PaymentTerminalService>(relaxUnitFun = true)
            coEvery { terminal.sendSumToTerminal(any(), any(), any(), any()) } throws RuntimeException("terminal down")
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Pax),
                    terminal,
                    RecordingAqsi(),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 0, false)
            assertTrue(r is CardPaymentResult.Failed)
        }

    @Test
    fun cancelActivePayment_duringAqsiPay_invokesCancelPayment() =
        runBlocking {
            val unblock = CompletableDeferred<Unit>()
            val payEntered = CompletableDeferred<Unit>()
            val aqsi = BlockingCancelAqsi(payEntered, unblock)
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Aqsi),
                    mockk(relaxed = true),
                    aqsi,
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val payJob = launch { orch.pay(TerminalProductType.Drink, 1, 0, false) }
            payEntered.await()
            orch.cancelActivePayment()
            assertEquals(1, aqsi.cancelCalls)
            unblock.complete(Unit)
            payJob.join()
        }

    @Test
    fun cancelActivePayment_duringPaxPay_invokesCancelTransaction() =
        runBlocking {
            val unblock = CompletableDeferred<Unit>()
            val payEntered = CompletableDeferred<Unit>()
            val terminal = mockk<PaymentTerminalService>(relaxUnitFun = true)
            coEvery { terminal.sendSumToTerminal(any(), any(), any(), any()) } coAnswers {
                payEntered.complete(Unit)
                unblock.await()
                Unit
            }
            coEvery { terminal.waitForPaymentResult(any()) } returns PaymentTerminalResult.Approved
            val orch =
                CardPaymentOrchestrator(
                    FakeCardMethodRepo(CardPaymentMethod.Pax),
                    terminal,
                    RecordingAqsi(),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val payJob = launch { orch.pay(TerminalProductType.Drink, 1, 0, false) }
            payEntered.await()
            orch.cancelActivePayment()
            verify(exactly = 1) { terminal.cancelTransaction() }
            unblock.complete(Unit)
            payJob.join()
        }
}
