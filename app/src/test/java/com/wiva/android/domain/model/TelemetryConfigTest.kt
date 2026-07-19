package com.wiva.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryConfigTest {
    @Test
    fun `normalize clears legacy shaker ws url`() {
        val stored =
            TelemetryConfig(
                apiUrl = TelemetryConfig.DEFAULT_API_URL,
                wsUrl = "ws://185.46.8.39:8315/ws",
            )
        val normalized = TelemetryConfig.normalize(stored)
        assertEquals("", normalized.wsUrl)
    }

    @Test
    fun `sanitizeWsUrl keeps custom ws url`() {
        val custom = "wss://example.com/api/v1/machines/ws"
        assertEquals(custom, TelemetryConfig.sanitizeWsUrl(custom))
    }
}
