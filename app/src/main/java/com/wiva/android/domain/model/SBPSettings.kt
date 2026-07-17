package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

const val SBP_DEV_DEFAULT_KEY = "25410201ubuntu"
const val SBP_DEV_DEFAULT_SPOT_ID = "8267501533"
const val SBP_DEV_DEFAULT_TIMEOUT_SEC = 120

@Serializable
data class SBPSettings(
    val spotId: String = SBP_DEV_DEFAULT_SPOT_ID,
    val key: String = SBP_DEV_DEFAULT_KEY,
    val timeoutInSeconds: Int = SBP_DEV_DEFAULT_TIMEOUT_SEC,
    val lastOrderId: String = "",
    val lastRequestNumber: String = "",
    val lastSign: String = "",
)
