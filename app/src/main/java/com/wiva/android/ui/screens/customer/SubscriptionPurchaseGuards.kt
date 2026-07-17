package com.wiva.android.ui.screens.customer

/**
 * Проверки перед стартом оплаты подписки (СБП или карта) и после СБП перед [saleSubscribeTopic].
 * Вынесено из [DrinkListViewModel] для unit-тестов без Hilt.
 */
internal object SubscriptionPurchaseGuards {
 /**
 * @return текст ошибки для UI или `null`, если можно продолжать.
 */
    fun validationErrorForStart(
        scannedClientId: String?,
        subscribeLevelUuid: String?,
        priceRub: Int,
    ): String? {
        if (scannedClientId.isNullOrBlank()) return "Сначала отсканируйте карту подписки"
        if (subscribeLevelUuid.isNullOrBlank()) return "Нет тарифа подписки от телеметрии"
        if (priceRub <= 0) return "Некорректная сумма подписки"
        return null
    }

 /**
 * После успешной оплаты СБП state мог сброситься — перед телеметрией проверяем UUID.
 */
    fun missingSessionAfterSbpError(
        userUuid: String?,
        levelUuid: String?,
    ): String? {
        if (userUuid.isNullOrBlank() || levelUuid.isNullOrBlank()) {
            return "Сессия подписки сброшена. Отсканируйте карту снова."
        }
        return null
    }
}
