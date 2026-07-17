package com.wiva.android.data.remote.telemetry.mvp

import com.wiva.android.domain.model.TelemetryConfig

/** Построение WS URL из REST base URL и ответа enroll. */
object MvpTelemetryUrlResolver {
    fun resolveWsUrl(apiBaseUrl: String, enrolledWsUrl: String?, configuredWsUrl: String?): String {
        enrolledWsUrl?.trim()?.takeIf { it.isNotEmpty() && !TelemetryConfig.isLegacyWsUrl(it) }?.let { return it }
        configuredWsUrl?.trim()?.takeIf { it.isNotEmpty() && !TelemetryConfig.isLegacyWsUrl(it) }?.let { return it }
        return deriveFromApiBaseUrl(apiBaseUrl)
    }

    /** MVP WS для отображения в форме, когда stored/config пуст или legacy. */
    fun displayWsUrl(apiBaseUrl: String, storedWsUrl: String): String {
        val sanitized = TelemetryConfig.sanitizeWsUrlForMvp(storedWsUrl)
        return sanitized.ifBlank { deriveFromApiBaseUrl(apiBaseUrl) }
    }

    fun deriveFromApiBaseUrl(apiBaseUrl: String): String {
        val base = apiBaseUrl.trimEnd('/')
        val wsBase =
            when {
                base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
                base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
                else -> base
            }
        return "$wsBase/api/v1/machines/ws"
    }
}
