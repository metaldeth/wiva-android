package com.viwa.android.services.payment

import com.viwa.android.domain.model.AqsiConfig
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.model.CardPaymentResult
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaymentOrchestratorTest {

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
    fun pay_whenAqsiMockEnabled_returnsSuccessWithoutUsb() =
        runBlocking {
            val aqsi = RecordingAqsi()
            val orch =
                CardPaymentOrchestrator(
                    aqsiRepository = aqsi,
                    cardPaymentMockModeRepository = FakeMockModeRepo(CardPaymentMockMode.Aqsi),
                    paymentEventLogger = CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 1, false)
            assertEquals(CardPaymentResult.Success, r)
            assertEquals(0, aqsi.payCalls)
        }

    @Test
    fun pay_callsInitiatePaymentWithKopecks() =
        runBlocking {
            val aqsi = RecordingAqsi()
            val orch =
                CardPaymentOrchestrator(
                    aqsiRepository = aqsi,
                    cardPaymentMockModeRepository = FakeMockModeRepo(),
                    paymentEventLogger = CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, price = 15, productNumber = 0, sbp = false)
            assertTrue(r is CardPaymentResult.Success)
            assertEquals(1, aqsi.payCalls)
            assertEquals(1500, aqsi.lastKopecks)
        }

    @Test
    fun pay_aqsiApproved_mapsToSuccess() =
        runBlocking {
            val orch =
                CardPaymentOrchestrator(
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
                    RecordingAqsi(Result.success(AqsiPaymentResult.Declined("051"))),
                    FakeMockModeRepo(),
                    CardPaymentEventLogger(),
                )
            val r = orch.pay(TerminalProductType.Drink, 1, 0, false)
            assertTrue(r is CardPaymentResult.Failed)
            assertEquals("declined:051", (r as CardPaymentResult.Failed).reason)
        }

    @Test
    fun cancelActivePayment_duringAqsiPay_invokesCancelPayment() =
        runBlocking {
            val unblock = CompletableDeferred<Unit>()
            val payEntered = CompletableDeferred<Unit>()
            val aqsi = BlockingCancelAqsi(payEntered, unblock)
            val orch =
                CardPaymentOrchestrator(
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
}
