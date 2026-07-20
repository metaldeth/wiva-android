package com.viwa.android.hardware.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlowTemperatureStoreTest {

    private val store = FlowTemperatureStore()

    @Test
    fun `should prefer T0 for telemetry when both known`() {
        // given
        store.update(t0 = 22, t1 = 30)

        // when / then
        assertEquals(22.0, store.temperatureCForTelemetry()!!, 0.0)
    }

    @Test
    fun `should use T1 when T0 is unknown`() {
        // given
        store.update(t0 = null, t1 = 15)

        // when / then
        assertEquals(15.0, store.temperatureCForTelemetry()!!, 0.0)
    }

    @Test
    fun `should return null when neither sensor known`() {
        // given
        store.update(t0 = null, t1 = null)

        // when / then
        assertNull(store.temperatureCForTelemetry())
    }

    @Test
    fun `should report zero Celsius from T0`() {
        // given
        store.update(t0 = 0, t1 = 20)

        // when / then
        assertEquals(0.0, store.temperatureCForTelemetry()!!, 0.0)
    }
}
