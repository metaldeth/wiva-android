package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReserveSerialRequestDto(
    val installationId: String? = null,
)

@Serializable
data class ReserveSerialResponseDto(
    val serialNumber: String,
    val reservationToken: String,
    val expiresAt: String,
)

@Serializable
data class EnrollDeviceDto(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
)

@Serializable
data class EnrollAppDto(
    val versionName: String,
    val versionCode: Int,
)

@Serializable
data class EnrollRequestDto(
    val installationId: String,
    val serialNumber: String,
    val reservationToken: String? = null,
    val credential: String,
    val rebind: Boolean = false,
    val device: EnrollDeviceDto,
    val app: EnrollAppDto,
)

@Serializable
data class EnrollResponseDto(
    val machineId: String,
    val serialNumber: String,
    val wsUrl: String? = null,
    val wsProtocolUrl: String? = null,
    val protocolVersion: Int? = null,
    val heartbeatIntervalSeconds: Int? = null,
)

@Serializable
data class EnrollErrorBodyDto(
    val code: String? = null,
    val message: String? = null,
)

/** Исходящий/входящий WS-конверт simple-telemetry MVP. */
@Serializable
data class MvpWsEnvelopeDto(
    val type: String,
    val messageId: String,
    val sentAt: String,
    val payload: JsonElement? = null,
    val correlationId: String? = null,
)

@Serializable
data class MvpHelloPayloadDto(
    val serialNumber: String,
    val protocolVersion: Int = 1,
    val heartbeatIntervalSeconds: Int = 30,
)

@Serializable
data class MvpHeartbeatPayloadDto(
    val state: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val temperatureC: Double? = null,
)
