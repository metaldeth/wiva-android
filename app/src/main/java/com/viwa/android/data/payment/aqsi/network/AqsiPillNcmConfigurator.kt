package com.viwa.android.data.payment.aqsi.network

import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Ensures host-side NCM link address [AqsiPillNetworkConstants.PILL_GATEWAY_HOST] is configured
 * on the USB ethernet interface when Pill is plugged in.
 */
@Singleton
class AqsiPillNcmConfigurator
@Inject
constructor() {
    suspend fun ensureHostLinkReady(router: AqsiPillNetworkRouter): Boolean {
        if (hasHostGatewayAddress()) {
            Timber.tag(TAG).i("NCM host IP already present on an interface")
            router.refreshNetworks()
            findNcmInterface()?.let { enablePillInternetSharing(it) }
            return true
        }

        val iface = findNcmInterface()
        if (iface == null) {
            Timber.tag(TAG).w("NCM setup skipped: no eth* interface found")
            return false
        }

        Timber.tag(TAG).i("NCM waiting for DHCP on %s (up to %d ms)", iface, DHCP_WAIT_MS)
        if (waitForHostAddress(DHCP_WAIT_MS)) {
            Timber.tag(TAG).i("NCM host IP appeared via DHCP")
            router.refreshNetworks()
            findNcmInterface()?.let { enablePillInternetSharing(it) }
            return true
        }

        Timber.tag(TAG).i("NCM DHCP timeout; assigning %s/24 on %s", HOST_IP, iface)
        val assigned = assignHostAddress(iface)
        if (assigned) {
            router.refreshNetworks()
            enablePillInternetSharing(iface)
        } else {
            Timber.tag(TAG).w("NCM manual assign failed for %s", iface)
        }
        return assigned
    }

    private suspend fun waitForHostAddress(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (hasHostGatewayAddress()) return true
            delay(DHCP_POLL_MS)
        }
        return hasHostGatewayAddress()
    }

    internal fun hasHostGatewayAddress(): Boolean =
        listNetworkInterfaces().any { iface ->
            iface.inetAddresses.toList().any { address ->
                address is Inet4Address && address.hostAddress == HOST_IP
            }
        }

    internal fun findNcmInterface(): String? {
        val ethInterfaces =
            listNetworkInterfaces()
                .map { it.name }
                .filter { it.startsWith("eth") }
                .sortedBy { ethNumber(it) }

        if (ethInterfaces.isEmpty()) return null

        val candidates =
            if (ethInterfaces.size > 1) {
                ethInterfaces.filter { it != "eth0" }
            } else {
                ethInterfaces
            }

        return candidates.filter { ethNumber(it) >= 2 }.lastOrNull()
            ?: candidates.lastOrNull()
    }

    private fun enablePillInternetSharing(pillIface: String) {
        val uplink = findWifiInterface() ?: "wlan0"
        val commands =
            listOf(
                "echo 1 > /proc/sys/net/ipv4/ip_forward",
                "iptables -t nat -C POSTROUTING -o $uplink -j MASQUERADE 2>/dev/null || iptables -t nat -A POSTROUTING -o $uplink -j MASQUERADE",
                "iptables -C FORWARD -i $pillIface -o $uplink -j ACCEPT 2>/dev/null || iptables -A FORWARD -i $pillIface -o $uplink -j ACCEPT",
                "iptables -C FORWARD -i $uplink -o $pillIface -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || iptables -A FORWARD -i $uplink -o $pillIface -m state --state RELATED,ESTABLISHED -j ACCEPT",
            )
        val ok = commands.all { AqsiPillShellRunner.runShell(it) }
        Timber.tag(TAG).i("Pill NAT sharing pill=%s uplink=%s ok=%s", pillIface, uplink, ok)
    }

    private fun findWifiInterface(): String? =
        listNetworkInterfaces()
            .firstOrNull { it.name.startsWith("wlan") && it.isUp }
            ?.name

    private fun assignHostAddress(iface: String): Boolean =
        AqsiPillShellRunner.runIp("addr", "add", "$HOST_IP/24", "dev", iface)

    private fun listNetworkInterfaces(): List<NetworkInterface> =
        try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (error: Exception) {
            Timber.tag(TAG).w(error, "Failed to enumerate network interfaces")
            emptyList()
        }

    private fun ethNumber(name: String): Int = name.removePrefix("eth").toIntOrNull() ?: 0

    companion object {
        private const val TAG = "AQSI_SETUP"
        private const val HOST_IP = AqsiPillNetworkConstants.PILL_GATEWAY_HOST
        private const val DHCP_WAIT_MS = 5_000L
        private const val DHCP_POLL_MS = 250L
    }
}
