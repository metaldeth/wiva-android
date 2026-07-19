package com.wiva.android.services.telemetry

import com.wiva.android.domain.model.TelemetryConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryRegistrationScanApplierTest {
    @Test
    fun `apply fills reg key and serial from qr scan`() {
        val event =
            TelemetryRegistrationScanUiEvent(
                registrationKey = "REG-0123456789AB",
                serialNumber = "WIVA-000004",
                apiUrl = "https://194.67.74.147",
            )

        val result = TelemetryRegistrationScanApplier.apply(event, TelemetryConfig.DEFAULT_API_URL)

        assertEquals("REG-0123456789AB", result.regKey)
        assertEquals("WIVA-000004", result.serial)
        assertEquals(TelemetryConfig.DEFAULT_API_URL, result.apiUrl)
        assertEquals("QR регистрации считан", result.qrBanner)
        assertNull(result.urlWarning)
    }

    @Test
    fun `apply reg key only uses reg banner`() {
        val event = TelemetryRegistrationScanUiEvent(registrationKey = "REG-0123456789AB")

        val result = TelemetryRegistrationScanApplier.apply(event, TelemetryConfig.DEFAULT_API_URL)

        assertEquals("REG-0123456789AB", result.regKey)
        assertNull(result.serial)
        assertNull(result.apiUrl)
        assertEquals("REG-ключ считан", result.qrBanner)
        assertNull(result.urlWarning)
    }

    @Test
    fun `apply rejects untrusted qr apiUrl with soft warning`() {
        val event =
            TelemetryRegistrationScanUiEvent(
                registrationKey = "REG-0123456789AB",
                apiUrl = "https://evil.example.com",
            )

        val result = TelemetryRegistrationScanApplier.apply(event, TelemetryConfig.DEFAULT_API_URL)

        assertNull(result.apiUrl)
        assertEquals("QR регистрации считан", result.qrBanner)
        assertEquals(
            "QR указывает другой сервер (https://evil.example.com). " +
                "Сначала измените адрес API вручную, сохраните, затем повторите сканирование.",
            result.urlWarning,
        )
    }
}
