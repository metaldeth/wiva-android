package com.wiva.android.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.wiva.android.data.remote.telemetry.KioskDeviceLocationBody
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Последняя известная точка NETWORK / GPS (.
 */
@Singleton
class WivaDeviceLocationReader
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun readLastKnownOrNull(): KioskDeviceLocationBody? {
        val fine =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        var best: android.location.Location? = null
        for (p in providers) {
            if (!lm.isProviderEnabled(p)) continue
            val loc =
                try {
                    lm.getLastKnownLocation(p)
                } catch (_: SecurityException) {
                    null
                } ?: continue
            if (best == null || loc.time > best.time) best = loc
        }
        val loc = best ?: return null
        return KioskDeviceLocationBody(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
            capturedAtEpochMillis = loc.time,
        )
    }
}
