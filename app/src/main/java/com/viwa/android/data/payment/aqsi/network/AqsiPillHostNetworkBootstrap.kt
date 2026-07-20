package com.viwa.android.data.payment.aqsi.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Full Pill host network setup from app code — no adb.
 * Order: clear stale proxy → NCM IP → Wi‑Fi bind → SOCKS :1080 → NAT (if su allows).
 */
@Singleton
class AqsiPillHostNetworkBootstrap
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val ncmConfigurator: AqsiPillNcmConfigurator,
    private val networkRouter: AqsiPillNetworkRouter,
    private val socksForwarder: AqsiPillSocksForwarder,
) {
    suspend fun runWhenPillPresent(): HostNetworkStatus {
        val proxyCleared = AqsiPillShellRunner.clearStaleHttpProxy(context)
        val ncmReady = ncmConfigurator.ensureHostLinkReady(networkRouter)
        networkRouter.refreshNetworks()
        if (ncmReady) {
            socksForwarder.ensureStarted()
        } else if (ncmConfigurator.hasHostGatewayAddress()) {
            socksForwarder.ensureStarted()
        }
        val wifiReachable = probeWifiInternet()
        val status =
            HostNetworkStatus(
                httpProxyCleared = proxyCleared,
                ncmReady = ncmReady,
                wifiProcessBound = networkRouter.isProcessBoundToWifi(),
                wifiInternetProbe = wifiReachable,
                socksStarted = ncmReady,
            )
        Timber.tag(TAG).i("host network bootstrap: %s", status)
        return status
    }

    private fun probeWifiInternet(): Boolean =
        runCatching {
            Socket().use { socket ->
                networkRouter.connectForInternet(socket, "1.1.1.1", 443, 4_000)
                true
            }
        }.getOrElse {
            Timber.tag(TAG).w(it, "Wi‑Fi internet probe failed")
            false
        }

    data class HostNetworkStatus(
        val httpProxyCleared: Boolean,
        val ncmReady: Boolean,
        val wifiProcessBound: Boolean,
        val wifiInternetProbe: Boolean,
        val socksStarted: Boolean,
    ) {
        val readyForJpay: Boolean
            get() = ncmReady && wifiProcessBound && wifiInternetProbe
    }

    companion object {
        private const val TAG = "AQSI_SETUP"
    }
}
