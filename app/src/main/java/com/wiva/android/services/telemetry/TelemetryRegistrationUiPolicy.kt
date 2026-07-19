package com.wiva.android.services.telemetry

import com.wiva.android.BuildConfig

/** UI/feature flags for telemetry registration on the Connection tab. */
internal object TelemetryRegistrationUiPolicy {
    /** Legacy reserve-free-serial API — visible only in debug builds. */
    fun showReserveFreeSerialButton(): Boolean = BuildConfig.DEBUG
}
