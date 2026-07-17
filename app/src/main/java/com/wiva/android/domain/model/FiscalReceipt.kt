package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FiscalReceipt(
    val receiptId: String,
    val kktId: String,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: FiscalStatus,
    val nuid: String? = null,
    val qnuid: String? = null,
    val checkPageUrl: String? = null,
)

@Serializable
enum class FiscalStatus {
    SUCCESS,
    FAILED,
    PENDING,
}
