package com.wiva.android.services.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryWriteOffMathTest {
    @Test
    fun fullVolume_standardConcentration_matchesDosage() {
        val (p, w) =
            InventoryWriteOffMath.computeProductAndWaterMl(
                drinkVolume = 300,
                dosageProduct = 10.0,
                dosageWater = 290.0,
                volumeMl = 300,
                concentrationRatio = 1.0,
            )
        assertEquals(10.0, p, 1e-6)
        assertEquals(290, w)
    }

    @Test
    fun halfVolume_halvesProductAndWater() {
        val (p, w) =
            InventoryWriteOffMath.computeProductAndWaterMl(
                drinkVolume = 300,
                dosageProduct = 10.0,
                dosageWater = 290.0,
                volumeMl = 150,
                concentrationRatio = 1.0,
            )
        assertEquals(5.0, p, 1e-6)
        assertEquals(145, w)
    }

    @Test
    fun concentration_scalesProductOnly() {
        val (p, w) =
            InventoryWriteOffMath.computeProductAndWaterMl(
                drinkVolume = 300,
                dosageProduct = 10.0,
                dosageWater = 290.0,
                volumeMl = 300,
                concentrationRatio = 2.0,
            )
        assertEquals(20.0, p, 1e-6)
        assertEquals(290, w)
    }

    @Test
    fun zeroDrinkVolume_returnsZeros() {
        val (p, w) =
            InventoryWriteOffMath.computeProductAndWaterMl(
                drinkVolume = 0,
                dosageProduct = 10.0,
                dosageWater = 290.0,
                volumeMl = 300,
                concentrationRatio = 1.0,
            )
        assertEquals(0.0, p, 1e-6)
        assertEquals(0, w)
    }
}
