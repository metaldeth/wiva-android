package com.wiva.android.data.remote.telemetry.mvp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Thread-safe ISO-8601 UTC timestamps (API 25+, no java.time). */
object TelemetryIsoTimestamps {
    private val isoUtc: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

    fun nowUtc(): String = requireNotNull(isoUtc.get()).format(Date())
}
