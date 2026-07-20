package com.viwa.android.data.payment.aqsi

internal data class AqsiResponseCode(
    val code: String,
    val approved: Boolean,
    val message: String,
    val timeout: Boolean = false,
)
