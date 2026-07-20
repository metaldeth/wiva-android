package com.viwa.android.services.payment

import com.viwa.android.hardware.controller.ControllerGateway
import com.viwa.android.hardware.controller.RequestCommand
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Тип продукта для команды 0x48 (СБП-уведомление контроллеру). */
enum class TerminalProductType {
    Drink,
    Cooler,
    Snack,
    Coffee,
}

/**
 * Тонкий сервис СБП: только отправка [RequestCommand.SendSumToPaymentTerminal] (0x48) с sbp=1.
 * Без ожидания PaxStatus (0x56).
 */
@Singleton
class ControllerSbpNotifyService
@Inject
constructor(
    private val controller: ControllerGateway,
    private val paymentEventLogger: CardPaymentEventLogger,
) {
    suspend fun notifySbpPayment(
        type: TerminalProductType,
        price: Int,
        productNumber: Int,
    ) {
        val command =
            byteArrayOf(
                (productNumber / 256).toByte(),
                (productNumber % 256).toByte(),
                (price / 256).toByte(),
                (price % 256).toByte(),
                1,
                type.ordinal.toByte(),
            )
        controller.sendCommand(RequestCommand.SendSumToPaymentTerminal, command)
        paymentEventLogger.info(
            PROVIDER,
            "Сумма отправлена контроллеру (СБП)",
            "$price руб., товар $productNumber, тип ${type.name}",
            lane = CardPaymentLogLane.ToTerminal,
        )
        Timber.tag(TAG).i(
            "SBP notify 0x48 price=%d productNumber=%d type=%s",
            price,
            productNumber,
            type.name,
        )
    }

    companion object {
        private const val TAG = "SbpNotify"
        private const val PROVIDER = "СБП"
    }
}
