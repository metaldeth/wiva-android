package com.wiva.android.domain.model

/** Классификация строки со сканера USB-serial. */
sealed class BarcodeEvent {
    data class ProductBarcode(val code: String) : BarcodeEvent()

    data class EmployeeKey(val code: String) : BarcodeEvent()

    data class RegistrationKey(val code: String) : BarcodeEvent()

    /** Карта лояльности / подписка: CLIENT_{uuid}. */
    data class ClientLoyaltyCard(val clientId: String) : BarcodeEvent()

    data class UnknownBarcode(val raw: String) : BarcodeEvent()
}
