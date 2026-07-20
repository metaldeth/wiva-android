package com.viwa.android.data.payment.aqsi

import com.viwa.android.data.payment.aqsi.UsbPaymentResult
import com.viwa.android.data.payment.aqsi.network.AqsiPillNetworkConstants
import com.viwa.android.data.payment.aqsi.network.AqsiPillNetworkRouter
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.regex.Pattern

internal object JpayTcpClient {
    private const val HOST = AqsiPillNetworkConstants.PILL_HOST
    private const val PORT = AqsiPillNetworkConstants.JPAY_PORT
    private const val DEFAULT_PASSWORD = "12345678"
    private const val CONNECT_TIMEOUT_MS = 3_000
    private const val TRANSACTION_TIMEOUT_MS = 180_000

    private val tokenPattern = Pattern.compile("<token>([^<]+)</token>")
    private val statusPattern = Pattern.compile("<status>([^<]+)</status>")

    fun tryPurchase(
        amountKopecks: Int,
        networkRouter: AqsiPillNetworkRouter,
        password: String = DEFAULT_PASSWORD,
        log: (String) -> Unit,
        isCancelled: () -> Boolean = { false },
        onTransactionSocket: (Socket?) -> Unit = {},
    ): UsbPaymentResult? {
        if (amountKopecks <= 0) {
            return UsbPaymentResult.Failure("JPAY_INVALID_AMOUNT", "Некорректная сумма")
        }
        return try {
            val loginXml =
                """
                <login>
                    <password>$password</password>
                </login>
                """.trimIndent()
            val loginResponse =
                callJpay(networkRouter, loginXml).also { response ->
                    log("JPAY_LOGIN $response")
                }
            val loginStatus = extractStatus(loginResponse)
            if (!loginStatus.equals("ok", ignoreCase = true)) {
                return UsbPaymentResult.Failure(
                    "JPAY_LOGIN_FAILED",
                    "JPAY login: $loginStatus",
                )
            }
            val token =
                extractToken(loginResponse)
                    ?: return UsbPaymentResult.Failure("JPAY_NO_TOKEN", "JPAY: нет token после login")

            log("JPAY_TX waiting for card")

            val amountField = amountKopecks.coerceAtLeast(0).toString().padStart(12, '0')
            val txXml =
                """
                <transaction>
                    <token>$token</token>
                    <type>purchase</type>
                    <currency>643</currency>
                    <amount>$amountField</amount>
                </transaction>
                """.trimIndent()
            val txResponse =
                callJpayTransaction(
                    networkRouter,
                    txXml,
                    isCancelled = isCancelled,
                    onTransactionSocket = onTransactionSocket,
                ) { response ->
                    log("JPAY_TX $response")
                }
            mapTransactionResult(txResponse, amountKopecks)
        } catch (_: JpayCancelledException) {
            log("JPAY_TCP_CANCELLED")
            UsbPaymentResult.Cancelled
        } catch (_: SocketTimeoutException) {
            log("JPAY_TCP_FAIL SocketTimeoutException: card wait timeout")
            UsbPaymentResult.Timeout
        } catch (error: Exception) {
            log("JPAY_TCP_FAIL ${error.javaClass.simpleName}: ${error.message.orEmpty()}")
            UsbPaymentResult.Failure("JPAY_UNREACHABLE", error.message ?: "JPAY недоступен")
        }
    }

    private fun callJpay(networkRouter: AqsiPillNetworkRouter, xml: String): String =
        Socket().use { socket ->
            networkRouter.connectToPill(socket, HOST, PORT, CONNECT_TIMEOUT_MS)
            socket.soTimeout = TRANSACTION_TIMEOUT_MS
            writeXml(socket, xml)
            readXml(socket)
        }

    /** Transaction socket stays open; Pill may send `<keepalive />` until card is read. */
    private fun callJpayTransaction(
        networkRouter: AqsiPillNetworkRouter,
        xml: String,
        isCancelled: () -> Boolean,
        onTransactionSocket: (Socket?) -> Unit,
        onResponse: (String) -> Unit,
    ): String {
        val socket = Socket()
        onTransactionSocket(socket)
        return try {
            networkRouter.connectToPill(socket, HOST, PORT, CONNECT_TIMEOUT_MS)
            socket.soTimeout = TRANSACTION_TIMEOUT_MS
            writeXml(socket, xml)
            var response: String
            do {
                if (isCancelled()) {
                    throw JpayCancelledException()
                }
                response = readXml(socket)
                onResponse(response)
            } while (!isFinalTransactionFrame(response))
            response
        } finally {
            onTransactionSocket(null)
            runCatching { socket.close() }
        }
    }

    private class JpayCancelledException : Exception()

    private fun isFinalTransactionFrame(xml: String): Boolean {
        val trimmed = xml.trim()
        if (trimmed.isEmpty()) return false
        val status = extractStatus(trimmed).lowercase(Locale.US)
        if (status.isBlank()) {
            return !isKeepalive(trimmed)
        }
        return status == "ok" ||
            status == "success" ||
            status == "approved" ||
            status == "succeeded" ||
            status.contains("cancel") ||
            status.contains("timeout") ||
            status.contains("declin") ||
            status.contains("fail") ||
            status.contains("error")
    }

    private fun isKeepalive(xml: String): Boolean {
        val trimmed = xml.trim()
        if (!trimmed.contains("keepalive", ignoreCase = true)) return false
        val status = extractStatus(trimmed).lowercase(Locale.US)
        if (status.isBlank()) return true
        return status == "waiting" || status == "keepalive"
    }

    private fun writeXml(socket: Socket, xml: String) {
        val payload = xml.toByteArray(Charsets.UTF_8)
        val header =
            byteArrayOf(
                ((payload.size shr 24) and 0xFF).toByte(),
                ((payload.size shr 16) and 0xFF).toByte(),
                ((payload.size shr 8) and 0xFF).toByte(),
                (payload.size and 0xFF).toByte(),
            )
        socket.getOutputStream().write(header)
        socket.getOutputStream().write(payload)
        socket.getOutputStream().flush()
    }

    private fun readXml(socket: Socket): String {
        val lengthBytes = ByteArray(4)
        var read = 0
        while (read < 4) {
            val chunk = socket.getInputStream().read(lengthBytes, read, 4 - read)
            if (chunk < 0) error("JPAY: short header")
            read += chunk
        }
        val length =
            ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                (lengthBytes[3].toInt() and 0xFF)
        val body = ByteArray(length)
        read = 0
        while (read < length) {
            val chunk = socket.getInputStream().read(body, read, length - read)
            if (chunk < 0) error("JPAY: short body")
            read += chunk
        }
        return body.toString(Charsets.UTF_8)
    }

    private fun extractToken(xml: String): String? {
        val matcher = tokenPattern.matcher(xml)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun extractStatus(xml: String): String =
        statusPattern.matcher(xml).let { matcher ->
            if (matcher.find()) matcher.group(1)?.trim().orEmpty() else ""
        }

    private fun mapTransactionResult(xml: String, amountKopecks: Int): UsbPaymentResult {
        val trimmed = xml.trim()
        if (isKeepalive(trimmed)) {
            return UsbPaymentResult.Timeout
        }
        val status = extractStatus(trimmed).lowercase(Locale.US)
        return when {
            status == "ok" || status == "success" || status == "approved" || status == "succeeded" ->
                UsbPaymentResult.Success("JPAY-${System.currentTimeMillis()}", amountKopecks)
            status.contains("cancel") -> UsbPaymentResult.Cancelled
            status.contains("timeout") -> UsbPaymentResult.Timeout
            status.isBlank() ->
                UsbPaymentResult.Failure("JPAY_UNKNOWN", trimmed.take(200))
            else -> UsbPaymentResult.Failure("JPAY_DECLINED", status)
        }
    }
}

