package com.viwa.android.services.drink

import org.junit.Assert.assertEquals
import org.junit.Test

class DrinkPreparationCalculationsTest {
    @Test
    fun waterMlForDrink_matchesRecipeRatio() {
 // 250 мл воды на 300 мл напитка -> для 700 мл воды будет 250*(700/300)=583.33.
        assertEquals(
            583.3333333333334,
            DrinkPreparationCalculations.waterMlForDrink(
                dosageWaterMl = 250.0,
                drinkVolumeMl = 700,
                recipeDrinkVolumeMl = 300,
            ),
            1e-9,
        )
    }

    @Test
    fun waterMlForDrink_invalidInput_returnsZero() {
        assertEquals(0.0, DrinkPreparationCalculations.waterMlForDrink(0.0, 300, 300), 0.0)
        assertEquals(0.0, DrinkPreparationCalculations.waterMlForDrink(200.0, 0, 300), 0.0)
        assertEquals(0.0, DrinkPreparationCalculations.waterMlForDrink(200.0, 300, 0), 0.0)
    }

    @Test
    fun preparingTime_matchesElectronWaterOverFlowRate() {
 // waterMl=90, flow 20 -> round(4.5)=5 (
        assertEquals(5, DrinkPreparationCalculations.preparingTimeSec(90.0, 20.0))
        assertEquals(4, DrinkPreparationCalculations.preparingTimeSec(90.0, 22.0))
    }

    @Test
    fun preparingTime_invalidFlow_returnsZero() {
        assertEquals(0, DrinkPreparationCalculations.preparingTimeSec(100.0, 0.0))
        assertEquals(0, DrinkPreparationCalculations.preparingTimeSec(100.0, -1.0))
    }
}
