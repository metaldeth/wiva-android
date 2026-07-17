package com.wiva.android.data.remote.sbp

import com.wiva.android.domain.model.SBPStatus

data class QPayGenerateQrParseResult(
    val retval: Int,
    val retdesc: String,
    val qr: String?,
)

object PaymasterXmlParser {
    fun parseQPayGenerateQr(xml: String): QPayGenerateQrParseResult {
        val retval = extractIntTag(xml, "retval") ?: -1
        val retdesc = extractTagText(xml, "retdesc").orEmpty()
        val qr = extractTagText(xml, "qr")
        return QPayGenerateQrParseResult(retval = retval, retdesc = retdesc, qr = qr?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun parseQPayOutInvoiceState(xml: String): Int = extractIntTag(xml, "state") ?: 0

    fun parseQPayCancelRetval(xml: String): Int = extractIntTag(xml, "retval") ?: -1

    fun parseQPayStatusToSbpStatus(state: Int): SBPStatus {
        return when (state) {
            5 -> SBPStatus.Success
            6 -> SBPStatus.Failed("DENIED")
            else -> SBPStatus.Pending
        }
    }

    fun parsePaymentUrl(xml: String): String? {
        val start = xml.indexOf("<PaymentUrl>")
        val end = xml.indexOf("</PaymentUrl>")
        if (start < 0 || end < 0) return null
        return xml.substring(start + "<PaymentUrl>".length, end).trim()
    }

    fun parseOrderId(xml: String): String? {
        val start = xml.indexOf("<OrderId>")
        val end = xml.indexOf("</OrderId>")
        if (start < 0 || end < 0) return null
        return xml.substring(start + "<OrderId>".length, end).trim()
    }

    fun parseStatus(xml: String): SBPStatus {
        return when {
            xml.contains("<StatusCode>PS_PAID</StatusCode>") -> SBPStatus.Success
            xml.contains("<StatusCode>PS_REJECTED</StatusCode>") -> SBPStatus.Failed("Rejected")
            xml.contains("<StatusCode>PS_CANCELLED</StatusCode>") -> SBPStatus.Cancelled
            else -> SBPStatus.Pending
        }
    }

    private fun extractTagText(xml: String, localName: String): String? {
        val open = "<$localName"
        val start = xml.indexOf(open)
        if (start < 0) return null
        val tagEnd = xml.indexOf('>', start)
        if (tagEnd < 0) return null
        val close = xml.indexOf("</$localName>", tagEnd)
        if (close < 0) return null
        return xml.substring(tagEnd + 1, close).trim()
    }

    private fun extractIntTag(xml: String, localName: String): Int? {
        val text = extractTagText(xml, localName) ?: return null
        return text.toIntOrNull()
    }
}
