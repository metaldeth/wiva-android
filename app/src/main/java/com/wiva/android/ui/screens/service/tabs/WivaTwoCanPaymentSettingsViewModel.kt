package com.wiva.android.ui.screens.service.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiva.android.domain.model.CardPaymentMethod
import com.wiva.android.domain.model.CardPaymentMockMode
import com.wiva.android.domain.model.CardPaymentMockOutcome
import com.wiva.android.domain.model.CardPaymentResult
import com.wiva.android.domain.repository.CardPaymentMockModeRepository
import com.wiva.android.domain.repository.CardPaymentMethodRepository
import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.ResponseCommand
import com.wiva.android.services.payment.CardPaymentEventLogger
import com.wiva.android.services.payment.CardPaymentLogLane
import com.wiva.android.services.payment.CardPaymentOrchestrator
import com.wiva.android.services.payment.PaymentTerminalService
import com.wiva.android.services.payment.TerminalProductType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class WivaTwoCanPaymentSettingsUiState(
    val isSelected: Boolean = false,
    val mockMode: CardPaymentMockMode = CardPaymentMockMode.Disabled,
    val statusLine: String = "",
    val isBusy: Boolean = false,
    val banner: String? = null,
    val bannerIsError: Boolean = false,
)

@HiltViewModel
class WivaTwoCanPaymentSettingsViewModel
    @Inject
    constructor(
        private val cardPaymentMethodRepository: CardPaymentMethodRepository,
        private val cardPaymentMockModeRepository: CardPaymentMockModeRepository,
        private val cardPaymentOrchestrator: CardPaymentOrchestrator,
        private val paymentTerminalService: PaymentTerminalService,
        private val controllerGateway: ControllerGateway,
        private val paymentEventLogger: CardPaymentEventLogger,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WivaTwoCanPaymentSettingsUiState(isBusy = true))
        val uiState: StateFlow<WivaTwoCanPaymentSettingsUiState> = _uiState.asStateFlow()
        val logEntries = paymentEventLogger.entries

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                val method = runCatching { cardPaymentMethodRepository.getSelected() }.getOrDefault(CardPaymentMethod.Pax)
                val mockMode = runCatching { cardPaymentMockModeRepository.getMode() }.getOrDefault(CardPaymentMockMode.Disabled)
                _uiState.update {
                    it.copy(
                        isSelected = method === CardPaymentMethod.Pax,
                        mockMode = mockMode,
                        statusLine = paymentTerminalService.getVendStatusText(),
                        isBusy = false,
                    )
                }
            }
        }

        fun enableTwoCanMock() {
            setMockMode(
                mode = CardPaymentMockMode.TwoCan,
                banner = "Mock для 2can включён",
            )
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
                if (mockMode != CardPaymentMockMode.TwoCan) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            banner = "Сначала включите mock 2can отдельной кнопкой",
                            bannerIsError = true,
                        )
                    }
                    return@launch
                }
                _uiState.update { it.copy(isBusy = true, banner = null, bannerIsError = false) }
                runCatching {
                    cardPaymentMethodRepository.setSelected(CardPaymentMethod.Pax)
                    cardPaymentMockModeRepository.setOutcome(outcome)
                    cardPaymentOrchestrator.pay(
                        type = TerminalProductType.Drink,
                        price = TEST_PRICE_RUB,
                        productNumber = TEST_PRODUCT_NUMBER,
                        sbp = false,
                    )
                }.onSuccess { result ->
                    val (banner, isError) =
                        when (result) {
                            CardPaymentResult.Success -> "Mock 2can: успех" to false
                            CardPaymentResult.Cancelled -> "Mock 2can: отмена" to false
                            is CardPaymentResult.Failed -> "Mock-оплата 2can: ${result.reason}" to true
                        }
                    val method = runCatching { cardPaymentMethodRepository.getSelected() }.getOrDefault(CardPaymentMethod.Pax)
                    _uiState.update {
                        it.copy(
                            isSelected = method === CardPaymentMethod.Pax,
                            mockMode = CardPaymentMockMode.TwoCan,
                            isBusy = false,
                            banner = banner,
                            bannerIsError = isError,
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            banner = e.message ?: "Ошибка mock-оплаты",
                            bannerIsError = true,
                        )
                    }
                }
            }
        }

        fun selectTwoCan() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null) }
                runCatching { cardPaymentMethodRepository.setSelected(CardPaymentMethod.Pax) }
                    .onSuccess {
                        Timber.tag(TAG).i("2can selected as active card provider")
                        _uiState.update {
                            it.copy(isSelected = true, isBusy = false, banner = "2can выбран для оплаты картой")
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = e.message ?: "Не удалось выбрать 2can",
                                bannerIsError = true,
                            )
                        }
                    }
            }
        }

        fun sendTestAmount() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null, bannerIsError = false) }
                runCatching {
                    paymentTerminalService.sendSumToTerminal(
                        type = TerminalProductType.Drink,
                        price = TEST_PRICE_RUB,
                        productNumber = TEST_PRODUCT_NUMBER,
                        sbp = false,
                    )
                }.onSuccess {
                    paymentEventLogger.info(
                        "2can",
                        "Тестовая сумма отправлена из сервисного меню",
                        lane = CardPaymentLogLane.ToTerminal,
                    )
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusLine = paymentTerminalService.getVendStatusText(),
                            banner = "Тест 0x48 отправлен: $TEST_PRICE_RUB руб., товар $TEST_PRODUCT_NUMBER",
                            bannerIsError = false,
                        )
                    }
                }.onFailure { e ->
                    Timber.tag(TAG).w(e, "send test amount failed")
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            banner = e.message ?: "Ошибка отправки 0x48",
                            bannerIsError = true,
                        )
                    }
                }
            }
        }

        fun simulateDiagnosticPaxStatus(statusCode: Int) {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null, bannerIsError = false) }
                runCatching {
                    controllerGateway.simulateResponseForTests(
                        ResponseCommand.PaymentSystemsPaxStatus,
                        byteArrayOf(statusCode.toByte()),
                    )
                }.onSuccess {
                    paymentEventLogger.info(
                        "2can",
                        "Диагностика: подстановка статуса Pax в шину контроллера",
                        "Код $statusCode — ${statusLabel(statusCode)}",
                        lane = CardPaymentLogLane.System,
                    )
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusLine = paymentTerminalService.getVendStatusText(),
                            banner = "Диагностика: подставлен статус ${statusLabel(statusCode)}",
                            bannerIsError = false,
                        )
                    }
                }.onFailure { e ->
                    Timber.tag(TAG).w(e, "simulate pax status failed")
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            banner = e.message ?: "Ошибка подстановки диагностического статуса",
                            bannerIsError = true,
                        )
                    }
                }
            }
        }

        fun cancelTransaction() {
            paymentTerminalService.cancelTransaction()
            _uiState.update {
                it.copy(
                    statusLine = paymentTerminalService.getVendStatusText(),
                    banner = "Отмена текущей операции вызвана локально",
                    bannerIsError = false,
                )
            }
        }

        fun clearLog() {
            paymentEventLogger.clear()
        }

        private fun setMockMode(
            mode: CardPaymentMockMode,
            banner: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, banner = null, bannerIsError = false) }
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
                                isSelected = method === CardPaymentMethod.Pax,
                                isBusy = false,
                                banner = banner,
                                bannerIsError = false,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                banner = e.message ?: "Не удалось изменить mock-режим",
                                bannerIsError = true,
                            )
                        }
                    }
            }
        }

        private fun statusLabel(code: Int): String =
            when (code) {
                PAX_STATUS_WAIT_CARD -> "ожидает карту"
                PAX_STATUS_APPROVED -> "оплата прошла"
                PAX_STATUS_DECLINED -> "отклонено"
                PAX_STATUS_CANCELLED -> "отменено"
                else -> "код $code"
            }

        private companion object {
            const val TAG = "TwoCanPayment"
            const val TEST_PRICE_RUB = 1
            const val TEST_PRODUCT_NUMBER = 1
            const val PAX_STATUS_WAIT_CARD = 2
            const val PAX_STATUS_APPROVED = 4
            const val PAX_STATUS_DECLINED = 5
            const val PAX_STATUS_CANCELLED = 6
        }
    }
