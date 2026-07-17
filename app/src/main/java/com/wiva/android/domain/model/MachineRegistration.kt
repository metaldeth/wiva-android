package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MachineRegistration(
    val machineId: String = "",
    val serialNumber: String = "",
    val kioskId: String = "",
 /** Machine credential (`mch_…`) после enroll в MVP или legacy secretKey. */
    val machineKey: String = "",
    val machineCredential: String = "",
    val installationId: String = "",
    val reservationToken: String = "",
    val reservationExpiresAt: String = "",
    val wsProtocolUrl: String = "",
    val regKey: String = "",
    val isRegistered: Boolean = false,
    val enrolled: Boolean = false,
 /** Из legacy `machineInfo`. */
    val organizationId: String = "",
    val modelId: String = "",
) {
    companion object {
        fun migrateLegacy(reg: MachineRegistration): MachineRegistration {
            val credential =
                when {
                    reg.machineCredential.isNotBlank() -> reg.machineCredential
                    reg.machineKey.startsWith("mch_") -> reg.machineKey
                    else -> reg.machineCredential
                }
            val registered = reg.enrolled || reg.isRegistered
            val serial = reg.serialNumber.ifBlank { "E-01" }.let {
                if (it == "E-01" && registered) it else it
            }
            return reg.copy(
                machineCredential = credential,
                machineKey = if (credential.isNotBlank()) credential else reg.machineKey,
                isRegistered = registered,
                enrolled = registered,
                serialNumber = serial,
                wsProtocolUrl =
                    if (TelemetryConfig.isLegacyWsUrl(reg.wsProtocolUrl)) {
                        ""
                    } else {
                        reg.wsProtocolUrl
                    },
            )
        }

        fun isEnrolled(reg: MachineRegistration): Boolean =
            reg.enrolled || (reg.isRegistered && reg.machineCredential.isNotBlank())
    }
}
