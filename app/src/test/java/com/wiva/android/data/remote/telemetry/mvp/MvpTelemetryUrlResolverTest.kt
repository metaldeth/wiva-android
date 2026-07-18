package com.wiva.android.data.remote.telemetry.mvp

import com.wiva.android.domain.model.TelemetryConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MvpTelemetryUrlResolverTest {
    private val defaultApi = TelemetryConfig.DEFAULT_API_URL
    private val expectedDerived = "wss://194.67.74.147/api/v1/machines/ws"
    private val legacyWs = TelemetryConfig.DEFAULT_WS_URL

    @Test
    fun `resolve prefers enrolled ws url`() {
        assertEquals(
            "wss://telemetry.example/ws",
            MvpTelemetryUrlResolver.resolveWsUrl(
                apiBaseUrl = "https://telemetry.example",
                enrolledWsUrl = "wss://telemetry.example/ws",
                configuredWsUrl = legacyWs,
            ),
        )
    }

    @Test
    fun `resolve builds wss path from https api base`() {
        assertEquals(
            "wss://telemetry.example/api/v1/machines/ws",
            MvpTelemetryUrlResolver.resolveWsUrl(
                apiBaseUrl = "https://telemetry.example",
                enrolledWsUrl = null,
                configuredWsUrl = "",
            ),
        )
    }

    @Test
    fun `resolve ignores legacy ws and derives from api base`() {
        assertEquals(
            expectedDerived,
            MvpTelemetryUrlResolver.resolveWsUrl(
                apiBaseUrl = defaultApi,
                enrolledWsUrl = legacyWs,
                configuredWsUrl = legacyWs,
            ),
        )
    }

    @Test
    fun `display ws url replaces legacy stored value with derived mvp ws`() {
        assertEquals(
            expectedDerived,
            MvpTelemetryUrlResolver.displayWsUrl(defaultApi, legacyWs),
        )
    }

    @Test
    fun `display ws url keeps explicit non-legacy configured url`() {
        assertEquals(
            "wss://custom.example/ws",
            MvpTelemetryUrlResolver.displayWsUrl(defaultApi, "wss://custom.example/ws"),
        )
    }
}
