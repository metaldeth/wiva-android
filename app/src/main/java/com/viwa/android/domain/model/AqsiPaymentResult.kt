package com.viwa.android.domain.model

/**
 * Исход платёжного сценария aQsi (JPAY/Arcus2). Поля — только безопасные для UI/логов
 * коды и общие сообщения, без PAN, трека, CVV и полезной нагрузки протокола.
 */
sealed interface AqsiPaymentResult {
 /** Операция успешно одобрена. */
    data object Approved : AqsiPaymentResult

 /**
 * Явный отказ (банк/ридер и т.п.). [publicCode] — агрегированный код/статус
 * для отображения, не восстанавливающий чувствительные данные транзакции.
 */
    data class Declined(val publicCode: String = "") : AqsiPaymentResult

 /**
 * Ошибка канала или протокола. [safeMessage] — краткое нейтральное описание для оператора/UI;
 * не должно содержать сырых пакетов, треков и др. PCI-чувствительного содержимого.
 */
    data class Error(val safeMessage: String) : AqsiPaymentResult

 /** Операция отменена до успешного завершения (карта / сессия JPAY). */
    data object Cancelled : AqsiPaymentResult
}
