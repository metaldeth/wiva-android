package com.wiva.android.hardware.controller

import org.junit.Assert.assertEquals
import org.junit.Test

class FlowTemperatureDecodeTest {

    @Test
    fun zeroStaysZero() {
        assertEquals(0, decodeFlowTemperatureByte(0))
    }

    @Test
    fun positiveDirect() {
        assertEquals(22, decodeFlowTemperatureByte(22))
        assertEquals(45, decodeFlowTemperatureByte(45))
        assertEquals(127, decodeFlowTemperatureByte(127))
    }

    @Test
    fun negativePerDeviceFormula() {
        assertEquals(-1, decodeFlowTemperatureByte(254))
        assertEquals(-5, decodeFlowTemperatureByte(250))
    }

    @Test
    fun highByteRangeNegative() {
        assertEquals(-127, decodeFlowTemperatureByte(128))
        assertEquals(0, decodeFlowTemperatureByte(255))
    }
}
