package com.wiva.android.services.payment

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.di.AppIoScope
import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.RequestCommand
import com.wiva.android.hardware.controller.ResponseCommand
import com.wiva.android.services.telemetry.WivaTelemetryService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import timber.log.Timber

/** Тип продукта для терминала оплаты. */
enum class TerminalProductType {
    Drink,
    Cooler,
    Snack,
    Coffee,
}

/**
 * Сервис терминала оплаты: тело `SendSumToPaymentTerminal` (0x48) и подписка на
 * [ResponseCommand.PaymentSystemsPaxStatus], как в `PaymentTerminalService.
 */
@Singleton
class PaymentTerminalService
@Inject
constructor(
    private val controller: ControllerGateway,
    private val telemetry: WivaTelemetryService,
    @AppIoScope private val scope: CoroutineScope,
    private val configRepository: ConfigRepository,
    private val paymentEventLogger: CardPaymentEventLogger,
) {
    private var vendingStatus: Int = 0
    private var isTransactionOnCancel = false
 /** D4: одна отправка saleImportTopic на успешный код Pax (4), как триггер после оплаты. */
    private var saleImportSentForCurrentSession: Boolean = false

    private val _vendStatusText =
        MutableStateFlow(VEND_STATUS_RU[0] ?: "Инициализация")
    val vendStatusText: StateFlow<String> = _vendStatusText.asStateFlow()
    private val paxStatusCodes = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 16)

    init {
        scope.launch {
            controller.incomingResponses.collect { event ->
                if (event.response == ResponseCommand.PaymentSystemsPaxStatus) {
                    processPax(event.payload)
                }
            }
        }
        Timber.tag(TAG).i("Инициализация: подписка на PaxStatus установлена")
    }

    private fun processPax(body: ByteArray) {
        val status = body.firstOrNull()?.toInt()?.and(0xff) ?: return
        paxStatusCodes.tryEmit(status)
        if (status != vendingStatus) {
            vendingStatus = status
            val codeHex = "%02x".format(status)
            Timber.tag(TAG).i("PaxStatus codeHex=%s", codeHex)
            _vendStatusText.value = VEND_STATUS_RU[status] ?: "Код $status"
            paymentEventLogger.info(
                PROVIDER,
                "Получен статус 2can/PAX",
                "${VEND_STATUS_RU[status] ?: "Код $status"} (0x$codeHex)",
                lane = CardPaymentLogLane.FromTerminal,
            )
            if (status == 4) {
                saleImportSentForCurrentSession = true
            }
            if (status != 4) {
                saleImportSentForCurrentSession = false
            }
        }
    }

 /**
 * Отправка суммы на терминал через контроллер (0x48).
 */
    suspend fun sendSumToTerminal(
        type: TerminalProductType,
        price: Int,
        productNumber: Int,
        sbp: Boolean,
    ) {
        isTransactionOnCancel = false
        resetResultStatusBuffer()
        val command =
            byteArrayOf(
                (productNumber / 256).toByte(),
                (productNumber % 256).toByte(),
                (price / 256).toByte(),
                (price % 256).toByte(),
                if (sbp) 1 else 0,
                type.ordinal.toByte(),
            )
        try {
            controller.sendCommand(RequestCommand.SendSumToPaymentTerminal, command)
            paymentEventLogger.info(
                PROVIDER,
                "Сумма отправлена на 2can/PAX",
                "$price руб., товар $productNumber, тип ${type.name}",
                lane = CardPaymentLogLane.ToTerminal,
            )
            Timber
                .tag(TAG)
                .i(
                    "Сумма отправлена на терминал price=%d productNumber=%d type=%s sbp=%s",
                    price,
                    productNumber,
                    type.name,
                    sbp,
                )
        } catch (e: Exception) {
            paymentEventLogger.error(
                PROVIDER,
                "Ошибка отправки суммы",
                e.message ?: e.javaClass.simpleName,
                lane = CardPaymentLogLane.ToTerminal,
            )
            Timber.tag(TAG).e(e, "Ошибка при отправке суммы на терминал")
            throw e
        }
 // Без физического терминала [waitForPaymentResult] ждёт до таймаута. Для СБП мок уже поднимает
 // статус из [DrinkListViewModel] после шага send; для карты — только здесь (см. task-05 / PAX wait).
        if (!sbp && configRepository.get(JsonStoreKeys.USE_MOCK_CONTROLLER) == "true") {
            scope.launch {
                delay(MOCK_CONTROLLER_PAX_STATUS_DELAY_MS)
                controller.simulateResponseForTests(
                    ResponseCommand.PaymentSystemsPaxStatus,
                    byteArrayOf(PAX_STATUS_APPROVED.toByte()),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun resetResultStatusBuffer() {
        paxStatusCodes.resetReplayCache()
    }

    suspend fun waitForPaymentResult(timeoutMs: Long = DEFAULT_PAYMENT_TIMEOUT_MS): PaymentTerminalResult {
        return try {
            val status =
                withTimeout(timeoutMs) {
                    paxStatusCodes.first { it in TERMINAL_RESULT_CODES }
                }
            when (status) {
                PAX_STATUS_APPROVED -> {
                    paymentEventLogger.info(
                        PROVIDER,
                        "Оплата подтверждена",
                        "Статус 4: оплата прошла",
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    PaymentTerminalResult.Approved
                }
                PAX_STATUS_DECLINED -> {
                    paymentEventLogger.info(
                        PROVIDER,
                        "Оплата отклонена",
                        "Статус 5",
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    PaymentTerminalResult.Declined
                }
                PAX_STATUS_CANCELLED, PAX_STATUS_SESSION_CANCELLED -> {
                    paymentEventLogger.info(
                        PROVIDER,
                        "Оплата отменена",
                        "Статус $status",
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    PaymentTerminalResult.Cancelled
                }
                PAX_STATUS_CARD_TIMEOUT -> {
                    paymentEventLogger.error(
                        PROVIDER,
                        "Таймаут чтения карты",
                        "Статус 3",
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    PaymentTerminalResult.Timeout
                }
                else -> {
                    paymentEventLogger.error(
                        PROVIDER,
                        "Неожиданный статус",
                        "Статус $status",
                        lane = CardPaymentLogLane.FromTerminal,
                    )
                    PaymentTerminalResult.Error("status_$status")
                }
            }
        } catch (e: TimeoutCancellationException) {
            paymentEventLogger.error(
                PROVIDER,
                "Таймаут ожидания оплаты",
                "${timeoutMs / 1000} сек.",
                lane = CardPaymentLogLane.FromTerminal,
            )
            PaymentTerminalResult.Timeout
        }
    }

 /** Как в wiva: VendCancel 0xC5 не отправляется. */
    fun cancelTransaction() {
        Timber.tag(TAG).i("cancelTransaction: VendCancel 0xC5 не отправляется (запрещено)")
        paymentEventLogger.info(
            PROVIDER,
            "Отмена 2can/PAX",
            "VendCancel 0xC5 не отправляется",
            lane = CardPaymentLogLane.System,
        )
        isTransactionOnCancel = true
    }

    fun getVendStatusText(): String = _vendStatusText.value

    companion object {
        private const val TAG = "PaymentTerminal"
        private const val PROVIDER = "2can"
        private const val MOCK_CONTROLLER_PAX_STATUS_DELAY_MS = 250L
        private const val DEFAULT_PAYMENT_TIMEOUT_MS = 120_000L
        private const val PAX_STATUS_CARD_TIMEOUT = 3
        private const val PAX_STATUS_APPROVED = 4
        private const val PAX_STATUS_DECLINED = 5
        private const val PAX_STATUS_CANCELLED = 6
        private const val PAX_STATUS_SESSION_CANCELLED = 8
        private val TERMINAL_RESULT_CODES =
            setOf(
                PAX_STATUS_CARD_TIMEOUT,
                PAX_STATUS_APPROVED,
                PAX_STATUS_DECLINED,
                PAX_STATUS_CANCELLED,
                PAX_STATUS_SESSION_CANCELLED,
            )

        private val VEND_STATUS_RU: Map<Int, String> =
            mapOf(
                0 to "Инициализация",
                1 to "Включён",
                2 to "Ожидает карту",
                3 to "Таймаут чтения карты",
                4 to "Оплата прошла",
                5 to "Отклонено",
                6 to "Отменено",
                7 to "Сессия завершена",
                8 to "Сессия отменена",
                9 to "Кнопка старта нажата",
            )
    }
}

sealed interface PaymentTerminalResult {
    data object Approved : PaymentTerminalResult

    data object Declined : PaymentTerminalResult

    data object Cancelled : PaymentTerminalResult

    data object Timeout : PaymentTerminalResult

    data class Error(val reason: String) : PaymentTerminalResult
}
