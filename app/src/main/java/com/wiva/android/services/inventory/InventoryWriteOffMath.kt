package com.wiva.android.services.inventory

import kotlin.math.roundToInt

/**
 * Расчёт списания по рецепту).
 * @return пара: списание продукта (в единицах dosage.product) и приращение счётчика воды (мл).
 */
internal object InventoryWriteOffMath {
    fun computeProductAndWaterMl(
        drinkVolume: Int,
        dosageProduct: Double,
        dosageWater: Double,
        volumeMl: Int,
        concentrationRatio: Double,
    ): Pair<Double, Int> {
        if (drinkVolume <= 0) return 0.0 to 0
        val ratio = volumeMl.toDouble() / drinkVolume
        val productWriteOff = dosageProduct * ratio * concentrationRatio
        val waterMl = (dosageWater * ratio).roundToInt()
        return productWriteOff to waterMl
    }
}
