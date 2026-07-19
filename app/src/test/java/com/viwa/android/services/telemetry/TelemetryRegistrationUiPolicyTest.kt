package com.viwa.android.services.telemetry

import com.viwa.android.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryRegistrationUiPolicyTest {
    @Test
    fun `reserve free serial button gated by debug build`() {
        assertEquals(BuildConfig.DEBUG, TelemetryRegistrationUiPolicy.showReserveFreeSerialButton())
    }
}
