package com.wiva.android.ui.screens.service.tabs

import com.wiva.android.services.payment.CardPaymentLogLane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaymentLogUiLabelsTest {

    @Test
    fun laneShortLabel_matchesExpectedTokens() {
        assertEquals("OUT", laneShortLabel(CardPaymentLogLane.ToTerminal))
        assertEquals("IN", laneShortLabel(CardPaymentLogLane.FromTerminal))
        assertEquals("MOCK", laneShortLabel(CardPaymentLogLane.Mock))
        assertEquals("SYS", laneShortLabel(CardPaymentLogLane.System))
    }

    @Test
    fun laneLongDescriptionRu_nonBlank() {
        for (lane in enumValues<CardPaymentLogLane>()) {
            assertTrue(laneLongDescriptionRu(lane).isNotBlank())
        }
    }
}
