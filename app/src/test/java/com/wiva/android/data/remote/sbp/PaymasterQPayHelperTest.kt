package com.wiva.android.data.remote.sbp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymasterQPayHelperTest {
    @Test
    fun generateReqn_isTenDigitNumericAndUnique() {
        repeat(50) {
            val a = PaymasterQPayHelper.generateReqn()
            val b = PaymasterQPayHelper.generateReqn()
            assertEquals(10, a.length)
            assertEquals(10, b.length)
            assertTrue(a.all { it.isDigit() })
            assertTrue(b.all { it.isDigit() })
            assertTrue(a != b)
        }
    }

    @Test
    fun calculateSign_isDeterministicForSameInputs() {
        val s1 = PaymasterQPayHelper.calculateSign("spot", "order123", "sbp", "reqn456", "secret")
        val s2 = PaymasterQPayHelper.calculateSign("spot", "order123", "sbp", "reqn456", "secret")
        assertEquals(s1, s2)
        assertEquals(64, s1.length)
    }
}
