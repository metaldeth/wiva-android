package com.viwa.android.domain.model

import java.util.Locale

/**
 * Mock only for card acquiring. It is separate from controller mock because a service engineer
 * may need to test payment UI without a physical card reader.
 */
enum class CardPaymentMockMode {
    Disabled,
    TwoCan,
    Aqsi,
    ;

    companion object {
        const val STORAGE_DISABLED = "OFF"
        const val STORAGE_TWO_CAN = "TWOCAN"
        const val STORAGE_AQSI = "AQSI"

        fun fromStorageString(raw: String?): CardPaymentMockMode =
            when (raw?.trim()?.uppercase(Locale.US)) {
                STORAGE_TWO_CAN -> TwoCan
                STORAGE_AQSI -> Aqsi
                else -> Disabled
            }

        fun toStorageString(mode: CardPaymentMockMode): String =
            when (mode) {
                Disabled -> STORAGE_DISABLED
                TwoCan -> STORAGE_TWO_CAN
                Aqsi -> STORAGE_AQSI
            }
    }
}
