package com.viwa.android.services.calibration

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SyrupCalibrationMathTest {
    @Test
    fun `computeNewConversionFactor matches electron formula`() {
        val newCf = SyrupCalibrationMath.computeNewConversionFactor(4.0, 32.0, 30.0)
        assertEquals(4.0 * (32.0 / 30.0), newCf, 1e-9)
    }

    @Test
    fun `computeNewConversionFactor rejects non-positive volumes`() {
        assertThrows(IllegalArgumentException::class.java) {
            SyrupCalibrationMath.computeNewConversionFactor(1.0, 0.0, 30.0)
        }
    }

    @Test
    fun `physicalPort is container plus eight clamped`() {
        assertEquals(9, SyrupCalibrationMath.physicalPort(1))
        assertEquals(14, SyrupCalibrationMath.physicalPort(6))
    }

    @Test
    fun `buildCalibratePourBody matches spec`() {
 // container 1 → port 9; target 30 ml, CF 4 → 7.5 s → 75 tenths → 75 > 255? no, 75
        val body = SyrupCalibrationMath.buildCalibratePourBody(1, 30.0, 4.0)
        assertArrayEquals(
            intArrayOf(0x09, 0, 9, 75, 0),
            body.map { it.toInt() and 0xff }.toIntArray(),
        )
    }

    @Test
    fun `dispenserTimeTenths clamps to 255`() {
        val tenths = SyrupCalibrationMath.dispenserTimeTenths(10000.0, 1.0)
        assertEquals(255, tenths)
    }
}
