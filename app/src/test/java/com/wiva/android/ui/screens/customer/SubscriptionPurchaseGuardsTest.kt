package com.wiva.android.ui.screens.customer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionPurchaseGuardsTest {

    @Test
    fun validationErrorForStart_allValid_returnsNull() {
        assertNull(
            SubscriptionPurchaseGuards.validationErrorForStart(
                scannedClientId = "client-uuid",
                subscribeLevelUuid = "level-uuid",
                priceRub = 199,
            ),
        )
    }

    @Test
    fun validationErrorForStart_blankClient_returnsMessage() {
        assertEquals(
            "Сначала отсканируйте карту подписки",
            SubscriptionPurchaseGuards.validationErrorForStart(null, "lvl", 100),
        )
        assertEquals(
            "Сначала отсканируйте карту подписки",
            SubscriptionPurchaseGuards.validationErrorForStart("   ", "lvl", 100),
        )
    }

    @Test
    fun validationErrorForStart_blankLevel_returnsMessage() {
        assertEquals(
            "Нет тарифа подписки от телеметрии",
            SubscriptionPurchaseGuards.validationErrorForStart("client", null, 100),
        )
        assertEquals(
            "Нет тарифа подписки от телеметрии",
            SubscriptionPurchaseGuards.validationErrorForStart("client", "", 100),
        )
    }

    @Test
    fun validationErrorForStart_nonPositivePrice_returnsMessage() {
        assertEquals(
            "Некорректная сумма подписки",
            SubscriptionPurchaseGuards.validationErrorForStart("c", "l", 0),
        )
        assertEquals(
            "Некорректная сумма подписки",
            SubscriptionPurchaseGuards.validationErrorForStart("c", "l", -1),
        )
    }

    @Test
    fun missingSessionAfterSbpError_bothPresent_returnsNull() {
        assertNull(SubscriptionPurchaseGuards.missingSessionAfterSbpError("u", "l"))
    }

    @Test
    fun missingSessionAfterSbpError_anyMissing_returnsMessage() {
        val msg = "Сессия подписки сброшена. Отсканируйте карту снова."
        assertEquals(msg, SubscriptionPurchaseGuards.missingSessionAfterSbpError(null, "l"))
        assertEquals(msg, SubscriptionPurchaseGuards.missingSessionAfterSbpError("u", null))
        assertEquals(msg, SubscriptionPurchaseGuards.missingSessionAfterSbpError("", "l"))
    }
}
