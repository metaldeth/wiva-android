package com.wiva.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryConfigTest {
    @Test
    fun `migrateLegacy clears legacy ws for mvp mode`() {
        // given
        val stored =
            TelemetryConfig(
                apiUrl = TelemetryConfig.DEFAULT_API_URL,
                wsUrl = TelemetryConfig.DEFAULT_WS_URL,
                useMvpProtocol = true,
            )
        // when
        val migrated = TelemetryConfig.migrateLegacy(stored)
        // then
        assertEquals("", migrated.wsUrl)
    }

    @Test
    fun `migrateLegacy keeps default legacy ws for non-mvp mode`() {
        // given
        val stored =
            TelemetryConfig(
                wsUrl = "",
                useMvpProtocol = false,
            )
        // when
        val migrated = TelemetryConfig.migrateLegacy(stored)
        // then
        assertEquals(TelemetryConfig.DEFAULT_WS_URL, migrated.wsUrl)
    }

    @Test
    fun `isLegacyWsUrl detects default legacy endpoint`() {
        assertTrue(TelemetryConfig.isLegacyWsUrl(TelemetryConfig.DEFAULT_WS_URL))
        assertTrue(TelemetryConfig.isLegacyWsUrl("ws://185.46.8.39:8315/ws/"))
    }
}
