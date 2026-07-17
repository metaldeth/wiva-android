package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MachineRegistration(
    val machineId: String = "",
    val serialNumber: String = "E-01",
    val kioskId: String = "",
 /** Секрет Keycloak / secretKey после регистрации в телеметрии (.secretKey`). */
    val machineKey: String = "",
    val regKey: String = "REG-SRIBC",
    val isRegistered: Boolean = false,
 /** Из входящего `machineInfo` ( `processMachineInfoResponse`). */
    val organizationId: String = "",
    val modelId: String = "",
)
