package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

/** Эндпоинты телеметрии. MVP-протокол включён по умолчанию. */
@Serializable
data class TelemetryConfig(
    val apiUrl: String = DEFAULT_API_URL,
    val wsUrl: String = "",
    val useMvpProtocol: Boolean = true,
 /** Legacy Keycloak — только если [useMvpProtocol] = false. */
    val keycloakUrl: String = DEFAULT_KEYCLOAK_URL,
    val keycloakRealm: String = DEFAULT_KEYCLOAK_REALM,
) {
    companion object {
        const val DEFAULT_API_URL = "https://194.67.74.147"
        const val DEFAULT_WS_URL = "ws://185.46.8.39:8315/ws"
        const val DEFAULT_KEYCLOAK_URL = "https://kk.ishaker.ru:4437"
        const val DEFAULT_KEYCLOAK_REALM = "machine-realm"

        fun isLegacyWsUrl(url: String?): Boolean {
            val normalized = url?.trim()?.removeSuffix("/").orEmpty()
            if (normalized.isEmpty()) return false
            val legacyNormalized = DEFAULT_WS_URL.removeSuffix("/")
            return normalized.equals(legacyNormalized, ignoreCase = true) ||
                normalized.contains("185.46.8.39:8315", ignoreCase = true)
        }

        /** Для MVP: legacy WS не сохраняем и не показываем как дефолт. */
        fun sanitizeWsUrlForMvp(wsUrl: String): String {
            val trimmed = wsUrl.trim()
            return if (isLegacyWsUrl(trimmed)) "" else trimmed
        }

        fun migrateLegacy(config: TelemetryConfig): TelemetryConfig {
            val ws =
                when {
                    config.useMvpProtocol -> sanitizeWsUrlForMvp(config.wsUrl)
                    config.wsUrl.isBlank() -> DEFAULT_WS_URL
                    else -> config.wsUrl
                }
            return config.copy(wsUrl = ws)
        }
    }
}
