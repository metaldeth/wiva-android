package com.viwa.android.ui.screens.service.tabs

import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import com.viwa.android.hardware.controller.ControllerGateway
import com.viwa.android.hardware.controller.ControllerResponseEvent
import com.viwa.android.hardware.controller.RequestCommand
import com.viwa.android.hardware.controller.ResponseCommand
import com.viwa.android.services.payment.CardPaymentEventLogger
import com.viwa.android.services.payment.CardPaymentLogLane
import com.viwa.android.services.payment.CardPaymentOrchestrator
import com.viwa.android.services.payment.PaymentTerminalService
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViwaTwoCanPaymentSettingsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectTwoCan_setsPaxMethod() =
        runBlocking {
            val repo = FakeMethodRepo(CardPaymentMethod.Aqsi)
            val vm = createVm(repo = repo)

            vm.selectTwoCan()

            assertEquals(CardPaymentMethod.Pax, repo.selected)
            assertTrue(vm.uiState.value.isSelected)
        }

    @Test
    fun sendTestAmount_callsPaymentTerminalWithCardDrink() =
        runBlocking {
            val gateway = FakeGateway()
            val terminal = realTerminal(gateway)
            val vm = createVm(terminal = terminal)

            vm.sendTestAmount()

            assertEquals(RequestCommand.SendSumToPaymentTerminal, gateway.lastCommand)
            assertEquals(listOf(0, 1, 0, 1, 0, 0), gateway.lastSentPayload?.map { it.toInt() })
            assertFalse(vm.uiState.value.bannerIsError)
        }

    @Test
    fun simulateDiagnosticPaxStatus_sendsPaxStatusToControllerGateway_andLogsSystemLane() =
        runBlocking {
            val gateway = FakeGateway()
            val logger = CardPaymentEventLogger()
            val vm = createVm(gateway = gateway, paymentEventLogger = logger)

            vm.simulateDiagnosticPaxStatus(4)

            assertEquals(ResponseCommand.PaymentSystemsPaxStatus, gateway.lastResponse)
            assertEquals(4, gateway.lastPayload?.first()?.toInt())
            assertTrue(vm.uiState.value.banner?.contains("Диагностика") == true)
            assertEquals(CardPaymentLogLane.System, logger.entries.value.first().lane)
            assertTrue(logger.entries.value.first().message.contains("Диагностика"))
        }

    @Test
    fun cancelTransaction_updatesBanner() {
        val terminal = realTerminal(FakeGateway())
        val vm = createVm(terminal = terminal)

        vm.cancelTransaction()

        assertTrue(vm.uiState.value.banner?.contains("Отмена") == true)
    }

    @Test
    fun runMockPaymentScenario_whenMockDisabled_doesNotCallOrchestrator() =
        runBlocking {
            val orch = mockk<CardPaymentOrchestrator>(relaxed = true)
            val vm = createVm(orchestrator = orch)

            vm.runMockPaymentScenario(CardPaymentMockOutcome.Approved)

            coVerify(exactly = 0) {
                orch.pay(any(), any(), any(), any())
            }
            assertTrue(vm.uiState.value.banner?.contains("mock", ignoreCase = true) == true)
            assertTrue(vm.uiState.value.bannerIsError)
        }

    private fun createVm(
        repo: FakeMethodRepo = FakeMethodRepo(),
        mockRepo: FakeMockRepo = FakeMockRepo(),
        orchestrator: CardPaymentOrchestrator = mockk(relaxed = true),
        gateway: ControllerGateway = FakeGateway(),
        terminal: PaymentTerminalService = realTerminal(gateway),
        paymentEventLogger: CardPaymentEventLogger = CardPaymentEventLogger(),
    ): ViwaTwoCanPaymentSettingsViewModel =
        ViwaTwoCanPaymentSettingsViewModel(
            cardPaymentMethodRepository = repo,
            cardPaymentMockModeRepository = mockRepo,
            cardPaymentOrchestrator = orchestrator,
            paymentTerminalService = terminal,
            controllerGateway = gateway,
            paymentEventLogger = paymentEventLogger,
        )

    private fun realTerminal(gateway: ControllerGateway): PaymentTerminalService =
        PaymentTerminalService(
            controller = gateway,
            telemetry = mockk(relaxed = true),
            scope = CoroutineScope(mainDispatcher),
            configRepository =
                object : ConfigRepository {
                    override suspend fun get(key: String): String? = null

                    override suspend fun set(key: String, value: String) = Unit

                    override suspend fun delete(key: String) = Unit

                    override suspend fun getJson(key: String): String? = null

                    override suspend fun setJson(key: String, json: String) = Unit
                },
            paymentEventLogger = CardPaymentEventLogger(),
        )

    private class FakeMethodRepo(
        var selected: CardPaymentMethod = CardPaymentMethod.Pax,
    ) : CardPaymentMethodRepository {
        override suspend fun getSelected(): CardPaymentMethod = selected

        override suspend fun setSelected(method: CardPaymentMethod) {
            selected = method
        }
    }

    private class FakeMockRepo(
        var mode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    ) : CardPaymentMockModeRepository {
        override suspend fun getMode(): CardPaymentMockMode = mode

        override suspend fun setMode(mode: CardPaymentMockMode) {
            this.mode = mode
        }

        override suspend fun getOutcome(): CardPaymentMockOutcome = CardPaymentMockOutcome.Approved

        override suspend fun setOutcome(outcome: CardPaymentMockOutcome) = Unit
    }

    private class FakeGateway : ControllerGateway {
        override val incomingResponses = MutableSharedFlow<ControllerResponseEvent>()
        override val isPhysicalControllerConnected = MutableStateFlow(false)
        var lastCommand: RequestCommand? = null
        var lastSentPayload: ByteArray? = null
        var lastResponse: ResponseCommand? = null
        var lastPayload: ByteArray? = null

        override suspend fun sendCommand(command: RequestCommand, payload: ByteArray) {
            lastCommand = command
            lastSentPayload = payload
        }

        override suspend fun simulateResponseForTests(command: ResponseCommand, payload: ByteArray) {
            lastResponse = command
            lastPayload = payload
        }
    }
}
