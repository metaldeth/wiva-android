package com.wiva.android.domain.model

import java.util.Locale

enum class CardPaymentMockOutcome {
    Approved,
    Declined,
    Cancelled,
    Timeout,
    ;

    companion object {
        const val STORAGE_APPROVED = "APPROVED"
        const val STORAGE_DECLINED = "DECLINED"
        const val STORAGE_CANCELLED = "CANCELLED"
        const val STORAGE_TIMEOUT = "TIMEOUT"

        fun fromStorageString(raw: String?): CardPaymentMockOutcome =
            when (raw?.trim()?.uppercase(Locale.US)) {
                STORAGE_DECLINED -> Declined
                STORAGE_CANCELLED -> Cancelled
                STORAGE_TIMEOUT -> Timeout
                else -> Approved
            }

        fun toStorageString(outcome: CardPaymentMockOutcome): String =
            when (outcome) {
                Approved -> STORAGE_APPROVED
                Declined -> STORAGE_DECLINED
                Cancelled -> STORAGE_CANCELLED
                Timeout -> STORAGE_TIMEOUT
            }
    }
}
