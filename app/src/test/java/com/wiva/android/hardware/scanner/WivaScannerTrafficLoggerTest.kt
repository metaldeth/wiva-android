package com.wiva.android.hardware.scanner

import com.wiva.android.domain.model.BarcodeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WivaScannerTrafficLoggerTest {
    private val validKey = "REG-0123456789AB"

    @Test
    fun `logger masks plain REG key in stored raw line`() {
        val logger = WivaScannerTrafficLogger()
        logger.log(validKey, BarcodeEvent.RegistrationKey(validKey))
        val entry = logger.entries.value.single()
        assertEquals("REG-************", entry.rawLine)
        assertFalse(entry.rawLine.contains("0123456789AB"))
    }

    @Test
    fun `logger masks registrationKey in json qr raw line`() {
        val raw =
            """
            {"type":"WIVA_TELEMETRY_REGISTRATION","version":1,"registrationKey":"$validKey"}
            """.trimIndent()
        val logger = WivaScannerTrafficLogger()
        logger.log(raw, BarcodeEvent.TelemetryRegistrationQr(raw))
        val entry = logger.entries.value.single()
        assertFalse(entry.rawLine.contains(validKey))
        assertFalse(entry.rawLine.contains("0123456789AB"))
    }
}
