package com.wiva.android.domain.model

data class SBPLink(
    val orderId: String,
    val url: String,
    val qrData: String,
)

sealed class SBPStatus {
    data object Pending : SBPStatus()

    data object Success : SBPStatus()

    data class Failed(val reason: String) : SBPStatus()

    data object Cancelled : SBPStatus()
}
