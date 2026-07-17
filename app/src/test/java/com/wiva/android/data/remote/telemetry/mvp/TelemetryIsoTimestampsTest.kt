package com.wiva.android.data.remote.telemetry.mvp

import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryIsoTimestampsTest {
    @Test
    fun `nowUtc returns ISO8601 UTC suffix`() {
        // when
        val iso = TelemetryIsoTimestamps.nowUtc()
        // then
        assertTrue(iso.endsWith("Z"))
        assertTrue(iso.contains("T"))
    }
}
