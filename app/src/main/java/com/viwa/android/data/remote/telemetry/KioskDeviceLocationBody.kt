package com.viwa.android.data.remote.telemetry

import kotlinx.serialization.Serializable

/** JSON внутри envelope `kioskDeviceLocationReport` (. */
@Serializable
data class KioskDeviceLocationBody(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val capturedAtEpochMillis: Long? = null,
)
