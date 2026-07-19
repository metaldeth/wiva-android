package com.viwa.android.services.drink

import org.junit.Assert.assertEquals
import org.junit.Test

class ChooseDrinkBodyBuilderTest {
    @Test
    fun matchesElectronDrinkSelectionServiceTest_container1_dispenserAndWater() {
        val body =
            ChooseDrinkBodyBuilder.build(
                physicalPort = 9,
                dispenserWorkTimeSec = 3.5,
                waterMl = 90.0,
                tof = 0,
            )
        assertEquals(0x01, body[0].toInt() and 0xff)
        assertEquals(9, body[1].toInt() and 0xff)
        assertEquals(35, body[2].toInt() and 0xff)
        assertEquals(9, body[3].toInt() and 0xff)
        assertEquals(0, body[8].toInt() and 0xff)
    }
}
