package com.viwa.android.hardware.controller

import javax.inject.Inject
import javax.inject.Singleton

/** Последние значения T0/T1 с контроллера для heartbeat телеметрии. */
@Singleton
class FlowTemperatureStore
@Inject
constructor() {
    @Volatile
    private var t0C: Int? = null

    @Volatile
    private var t1C: Int? = null

    fun update(t0: Int?, t1: Int?) {
        t0C = t0
        t1C = t1
    }

    /** T0 (вода) приоритетнее; если T0 неизвестен — T1; иначе null. */
    fun temperatureCForTelemetry(): Double? = (t0C ?: t1C)?.toDouble()
}
