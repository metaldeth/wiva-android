package com.viwa.android.data.local.sales

import kotlinx.serialization.Serializable

@Serializable
enum class PendingSaleStatus {
    PENDING,
    SENT,
}

@Serializable
data class PendingSale(
    val saleId: String,
    val soldAt: String,
    val drinkId: Int,
    val volumeMl: Int,
    val amountRub: Double,
    val payMethod: String,
    val attempts: Int = 0,
    val nextRetryAtMillis: Long = 0L,
    val status: PendingSaleStatus = PendingSaleStatus.PENDING,
)
