package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

/** Эндпоинты телеметрии по умолчанию. */
@Serializable
data class TelemetryConfig(
    val apiUrl: String = DEFAULT_API_URL,
    val wsUrl: String = DEFAULT_WS_URL,
    val keycloakUrl: String = DEFAULT_KEYCLOAK_URL,
    val keycloakRealm: String = DEFAULT_KEYCLOAK_REALM,
) {
    companion object {
        const val DEFAULT_API_URL = "https://dev.ishaker.ru"
        const val DEFAULT_WS_URL = "ws://185.46.8.39:8315/ws"
        const val DEFAULT_KEYCLOAK_URL = "https://kk.ishaker.ru:4437"
        const val DEFAULT_KEYCLOAK_REALM = "machine-realm"
    }
}
