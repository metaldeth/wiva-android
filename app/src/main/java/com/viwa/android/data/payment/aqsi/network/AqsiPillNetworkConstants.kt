package com.viwa.android.data.payment.aqsi.network

/** USB-NCM subnet used by AQSI Pill (host side is typically .1, reader .123). */
internal object AqsiPillNetworkConstants {
    const val PILL_HOST = "192.168.137.123"
    const val PILL_GATEWAY_HOST = "192.168.137.1"
    const val PILL_SUBNET_PREFIX = "192.168.137."
    const val JPAY_PORT = 4433
    const val SOCKS_PORT = 1080
}
