package com.viwa.android.domain.model

/**
 * Унифицированный исход карточной оплаты (PAX и aQsi) с экрана заказа, без СБП.
 * См. [com.viwa.android.services.payment.CardPaymentOrchestrator].
 */
sealed interface CardPaymentResult {
    data object Success : CardPaymentResult

    data class Failed(val reason: String) : CardPaymentResult

    data object Cancelled : CardPaymentResult
}
