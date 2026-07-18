package com.wiva.android.data.remote.telemetry.mvp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryRegistrationQrParserTest {
    private val validKey = RegistrationKeyUtilsTest.VALID_KEY

    @Test
    fun `parse raw REG key`() {
        val result = TelemetryRegistrationQrParser.parse(validKey)
        assertTrue(result is TelemetryRegistrationQrParseResult.Success)
        val scan = (result as TelemetryRegistrationQrParseResult.Success).scan
        assertEquals(validKey, scan.registrationKey)
    }

    @Test
    fun `parse json v1 qr with trusted https apiUrl`() {
        val json =
            """
            {
              "type":"WIVA_TELEMETRY_REGISTRATION",
              "version":1,
              "registrationKey":"$validKey",
              "serialNumber":"WIVA-000004",
              "apiUrl":"https://194.67.74.147"
            }
            """.trimIndent()
        val result = TelemetryRegistrationQrParser.parse(json)
        assertTrue(result is TelemetryRegistrationQrParseResult.Success)
        val scan = (result as TelemetryRegistrationQrParseResult.Success).scan
        assertEquals(validKey, scan.registrationKey)
        assertEquals("WIVA-000004", scan.serialNumber)
        assertEquals("https://194.67.74.147", scan.apiUrl)
    }

    @Test
    fun `parse json rejects http apiUrl`() {
        val json =
            """
            {"type":"WIVA_TELEMETRY_REGISTRATION","version":1,"registrationKey":"$validKey","apiUrl":"http://194.67.74.147"}
            """.trimIndent()
        val result = TelemetryRegistrationQrParser.parse(json)
        assertTrue(result is TelemetryRegistrationQrParseResult.Invalid)
    }

    @Test
    fun `parse json rejects invalid registrationKey with I L O U`() {
        val json =
            """
            {"type":"WIVA_TELEMETRY_REGISTRATION","version":1,"registrationKey":"REG-0123456789IL"}
            """.trimIndent()
        val result = TelemetryRegistrationQrParser.parse(json)
        assertTrue(result is TelemetryRegistrationQrParseResult.Invalid)
    }

    @Test
    fun `parse json rejects wrong version`() {
        val json =
            """{"type":"WIVA_TELEMETRY_REGISTRATION","version":2,"registrationKey":"$validKey"}"""
        val result = TelemetryRegistrationQrParser.parse(json)
        assertTrue(result is TelemetryRegistrationQrParseResult.Invalid)
    }

    @Test
    fun `parse json rejects wrong type`() {
        val json =
            """{"type":"OTHER","version":1,"registrationKey":"$validKey"}"""
        val result = TelemetryRegistrationQrParser.parse(json)
        assertTrue(result is TelemetryRegistrationQrParseResult.Invalid)
    }

    @Test
    fun `parse invalid payload`() {
        val result = TelemetryRegistrationQrParser.parse("not-a-reg-key")
        assertTrue(result is TelemetryRegistrationQrParseResult.Invalid)
    }
}
