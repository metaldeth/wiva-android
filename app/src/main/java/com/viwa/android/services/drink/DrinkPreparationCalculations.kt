package com.viwa.android.services.drink

import kotlin.math.roundToInt

/**
 * Чистые расчёты для готовки).
 * Время готовки ≈ время налива воды: round(waterMl / flowRateMlPerSec).
 */
object DrinkPreparationCalculations {
    fun waterMlForDrink(
        dosageWaterMl: Double,
        drinkVolumeMl: Int,
        recipeDrinkVolumeMl: Int,
    ): Double {
        if (
            !dosageWaterMl.isFinite() ||
            dosageWaterMl <= 0.0 ||
            drinkVolumeMl <= 0 ||
            recipeDrinkVolumeMl <= 0
        ) {
            return 0.0
        }
        return dosageWaterMl * (drinkVolumeMl.toDouble() / recipeDrinkVolumeMl.toDouble())
    }

    fun preparingTimeSec(
        waterMl: Double,
        flowRateMlPerSec: Double,
    ): Int {
        if (!waterMl.isFinite() || !flowRateMlPerSec.isFinite() || flowRateMlPerSec <= 0) {
            return 0
        }
        return (waterMl / flowRateMlPerSec).roundToInt()
    }
}
