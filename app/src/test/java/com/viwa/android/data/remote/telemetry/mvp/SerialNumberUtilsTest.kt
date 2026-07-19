package com.viwa.android.data.remote.telemetry.mvp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialNumberUtilsTest {
    @Test
    fun `normalize pads digits to VIWA-000001 format`() {
        assertEquals("VIWA-000001", SerialNumberUtils.normalize("viwa-1"))
        assertEquals("VIWA-000001", SerialNumberUtils.normalize("VIWA000001"))
        assertEquals("VIWA-123456", SerialNumberUtils.normalize("  viwa-123456  "))
    }

    @Test
    fun `isValid accepts normalized serial`() {
        assertTrue(SerialNumberUtils.isValid("VIWA-000001"))
        assertTrue(SerialNumberUtils.isValid("viwa1"))
    }

    @Test
    fun `isValid rejects legacy WIVA prefix`() {
        assertFalse(SerialNumberUtils.isValid("WIVA-000001"))
    }

    @Test
    fun `validationMessage rejects invalid serial`() {
        assertEquals("Введите серийный номер", SerialNumberUtils.validationMessage(""))
        assertNull(SerialNumberUtils.validationMessage("VIWA-000001"))
        assertEquals("Формат: VIWA-000001 (6 цифр)", SerialNumberUtils.validationMessage("WIVA-000001"))
    }
}
