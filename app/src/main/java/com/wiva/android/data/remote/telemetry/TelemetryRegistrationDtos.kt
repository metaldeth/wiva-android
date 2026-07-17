package com.wiva.android.data.remote.telemetry

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponseDto(
    val secretKey: String,
    val type: String,
)

@Serializable
data class MachineRegistrationRequestDto(
    val modelName: String,
    val machineName: String,
    val serialNumber: String,
)
