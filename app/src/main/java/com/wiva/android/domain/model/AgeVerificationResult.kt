package com.wiva.android.domain.model

sealed class AgeVerificationResult {
    data object Approved : AgeVerificationResult()

    data class Rejected(val reason: String) : AgeVerificationResult()

    data object Cancelled : AgeVerificationResult()

    data object Timeout : AgeVerificationResult()
}
