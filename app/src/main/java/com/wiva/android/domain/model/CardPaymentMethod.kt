package com.wiva.android.domain.model

import java.util.Locale

/**
 * Вариант оплаты **картой** (PAX через терминал или aQsi ридер). Не путать с [PaymentMethod] приложения.
 */
sealed interface CardPaymentMethod {
    data object Pax : CardPaymentMethod

    data object Aqsi : CardPaymentMethod

    companion object {
        const val STORAGE_PAX = "PAX"
        const val STORAGE_AQSI = "AQSI"

 /** `null`, после trim пустая строка, неизвестное значение → [Pax]. Регистр и пробелы по краям игнорируются. */
        fun fromStorageString(raw: String?): CardPaymentMethod {
            return when (raw?.trim()?.uppercase(Locale.US)) {
                STORAGE_AQSI -> Aqsi
                STORAGE_PAX -> Pax
                else -> Pax
            }
        }

        fun toStorageString(method: CardPaymentMethod): String =
            when (method) {
                Pax -> STORAGE_PAX
                Aqsi -> STORAGE_AQSI
            }
    }
}
