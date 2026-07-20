package com.viwa.android.domain.model

/**
 * Карточный метод оплаты — только USB aQsi Pill (Arcus2).
 * Legacy storage `PAX` / `TWOCAN` мигрируется в [Aqsi].
 */
sealed interface CardPaymentMethod {
    data object Aqsi : CardPaymentMethod

    companion object {
        const val STORAGE_AQSI = "AQSI"
        const val STORAGE_LEGACY_PAX = "PAX"

        fun fromStorageString(raw: String?): CardPaymentMethod = Aqsi

        fun toStorageString(method: CardPaymentMethod): String = STORAGE_AQSI
    }
}
