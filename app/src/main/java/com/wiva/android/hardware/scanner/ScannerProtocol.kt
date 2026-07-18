package com.wiva.android.hardware.scanner

import com.wiva.android.data.remote.telemetry.mvp.RegistrationKeyUtils
import com.wiva.android.domain.model.BarcodeEvent

object ScannerProtocol {
    fun parse(rawBytes: ByteArray): String? {
        val str = rawBytes.toString(Charsets.UTF_8).trim()
        return if (str.isNotEmpty()) str else null
    }

 /**
 * Лояльность в Android повторяет пользовательское поведение :
 * только код формата `CLIENT_<clientId>` считается картой подписки.
 */
    fun extractSubscriptionClientId(code: String): String? {
        val trimmed = code.trim()
        if (!trimmed.startsWith(CLIENT_PREFIX)) return null

        val clientId = trimmed.removePrefix(CLIENT_PREFIX).trim()
        return clientId.ifEmpty { null }
    }

    fun classifyBarcode(code: String): BarcodeEvent {
        val trimmed = code.trim()
        val subscriptionClientId = extractSubscriptionClientId(trimmed)
        return when {
            subscriptionClientId != null -> BarcodeEvent.ClientLoyaltyCard(subscriptionClientId)
            trimmed.startsWith("{") -> BarcodeEvent.TelemetryRegistrationQr(trimmed)
            trimmed.startsWith("EMP:") -> BarcodeEvent.EmployeeKey(trimmed)
            trimmed.startsWith("KEY-") -> BarcodeEvent.EmployeeKey(trimmed)
            trimmed.startsWith("REG-") || trimmed.startsWith("REG:") -> {
                BarcodeEvent.RegistrationKey(RegistrationKeyUtils.normalize(trimmed))
            }
            trimmed.all { it.isDigit() || it == '-' } && trimmed.length in 8..20 ->
                BarcodeEvent.ProductBarcode(trimmed)
            else -> BarcodeEvent.UnknownBarcode(trimmed)
        }
    }

    private const val CLIENT_PREFIX = "CLIENT_"
}
