package com.viwa.android.hardware.scanner

import com.viwa.android.domain.model.BarcodeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerProtocolTest {
    @Test
    fun classifyBarcode_empPrefix_isEmployeeKey() {
        val e = ScannerProtocol.classifyBarcode("EMP:abc-1")
        assertTrue(e is BarcodeEvent.EmployeeKey)
        assertEquals("EMP:abc-1", (e as BarcodeEvent.EmployeeKey).code)
    }

    @Test
    fun classifyBarcode_keyPrefix_isEmployeeKey() {
        val e = ScannerProtocol.classifyBarcode("KEY-DM1NF5KS")
        assertTrue(e is BarcodeEvent.EmployeeKey)
        assertEquals("KEY-DM1NF5KS", (e as BarcodeEvent.EmployeeKey).code)
    }

    @Test
    fun classifyBarcode_regPrefix_isRegistrationKey() {
        val e = ScannerProtocol.classifyBarcode("REG:0123456789AB")
        assertTrue(e is BarcodeEvent.RegistrationKey)
        assertEquals("REG-0123456789AB", (e as BarcodeEvent.RegistrationKey).code)
    }

    @Test
    fun classifyBarcode_regDashPrefix_isRegistrationKey() {
        val e = ScannerProtocol.classifyBarcode("REG-0123456789AB")
        assertTrue(e is BarcodeEvent.RegistrationKey)
        assertEquals("REG-0123456789AB", (e as BarcodeEvent.RegistrationKey).code)
    }

    @Test
    fun classifyBarcode_jsonQr_isTelemetryRegistrationQr() {
        val json = """{"type":"VIWA_TELEMETRY_REGISTRATION","version":1,"registrationKey":"REG-0123456789AB"}"""
        val e = ScannerProtocol.classifyBarcode(json)
        assertTrue(e is BarcodeEvent.TelemetryRegistrationQr)
    }

    @Test
    fun classifyBarcode_digits_isProduct() {
        val e = ScannerProtocol.classifyBarcode("12345678")
        assertTrue(e is BarcodeEvent.ProductBarcode)
        assertEquals("12345678", (e as BarcodeEvent.ProductBarcode).code)
    }

    @Test
    fun classifyBarcode_clientPrefix_isLoyaltyCard() {
        val uuid = "2caaf0b2-2b7f-4c09-9bef-dafd984c9a66"
        val e = ScannerProtocol.classifyBarcode("CLIENT_$uuid")
        assertTrue(e is BarcodeEvent.ClientLoyaltyCard)
        assertEquals(uuid, (e as BarcodeEvent.ClientLoyaltyCard).clientId)
    }

    @Test
    fun classifyBarcode_lowercaseClientPrefix_isUnknown() {
        val uuid = "2caaf0b2-2b7f-4c09-9bef-dafd984c9a66"
        val e = ScannerProtocol.classifyBarcode("client_$uuid")
        assertTrue(e is BarcodeEvent.UnknownBarcode)
        assertEquals("client_$uuid", (e as BarcodeEvent.UnknownBarcode).raw)
    }

    @Test
    fun classifyBarcode_other_isUnknown() {
        val e = ScannerProtocol.classifyBarcode("random")
        assertTrue(e is BarcodeEvent.UnknownBarcode)
        assertEquals("random", (e as BarcodeEvent.UnknownBarcode).raw)
    }
}
