package com.wiva.android.data.remote.telemetry.mvp

/** Injectable epoch clock for JWT cache expiry and refresh skew. */
fun interface EpochMillisClock {
    fun epochMillis(): Long
}

class SystemEpochMillisClock : EpochMillisClock {
    override fun epochMillis(): Long = System.currentTimeMillis()
}
