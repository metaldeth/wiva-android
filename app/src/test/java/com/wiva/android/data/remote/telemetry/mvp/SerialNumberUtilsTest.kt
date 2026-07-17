package com.wiva.android.data.remote.telemetry.mvp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialNumberUtilsTest {
    @Test
    fun `normalize pads digits to WIVA-000001 format`() {
        assertEquals("WIVA-000001", SerialNumberUtils.normalize("wiva-1"))
        assertEquals("WIVA-000001", SerialNumberUtils.normalize("WIVA000001"))
        assertEquals("WIVA-123456", SerialNumberUtils.normalize("  wiva-123456  "))
    }

    @Test
    fun `isValid accepts normalized serial`() {
        assertTrue(SerialNumberUtils.isValid("WIVA-000001"))
        assertTrue(SerialNumberUtils.isValid("wiva1"))
    }

    @Test
    fun `validationMessage rejects invalid serial`() {
        assertEquals("Введите серийный номер", SerialNumberUtils.validationMessage(""))
        assertNull(SerialNumberUtils.validationMessage("WIVA-000001"))
    }
}
