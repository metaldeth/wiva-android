package com.wiva.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AqsiPaymentResultTest {

    @Test
    fun approvedEquality() {
        assertEquals(AqsiPaymentResult.Approved, AqsiPaymentResult.Approved)
        assertEquals(AqsiPaymentResult.Approved.hashCode(), AqsiPaymentResult.Approved.hashCode())
    }

    @Test
    fun declinedEquality() {
        assertEquals(AqsiPaymentResult.Declined("D01"), AqsiPaymentResult.Declined(publicCode = "D01"))
        assertNotEquals(
            AqsiPaymentResult.Declined(publicCode = "A"),
            AqsiPaymentResult.Declined(publicCode = "B"),
        )
    }

    @Test
    fun errorEquality() {
        val e = AqsiPaymentResult.Error(safeMessage = "timeout")
        assertEquals(e, AqsiPaymentResult.Error(safeMessage = "timeout"))
        assertNotEquals(
            AqsiPaymentResult.Error(safeMessage = "timeout"),
            AqsiPaymentResult.Error(safeMessage = "io"),
        )
    }

    @Test
    fun cancelledEquality() {
        assertEquals(AqsiPaymentResult.Cancelled, AqsiPaymentResult.Cancelled)
    }

    @Test
    fun allBranchesDistinct_exhaustiveWhen() {
        val samples =
            listOf(
                AqsiPaymentResult.Approved,
                AqsiPaymentResult.Declined(),
                AqsiPaymentResult.Error(safeMessage = "test"),
                AqsiPaymentResult.Cancelled,
            )
        samples.forEach { r ->
            val tag =
                when (r) {
                    AqsiPaymentResult.Approved -> "ok"
                    is AqsiPaymentResult.Declined -> "dec"
                    is AqsiPaymentResult.Error -> "err"
                    AqsiPaymentResult.Cancelled -> "cancel"
                }
            assertTrue(tag.isNotEmpty())
        }
    }
}
