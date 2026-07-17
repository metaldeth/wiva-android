package com.wiva.android.domain.model.customer

/**
 * Переключатель воды для команды WaterPourByTouch (0xD0), байт SelW.
 * 0 — фильтрованная, 1 — холодная, 2 — газированная.
 */
enum class FlowWaterPourType(val selByte: Int) {
    Filtered(0),
    Cold(1),
    Sparkling(2),
    ;

    val shortLabel: String
        get() =
            when (this) {
                Filtered -> "Фильтр"
                Cold -> "Холодная"
                Sparkling -> "Газированная"
            }
}
