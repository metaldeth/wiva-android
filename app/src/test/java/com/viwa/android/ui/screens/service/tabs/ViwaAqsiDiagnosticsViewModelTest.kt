package com.viwa.android.ui.screens.service.tabs

import com.viwa.android.data.payment.aqsi.AqsiDiagnosticOperationKind
import com.viwa.android.data.payment.aqsi.AqsiDiagnosticOutcome
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSummary
import com.viwa.android.domain.model.AqsiConfig
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import com.viwa.android.services.payment.CardPaymentEventLogger
import com.viwa.android.services.payment.CardPaymentOrchestrator
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViwaAqsiDiagnosticsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RepoCard(
        var selected: CardPaymentMethod,
    ) : CardPaymentMethodRepository {
        override suspend fun getSelected(): CardPaymentMethod = selected

        override suspend fun setSelected(method: CardPaymentMethod) {
            this.selected = method
        }
    }

    private class RepoAqsi(
        var paymentResult: Result<AqsiPaymentResult> = Result.success(AqsiPaymentResult.Approved),
    ) : AqsiRepository {
        override suspend fun loadConfig(): AqsiConfig = AqsiConfig()

        override suspend fun saveConfig(config: AqsiConfig) {}

        override suspend fun testTcpConnection(): Result<Unit> = Result.success(Unit)

        override suspend fun initiatePayment(amountKopecks: Int): Result<AqsiPaymentResult> = paymentResult

        override suspend fun cancelPayment(): Result<Unit> = Result.success(Unit)
    }

    private class RepoMock(
        private var mode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    ) : CardPaymentMockModeRepository {
        override suspend fun getMode(): CardPaymentMockMode = mode

        override suspend fun setMode(mode: CardPaymentMockMode) {
            this.mode = mode
        }

        override suspend fun getOutcome(): CardPaymentMockOutcome = CardPaymentMockOutcome.Approved

        override suspend fun setOutcome(outcome: CardPaymentMockOutcome) = Unit
    }

    private fun createVm(
        method: CardPaymentMethod,
        holder: AqsiLastOperationSnapshotHolder = AqsiLastOperationSnapshotHolder(),
        aqsi: AqsiRepository = RepoAqsi(),
        orchestrator: CardPaymentOrchestrator = mockk(relaxed = true),
    ) = ViwaAqsiDiagnosticsViewModel(
        cardPaymentMethodRepository = RepoCard(method),
        cardPaymentMockModeRepository = RepoMock(),
        lastOperationSnapshotHolder = holder,
        aqsiRepository = aqsi,
        cardPaymentOrchestrator = orchestrator,
        paymentEventLogger = CardPaymentEventLogger(),
    )

    @Test
    fun enableAqsiMock_doesNotChangeSelectedMethod() =
        runBlocking {
            val repo = RepoCard(CardPaymentMethod.Aqsi)
            val vm =
                ViwaAqsiDiagnosticsViewModel(
                    cardPaymentMethodRepository = repo,
                    cardPaymentMockModeRepository = RepoMock(),
                    lastOperationSnapshotHolder = AqsiLastOperationSnapshotHolder(),
                    aqsiRepository = RepoAqsi(),
                    cardPaymentOrchestrator = mockk(relaxed = true),
                    paymentEventLogger = CardPaymentEventLogger(),
                )
            vm.enableAqsiMock()
            assertEquals(CardPaymentMethod.Aqsi, repo.selected)
            assertEquals(CardPaymentMockMode.Aqsi, vm.uiState.value.mockMode)
        }

    @Test
    fun runMockPaymentScenario_whenMockDisabled_doesNotCallOrchestrator() =
        runBlocking {
            val orch = mockk<CardPaymentOrchestrator>(relaxed = true)
            val vm = createVm(CardPaymentMethod.Aqsi, orchestrator = orch)
            vm.runMockPaymentScenario(CardPaymentMockOutcome.Approved)
            coVerify(exactly = 0) {
                orch.pay(any(), any(), any(), any())
            }
            assertTrue(vm.uiState.value.pennyTestBanner?.contains("mock", ignoreCase = true) == true)
            assertTrue(vm.uiState.value.pennyTestBannerIsError)
        }

    @Test
    fun refresh_showsCardMethodFromRepository() =
        runBlocking {
            val vm = createVm(CardPaymentMethod.Aqsi)
            assertTrue(vm.uiState.value.cardMethodLabel.contains("aQsi", ignoreCase = true))
        }

    @Test
    fun refresh_withPresetSnapshot_showsOutcomeLines() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            holder.update(
                AqsiLastOperationSummary(
                    timestampMillis = 1_700_000_000_000L,
                    operationKind = AqsiDiagnosticOperationKind.TCP_TEST,
                    outcome = AqsiDiagnosticOutcome.SUCCESS,
                    detailCode = "",
                ),
            )
            val vm = createVm(CardPaymentMethod.Aqsi, holder)
            val line = vm.uiState.value.lastOperationLine1
            assertTrue(line != null && line.contains("Тест TCP"))
        }

    @Test
    fun afterFakeOrderLikePayment_holderReadableByRefresh() =
        runBlocking {
            val holder = AqsiLastOperationSnapshotHolder()
            holder.update(
                AqsiLastOperationSummary(
                    timestampMillis = 42L,
                    operationKind = AqsiDiagnosticOperationKind.PAYMENT,
                    outcome = AqsiDiagnosticOutcome.DECLINED,
                    detailCode = "Z1",
                ),
            )
            val vm = createVm(CardPaymentMethod.Aqsi, holder)
            assertTrue(vm.uiState.value.lastOperationLine1?.contains("одобрено") == false)
            assertTrue(vm.uiState.value.lastOperationLine2?.contains("Z1") == true)
        }
}
