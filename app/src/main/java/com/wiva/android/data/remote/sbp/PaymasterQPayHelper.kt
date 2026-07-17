package com.wiva.android.data.remote.sbp

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object PaymasterQPayHelper {
    fun calculateSign(spotId: String, orderId: String, paymentType: String, reqn: String, key: String): String {
        val signString = spotId + orderId + paymentType + reqn + key
        return sha256Hex(signString)
    }

    fun calculateSignGetOutInvoices(posid: String, reqn: String, key: String): String {
        val signString = posid + reqn + key
        return sha256Hex(signString)
    }

    fun calculateSignCancelOrder(posid: String, orderId: String, reqn: String, key: String): String {
        val signString = posid + orderId + reqn + key
        return sha256Hex(signString)
    }

    fun formatDateExp(timeoutSeconds: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.SECOND, timeoutSeconds)
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val s = cal.get(Calendar.SECOND).toString().padStart(2, '0')
        return "$y$m$d $h:$min:$s"
    }

    fun createGenerateQrXml(
        spotId: String,
        orderId: String,
        amountRubles: String,
        reqn: String,
        sign: String,
        timeoutSeconds: Int,
    ): String {
        val dateExp = formatDateExp(timeoutSeconds)
        return (
            "<w3s.request>\r\n" +
                "  <reqn>${xmlEscape(reqn)}</reqn>\r\n" +
                "  <sign>${xmlEscape(sign)}</sign>\r\n" +
                "  <generate_qr_code>\r\n" +
                "    <posid>${xmlEscape(spotId)}</posid>\r\n" +
                "    <orderid>${xmlEscape(orderId)}</orderid>\r\n" +
                "    <amount>${xmlEscape(amountRubles)}</amount>\r\n" +
                "    <ptype>sbp</ptype>\r\n" +
                "    <dateexp>${xmlEscape(dateExp)}</dateexp>\r\n" +
                "  </generate_qr_code>\r\n" +
                "</w3s.request>"
            )
    }

    fun createGetOutInvoicesXml(
        reqn: String,
        sign: String,
        posid: String,
        orderId: String,
    ): String {
        return (
            "<w3s.request>\r\n" +
                "  <reqn>${xmlEscape(reqn)}</reqn>\r\n" +
                "  <sign>${xmlEscape(sign)}</sign>\r\n" +
                "  <posid>${xmlEscape(posid)}</posid>\r\n" +
                "  <outinvoices>\r\n" +
                "    <order><orderid>${xmlEscape(orderId)}</orderid></order>\r\n" +
                "  </outinvoices>\r\n" +
                "</w3s.request>"
            )
    }

    fun createCancelOrderXml(
        reqn: String,
        sign: String,
        posid: String,
        orderId: String,
    ): String {
        return (
            "<w3s.request>\r\n" +
                "  <reqn>${xmlEscape(reqn)}</reqn>\r\n" +
                "  <sign>${xmlEscape(sign)}</sign>\r\n" +
                "  <cancelorder>\r\n" +
                "    <posid>${xmlEscape(posid)}</posid>\r\n" +
                "    <orderid>${xmlEscape(orderId)}</orderid>\r\n" +
                "  </cancelorder>\r\n" +
                "</w3s.request>"
            )
    }

 /**
 * Уникальный **числовой** идентификатор для [orderid] / [reqn] в Paymaster QPay.
 *
 * Ограничение API: строка только из цифр, **не длиннее 10 символов** (см. `PaymasterQPayHelperTest`
 * `PaymasterQPayHelperTest`, тот же контракт, что в Paymaster). Более длинные значения дают
 * `retval != 0` с текстом вроде «Invalid request format».
 *
 * Диапазон [1_000_000_000, 9_999_999_999] — всегда ровно 10 цифр, коллизии «Duplicate order»
 * редки; при ответе про дубликат [DrinkListViewModel] делает повторные попытки.
 */
    fun generateReqn(): String {
        val v = Random.nextLong(1_000_000_000L, 10_000_000_000L)
        return v.toString()
    }

    fun amountRublesString(amountKopecks: Int): String =
        String.format(Locale.US, "%.2f", amountKopecks / 100.0)

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(StandardCharsets.US_ASCII))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    private fun xmlEscape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
