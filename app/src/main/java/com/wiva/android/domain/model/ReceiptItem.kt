package com.wiva.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptItem(
    val name: String,
    val price: Int,
    val quantity: Int = 1,
)
