package com.viwa.android.data.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.text.Charsets
import okhttp3.Dns
import org.json.JSONObject
import timber.log.Timber

/**
 * DNS для HTTPS к Paymaster: на части эмуляторов системный резолв даёт
 * `UnknownHostException` / EAI_NODATA для [PAYMASTER_HOST] при рабочем интернете.
 * Сначала предпочитаем IPv4 (часто ломается только IPv6), при полном провале —
 * A-запись через публичный DNS по HTTPS (Google, затем Cloudflare).
 */
class PaymasterFriendlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val fromSystem =
            runCatching {
                val list = Dns.SYSTEM.lookup(hostname)
                ipv4First(list)
            }
        if (fromSystem.isSuccess) {
            return fromSystem.getOrThrow()
        }
        val err = fromSystem.exceptionOrNull()
        if (hostname.equals(PAYMASTER_HOST, ignoreCase = true) && err is UnknownHostException) {
            Timber.w(err, "System DNS failed for %s, trying public DNS-over-HTTPS", hostname)
            val encoded = URLEncoder.encode(hostname, Charsets.UTF_8.name())
            val ip =
                resolveARecordViaHttps(googleDnsUrl(encoded), dnsJsonAccept = false)
                    ?: resolveARecordViaHttps(cloudflareDnsUrl(encoded), dnsJsonAccept = true)
            if (ip != null) {
                Timber.i("Resolved %s → %s via DoH fallback", hostname, ip)
                return listOf(InetAddress.getByName(ip))
            }
        }
        throw err ?: UnknownHostException(hostname)
    }

    private fun googleDnsUrl(encodedName: String): String =
        "https://dns.google/resolve?name=$encodedName&type=A"

    private fun cloudflareDnsUrl(encodedName: String): String =
        "https://cloudflare-dns.com/dns-query?name=$encodedName&type=A"

    private fun ipv4First(addresses: List<InetAddress>): List<InetAddress> =
        addresses.sortedWith(compareBy { if (it is Inet4Address) 0 else 1 })

    private fun resolveARecordViaHttps(
        urlString: String,
        dnsJsonAccept: Boolean,
    ): String? {
        return runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            if (dnsJsonAccept) {
                conn.setRequestProperty("Accept", "application/dns-json")
            }
            if (conn.responseCode != 200) {
                Timber.d("DoH %s HTTP %s", url.host, conn.responseCode)
                return@runCatching null
            }
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseARecordFromDnsJson(text)
        }.getOrElse { e ->
            Timber.d(e, "DoH lookup failed for %s", urlString.take(80))
            null
        }
    }

    private fun parseARecordFromDnsJson(jsonText: String): String? {
        val json = JSONObject(jsonText)
        if (json.optInt("Status", -1) != 0) return null
        val answer = json.optJSONArray("Answer") ?: return null
        for (i in 0 until answer.length()) {
            val o = answer.optJSONObject(i) ?: continue
            if (o.optInt("type") != DNS_TYPE_A) continue
            val data = o.optString("data").trim().takeIf { it.isNotEmpty() } ?: continue
            if (IPV4_REGEX.matches(data)) return data
        }
        return null
    }

    companion object {
        private const val PAYMASTER_HOST = "paymaster.ru"
        private const val DNS_TYPE_A = 1

        private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    }
}
