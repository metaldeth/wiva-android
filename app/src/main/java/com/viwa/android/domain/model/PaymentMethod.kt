package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod {
    NONE,
    SBP,
    CARD,
}
