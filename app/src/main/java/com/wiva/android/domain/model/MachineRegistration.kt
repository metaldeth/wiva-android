package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MachineRegistration(
    val machineId: String = "",
    val serialNumber: String = "",
    val kioskId: String = "",
 /** Legacy machine credential (`mch_…`) — только fallback до REG; не stable secret. */
    val machineKey: String = "",
    val machineCredential: String = "",
    val installationId: String = "",
    val reservationToken: String = "",
    val reservationExpiresAt: String = "",
    val wsProtocolUrl: String = "",
    val tokenEndpoint: String = "/api/v1/machines/token",
    val regKey: String = "",
 /** `stable_secret` после REG; `legacy_credential` до REG; пусто — не зарегистрирован. */
    val authScheme: String = "",
    val isRegistered: Boolean = false,
    val enrolled: Boolean = false,
 /** Из legacy `machineInfo`. */
    val organizationId: String = "",
    val modelId: String = "",
) {
    companion object {
        const val AUTH_SCHEME_STABLE_SECRET = "stable_secret"
        const val AUTH_SCHEME_LEGACY_CREDENTIAL = "legacy_credential"

        fun migrateLegacy(reg: MachineRegistration): MachineRegistration {
            val authScheme =
                when {
                    reg.authScheme.isNotBlank() -> reg.authScheme
                    reg.enrolled && reg.machineCredential.isBlank() && reg.machineKey.isBlank() ->
                        AUTH_SCHEME_STABLE_SECRET
                    else -> {
                        val credential =
                            when {
                                reg.machineCredential.isNotBlank() -> reg.machineCredential
                                reg.machineKey.startsWith("mch_") -> reg.machineKey
                                else -> reg.machineCredential
                            }
                        if (credential.isNotBlank()) AUTH_SCHEME_LEGACY_CREDENTIAL else ""
                    }
                }
            val credential =
                if (authScheme == AUTH_SCHEME_LEGACY_CREDENTIAL) {
                    when {
                        reg.machineCredential.isNotBlank() -> reg.machineCredential
                        reg.machineKey.startsWith("mch_") -> reg.machineKey
                        else -> reg.machineCredential
                    }
                } else {
                    ""
                }
            val registered = reg.enrolled || reg.isRegistered
            val serial = reg.serialNumber.ifBlank { "E-01" }.let {
                if (it == "E-01" && registered) it else it
            }
            return reg.copy(
                machineCredential = credential,
                machineKey = if (credential.isNotBlank()) credential else "",
                authScheme = authScheme,
                isRegistered = registered,
                enrolled = registered,
                serialNumber = serial,
                tokenEndpoint = reg.tokenEndpoint.ifBlank { "/api/v1/machines/token" },
                wsProtocolUrl =
                    if (TelemetryConfig.isLegacyWsUrl(reg.wsProtocolUrl)) {
                        ""
                    } else {
                        reg.wsProtocolUrl
                    },
            )
        }

        fun isEnrolled(reg: MachineRegistration): Boolean =
            reg.enrolled ||
                (reg.isRegistered &&
                    (reg.authScheme == AUTH_SCHEME_STABLE_SECRET || reg.machineCredential.isNotBlank()))
    }
}
