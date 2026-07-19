package com.viwa.android.data.remote.telemetry.mvp

import com.viwa.android.domain.model.TelemetryConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryUrlValidatorTest {
    @Test
    fun `validateStrict accepts https origin and normalizes default port`() {
        val result = TelemetryUrlValidator.validateStrict("https://194.67.74.147")
        assertTrue(result is TelemetryUrlValidator.Result.Valid)
        assertEquals("https://194.67.74.147", (result as TelemetryUrlValidator.Result.Valid).normalizedOrigin)
    }

    @Test
    fun `validateStrict normalizes explicit port 443`() {
        val result = TelemetryUrlValidator.validateStrict("https://194.67.74.147:443")
        assertTrue(result is TelemetryUrlValidator.Result.Valid)
        assertEquals("https://194.67.74.147", (result as TelemetryUrlValidator.Result.Valid).normalizedOrigin)
    }

    @Test
    fun `validateStrict rejects http file and path`() {
        assertTrue(TelemetryUrlValidator.validateStrict("http://194.67.74.147") is TelemetryUrlValidator.Result.Invalid)
        assertTrue(TelemetryUrlValidator.validateStrict("file:///etc/passwd") is TelemetryUrlValidator.Result.Invalid)
        assertTrue(TelemetryUrlValidator.validateStrict("https://194.67.74.147/api/v1") is TelemetryUrlValidator.Result.Invalid)
    }

    @Test
    fun `validateStrict rejects userinfo query fragment`() {
        assertTrue(
            TelemetryUrlValidator.validateStrict("https://user:pass@194.67.74.147") is
                TelemetryUrlValidator.Result.Invalid,
        )
        assertTrue(
            TelemetryUrlValidator.validateStrict("https://194.67.74.147?x=1") is TelemetryUrlValidator.Result.Invalid,
        )
        assertTrue(
            TelemetryUrlValidator.validateStrict("https://194.67.74.147#frag") is TelemetryUrlValidator.Result.Invalid,
        )
    }

    @Test
    fun `validateTrustedCandidate accepts matching persisted host and port`() {
        val result =
            TelemetryUrlValidator.validateTrustedCandidate(
                "https://194.67.74.147",
                TelemetryConfig.DEFAULT_API_URL,
            )
        assertTrue(result is TelemetryUrlValidator.Result.Valid)
    }

    @Test
    fun `validateTrustedCandidate rejects different host`() {
        val result =
            TelemetryUrlValidator.validateTrustedCandidate(
                "https://evil.example.com",
                TelemetryConfig.DEFAULT_API_URL,
            )
        assertTrue(result is TelemetryUrlValidator.Result.Invalid)
        assertTrue((result as TelemetryUrlValidator.Result.Invalid).reason.contains("другой сервер"))
    }
}
