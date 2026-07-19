package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

/** Flat domain cell — mirrors CellFull (downlink / local store after snapshot). */
@Serializable
data class TelemetryCell(
    val uuid: String,
    val cellNumber: Int,
    val productUuid: String? = null,
    val productName: String? = null,
    val tasteMediaKey: String? = null,
    val blockVolume: Int = 0,
    val sosVolume: Int = 0,
    val volume: Int = 0,
    val maxVolume: Int,
    val dosage1Price: Int? = null,
    val dosage2Price: Int? = null,
)
