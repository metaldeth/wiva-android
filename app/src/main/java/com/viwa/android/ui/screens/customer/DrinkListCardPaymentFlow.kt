package com.viwa.android.ui.screens.customer

import com.viwa.android.domain.model.CardPaymentResult
import com.viwa.android.domain.model.customer.DrinkContainer
import com.viwa.android.services.payment.CardPaymentOrchestrator
import com.viwa.android.services.payment.PaymentTerminalService
import com.viwa.android.services.payment.TerminalProductType

/**
 * Карточный и СБП-вызовы терминала для напитка и подписки (task-05).
 * Логика.
 */
internal object DrinkListCardPaymentFlow {

    suspend fun runDrinkPaymentBeforePour(
        container: DrinkContainer,
        volume: Int,
        sbp: Boolean,
        cardPaymentOrchestrator: CardPaymentOrchestrator,
        paymentTerminalService: PaymentTerminalService,
    ) {
        val priceRub = container.product.dPrices.firstOrNull { it.volume == volume }?.priceRub ?: 0
        if (sbp) {
            paymentTerminalService.sendSumToTerminal(
                TerminalProductType.Drink,
                priceRub,
                container.containerNumber,
                sbp = true,
            )
            return
        }
        when (
            val payResult =
                cardPaymentOrchestrator.pay(
                    TerminalProductType.Drink,
                    priceRub,
                    container.containerNumber,
                    sbp = false,
                )
        ) {
            CardPaymentResult.Success -> Unit
            is CardPaymentResult.Failed ->
                throw IllegalStateException(
                    payResult.reason.ifBlank { "Ошибка оплаты картой" },
                )

            CardPaymentResult.Cancelled ->
                throw IllegalStateException("Оплата отменена")
        }
    }

    suspend fun paySubscriptionWithCard(
        priceRub: Int,
        cardPaymentOrchestrator: CardPaymentOrchestrator,
    ): CardPaymentResult =
        cardPaymentOrchestrator.pay(
            TerminalProductType.Drink,
            priceRub,
            productNumber = 0,
            sbp = false,
        )
}
