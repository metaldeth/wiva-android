package com.viwa.android.services.telemetry

/** Упрощённая строка продажи — legacy uplink удалён, тип оставлен для callers. */
data class SaleImportItem(
    val drinkId: Int,
    val volume: Int,
    val price: Double,
    val payMethod: String?,
    val totalChargedRub: Double? = null,
)
