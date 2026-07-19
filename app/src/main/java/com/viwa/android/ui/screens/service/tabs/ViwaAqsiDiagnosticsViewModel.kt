package com.viwa.android.ui.screens.service.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viwa.android.data.payment.aqsi.AqsiDiagnosticOperationKind
import com.viwa.android.data.payment.aqsi.AqsiDiagnosticOutcome
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSnapshotHolder
import com.viwa.android.data.payment.aqsi.AqsiLastOperationSummary
import com.viwa.android.domain.model.AqsiPaymentResult
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.model.CardPaymentResult
import com.viwa.android.domain.repository.AqsiRepository
import com.viwa.android.domain.repository.CardPaymentMockModeRepository
import com.viwa.android.domain.repository.CardPaymentMethodRepository
import com.viwa.android.services.payment.CardPaymentEventLogger
import com.viwa.android.services.payment.CardPaymentLogLane
import com.viwa.android.services.payment.CardPaymentOrchestrator
import com.viwa.android.services.payment.TerminalProductType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViwaCardPaymentDiagnosticsUiState(
    val cardMethodLabel: String = "",
    val mockMode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    val lastOperationLine1: String? = null,
    val lastOperationLine2: String? = null,
    val pennyTestBusy: Boolean = false,
    val pennyTestBanner: String? = null,
    val pennyTestBannerIsError: Boolean = false,
)

@HiltViewModel
class ViwaAqsiDiagnosticsViewModel
    @Inject
    constructor(
        private val cardPaymentMethodRepository: CardPaymentMethodRepository,
        private val cardPaymentMockModeRepository: CardPaymentMockModeRepository,
        private val lastOperationSnapshotHolder: AqsiLastOperationSnapshotHolder,
        private val aqsiRepository: AqsiRepository,
        private val cardPaymentOrchestrator: CardPaymentOrchestrator,
        private val paymentEventLogger: CardPaymentEventLogger,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ViwaCardPaymentDiagnosticsUiState())
        val uiState: StateFlow<ViwaCardPaymentDiagnosticsUiState> = _uiState.asStateFlow()
        val logEntries = paymentEventLogger.entries

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val method = runCatching { cardPaymentMethodRepository.getSelected() }.getOrDefault(CardPaymentMethod.Pax)
                val mockMode = runCatching { cardPaymentMockModeRepository.getMode() }.getOrDefault(CardPaymentMockMode.Disabled)
                val snap = lastOperationSnapshotHolder.getSnapshot()
                _uiState.update {
                    it.copy(
                        cardMethodLabel = cardMethodLabel(method),
                        mockMode = mockMode,
                        lastOperationLine1 = formatSummaryLine1(snap),
                        lastOperationLine2 = formatSummaryLine2(snap),
                    )
                }
            }
        }

        fun enableAqsiMock() {
            setMockMode(
                mode = CardPaymentMockMode.Aqsi,
                banner = "Mock для нового считывателя включён",
            )
        }

        fun selectAqsiAsActive() {
            viewModelScope.launch {
                runCatching { cardPaymentMethodRepository.setSelected(CardPaymentMethod.Aqsi) }
                    .onSuccess {
                        paymentEventLogger.info(
                            "Новый считыватель",
                            "Выбран активным методом оплаты картой",
                            lane = CardPaymentLogLane.System,
                        )
                        _uiState.update {
                            it.copy(
                                cardMethodLabel = cardMethodLabel(CardPaymentMethod.Aqsi),
                                pennyTestBanner = "aQsi выбран активным методом",
                                pennyTestBannerIsError = false,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                pennyTestBanner = e.message ?: "Не удалось выбрать метод",
                                pennyTestBannerIsError = true,
                            )
                        }
                    }
            }
        }

        fun cancelAqsiFromServiceMenu() {
            viewModelScope.launch {
                runCatching { aqsiRepository.cancelPayment() }
                    .onSuccess {
                        paymentEventLogger.info(
                            "Новый считыватель",
                            "Запрос отмены из сервисного меню",
                            lane = CardPaymentLogLane.System,
                        )
                        val snap = lastOperationSnapshotHolder.getSnapshot()
                        _uiState.update {
                            it.copy(
                                pennyTestBanner = "Запрос отмены отправлен (см. «Последняя операция» и журнал)",
                                pennyTestBannerIsError = false,
                                lastOperationLine1 = formatSummaryLine1(snap),
                                lastOperationLine2 = formatSummaryLine2(snap),
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                pennyTestBanner = e.message ?: "Ошибка отмены",
                                pennyTestBannerIsError = true,
                            )
                        }
                    }
            }
        }

        fun disableMock() {
            setMockMode(CardPaymentMockMode.Disabled, "Mock платёжника выключен")
        }

        fun runMockPayment() {
            runMockPaymentScenario(CardPaymentMockOutcome.Approved)
        }

        fun runMockPaymentScenario(outcome: CardPaymentMockOutcome) {
            viewModelScope.launch {
                val mockMode =
                    runCatching { cardPaymentMockModeRepository.getMode() }.getOrDefault(CardPaymentMockMode.Disabled)
                if (mockMode != CardPaymentMockMode.Aqsi) {
                    _uiState.update {
                        it.copy(
                            pennyTestBusy = false,
                            pennyTestBanner = "Сначала включите mock для нового считывателя",
                            pennyTestBannerIsError = true,
                        )
                    }
                    return@launch
                }
                _uiState.update { it.copy(pennyTestBusy = true, pennyTestBanner = null, pennyTestBannerIsError = false) }
                runCatching {
                    cardPaymentMethodRepository.setSelected(CardPaymentMethod.Aqsi)
                    cardPaymentMockModeRepository.setOutcome(outcome)
                    cardPaymentOrchestrator.pay(
                        type = TerminalProductType.Drink,
                        price = 1,
                        productNumber = 1,
                        sbp = false,
                    )
                }.onSuccess { result ->
                    val (banner, isError) =
                        when (result) {
                            CardPaymentResult.Success -> "Mock aQsi: успех" to false
                            CardPaymentResult.Cancelled -> "Mock aQsi: отмена" to false
                            is CardPaymentResult.Failed -> "Mock-оплата: ${result.reason}" to true
                        }
                    _uiState.update {
                        it.copy(
                            mockMode = CardPaymentMockMode.Aqsi,
                            pennyTestBusy = false,
                            pennyTestBanner = banner,
                            pennyTestBannerIsError = isError,
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            pennyTestBusy = false,
                            pennyTestBanner = e.message ?: "Ошибка mock-оплаты",
                            pennyTestBannerIsError = true,
                        )
                    }
                }
            }
        }

        fun clearLog() {
            paymentEventLogger.clear()
        }

        fun runPennyTest() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        pennyTestBusy = true,
                        pennyTestBanner = null,
                        pennyTestBannerIsError = false,
                    )
                }
                paymentEventLogger.info(
                    "Новый считыватель",
                    "Тест 1 коп. отправлен",
                    lane = CardPaymentLogLane.ToTerminal,
                )
                val result = aqsiRepository.initiatePayment(1)
                val snap = lastOperationSnapshotHolder.getSnapshot()
                val payment = result.getOrNull()
                val (banner, isError) =
                    when {
                        result.isFailure ->
                            (result.exceptionOrNull()?.message?.take(160) ?: "Ошибка запуска теста") to true
                        payment === AqsiPaymentResult.Approved ->
                            "Тест 1 коп.: одобрено" to false
                        payment is AqsiPaymentResult.Declined ->
                            "Тест 1 коп.: отказ" to false
                        payment is AqsiPaymentResult.Error ->
                            "Тест 1 коп.: ошибка оплаты" to true
                        payment === AqsiPaymentResult.Cancelled ->
                            "Тест 1 коп.: отмена" to false
                        else ->
                            "Тест 1 коп.: завершено" to false
                    }
                _uiState.update {
                    it.copy(
                        pennyTestBusy = false,
                        pennyTestBanner = banner,
                        pennyTestBannerIsError = isError,
                        lastOperationLine1 = formatSummaryLine1(snap),
                        lastOperationLine2 = formatSummaryLine2(snap),
                    )
                }
            }
        }

        private fun cardMethodLabel(m: CardPaymentMethod): String =
            when (m) {
                CardPaymentMethod.Pax -> "2can"
                CardPaymentMethod.Aqsi -> "Новый считыватель aQsi"
            }

        private fun setMockMode(
            mode: CardPaymentMockMode,
            banner: String,
        ) {
            viewModelScope.launch {
                runCatching {
                    cardPaymentMockModeRepository.setMode(mode)
                }
                    .onSuccess {
                        paymentEventLogger.info(
                            "Mock платёжника",
                            banner,
                            lane = CardPaymentLogLane.System,
                        )
                        val method =
                            runCatching { cardPaymentMethodRepository.getSelected() }.getOrDefault(CardPaymentMethod.Pax)
                        _uiState.update {
                            it.copy(
                                mockMode = mode,
                                cardMethodLabel = cardMethodLabel(method),
                                pennyTestBanner = banner,
                                pennyTestBannerIsError = false,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                pennyTestBanner = e.message ?: "Не удалось изменить mock-режим",
                                pennyTestBannerIsError = true,
                            )
                        }
                    }
            }
        }

        private fun formatSummaryLine1(s: AqsiLastOperationSummary?): String? {
            if (s == null) {
                return "Последняя операция aQsi: нет данных в памяти процесса."
            }
            val timeStr = formatTime(s.timestampMillis)
            val kind =
                when (s.operationKind) {
                    AqsiDiagnosticOperationKind.TCP_TEST -> "Тест TCP"
                    AqsiDiagnosticOperationKind.PAYMENT -> "Платёж"
                    AqsiDiagnosticOperationKind.CANCEL -> "Отмена"
                }
            return "$timeStr · $kind · ${outcomeRu(s.outcome)}"
        }

        private fun formatSummaryLine2(s: AqsiLastOperationSummary?): String? {
            if (s == null) return null
            val code = s.detailCode.trim()
            return if (code.isEmpty()) {
                "Код: —"
            } else {
                "Код: $code"
            }
        }

        private fun outcomeRu(o: AqsiDiagnosticOutcome): String =
            when (o) {
                AqsiDiagnosticOutcome.SUCCESS -> "успех"
                AqsiDiagnosticOutcome.ERROR -> "ошибка"
                AqsiDiagnosticOutcome.APPROVED -> "одобрено"
                AqsiDiagnosticOutcome.DECLINED -> "отказ"
                AqsiDiagnosticOutcome.CANCELLED -> "отмена"
            }

        private fun formatTime(epochMs: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            return fmt.format(Date(epochMs))
        }
    }
