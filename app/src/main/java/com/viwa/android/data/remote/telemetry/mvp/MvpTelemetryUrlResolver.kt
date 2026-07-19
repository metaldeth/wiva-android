package com.viwa.android.data.remote.telemetry.mvp

import com.viwa.android.domain.model.TelemetryConfig

/** Построение WS URL из REST base URL и ответа enroll. */
object MvpTelemetryUrlResolver {
    fun resolveWsUrl(apiBaseUrl: String, enrolledWsUrl: String?, configuredWsUrl: String?): String {
        enrolledWsUrl?.trim()?.takeIf { it.isNotEmpty() && TelemetryConfig.sanitizeWsUrl(it).isNotEmpty() }?.let {
            return TelemetryConfig.sanitizeWsUrl(it)
        }
        configuredWsUrl?.trim()?.takeIf { it.isNotEmpty() && TelemetryConfig.sanitizeWsUrl(it).isNotEmpty() }?.let {
            return TelemetryConfig.sanitizeWsUrl(it)
        }
        return deriveFromApiBaseUrl(apiBaseUrl)
    }

    fun displayWsUrl(apiBaseUrl: String, storedWsUrl: String): String {
        val sanitized = TelemetryConfig.sanitizeWsUrl(storedWsUrl)
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
