package com.wiva.android.services.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaymentEventLoggerLaneTest {

    @Test
    fun info_recordsLaneAndTruncatesDetail() {
        val logger = CardPaymentEventLogger()
        val longDetail = "x".repeat(500)
        logger.info("2can", "Сумма отправлена", longDetail, CardPaymentLogLane.ToTerminal)
        val e = logger.entries.value.first()
        assertEquals(CardPaymentLogLane.ToTerminal, e.lane)
        assertEquals(400, e.detail.length)
        assertTrue(e.detail.startsWith("xxxx"))
    }

    @Test
    fun error_defaultsToSystemLaneWhenUsingTwoArgOverload() {
        val logger = CardPaymentEventLogger()
        logger.error("p", "ошибка")
        assertEquals(CardPaymentLogLane.System, logger.entries.value.first().lane)
        assertTrue(logger.entries.value.first().isError)
    }
}
