package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MaxSettings(
    val extApiToken: String = "I1QVLuvPQW5-eigRyHxUifYRfQr5Qg9A6k7nyVJPLR-gsc7jbb655nVvVSarEeW32UoJ4_8_Aac",
    val verificationDetailsEnabled: Boolean = false,
)
