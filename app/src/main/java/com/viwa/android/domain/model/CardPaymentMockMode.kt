package com.viwa.android.domain.model

import java.util.Locale

/** Mock only for card acquiring (USB aQsi). */
enum class CardPaymentMockMode {
    Disabled,
    Aqsi,
    ;

    companion object {
        const val STORAGE_DISABLED = "OFF"
        const val STORAGE_AQSI = "AQSI"
        const val STORAGE_LEGACY_TWO_CAN = "TWOCAN"

        fun fromStorageString(raw: String?): CardPaymentMockMode =
            when (raw?.trim()?.uppercase(Locale.US)) {
                STORAGE_AQSI -> Aqsi
                STORAGE_LEGACY_TWO_CAN -> Disabled
                else -> Disabled
            }

        fun toStorageString(mode: CardPaymentMockMode): String =
            when (mode) {
                Disabled -> STORAGE_DISABLED
                Aqsi -> STORAGE_AQSI
            }
    }
}
