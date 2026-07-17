package com.wiva.android.data.payment.aqsi

import com.wiva.android.BuildConfig
import com.wiva.android.domain.model.AqsiPaymentResult
import timber.log.Timber
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.Charset
import java.util.Collections
import kotlin.math.min

private const val TAG_ARCUS2 = "Arcus2"

/**
 * Единая точка для политики байтового лога в [Arcus2Client] — совпадает с [BuildConfig.DEBUG].
 * Вынесено, чтобы юнит-тесты могли явно связать wire-лог с реальным флагом сборки.
 */
internal fun arcus2WireBytesLoggingEnabled(): Boolean = BuildConfig.DEBUG

/**
 * Байтовый «снимок» Arcus2-кадра — только при [debugEnabled]
 * (в приложении: [arcus2WireBytesLoggingEnabled] / [BuildConfig.DEBUG]).
 * Не логирует полный DATA/JPAY payload, только длину и первые байты заголовка кадра.
 */
internal fun logArcus2WireFrameHead(
    direction: String,
    frame: ByteArray,
    tag: String = TAG_ARCUS2,
    debugEnabled: Boolean,
) {
    if (!debugEnabled) return
    val n = min(frame.size, 24)
    val head = frame.copyOfRange(0, n).joinToString(" ") { b -> "%02x".format(b) }
    Timber.tag(tag).d("$direction arcus2 len=${frame.size} head=$head")
}

/** Контракт низкоуровневого TCP/JPAY-клиента (моки в тестах). */
interface Arcus2TerminalClient {
    fun testTcpChannel(host: String, port: Int, timeoutMs: Long): Result<Unit>

    fun initiatePurchase(host: String, port: Int, timeoutMs: Long, amountKopecks: Int): Result<AqsiPaymentResult>

    fun cancelPurchase(host: String, port: Int, timeoutMs: Long): Result<Unit>

 /**
 * Закрыть все TCP-сессии этого клиента, открытые внутри [runTerminal] (прервать блокирующий read/connect).
 * Вызывается при отмене корутины оплаты и в начале [cancelPurchase], чтобы операция могла закончиться раньше [Socket.soTimeout].
 */
    fun interruptCurrentTcpSession() {}
}

fun Long.toSocketTimeoutMs(): Int =
    when {
        this <= 0 -> 1
        this > Int.MAX_VALUE -> Int.MAX_VALUE
        else -> this.toInt()
    }

/**
 * TCP-клиент Arcus2 + JPAY BinLen для сценариев aQsi Pill / T7100.
 *
 * Байтовый лог — только при [arcus2WireBytesLoggingEnabled] (эквивалент [BuildConfig.DEBUG] в приложении),
 * тег [TAG_ARCUS2], без полного DATA (только заголовок кадра).
 */
class Arcus2Client(
    private val socketFactory: () -> Socket = { Socket() },
) : Arcus2TerminalClient {

    private val interruptibleSockets: MutableSet<Socket> =
        Collections.synchronizedSet(HashSet())

 /** Для каждого хостового сокета, открытого клиентом, закрывает соединение. */
    override fun interruptCurrentTcpSession() {
        val snapshot = synchronized(interruptibleSockets) { interruptibleSockets.toList() }
        snapshot.forEach { sock -> runCatching { sock.shutdownInput(); sock.shutdownOutput(); sock.close() } }
    }

    private val jpayCharset: Charset by lazy { Charset.forName("windows-1251") }

    private var blockToggle: Boolean = false

    private fun nextPcb(): Byte {
        val bit = if (blockToggle) 0x80 else 0x00
        blockToggle = !blockToggle
        return bit.toByte()
    }

    private fun resetProtocolState() {
        blockToggle = false
    }

    override fun testTcpChannel(host: String, port: Int, timeoutMs: Long): Result<Unit> =
        runTerminal(host, port, timeoutMs) { socket ->
            sendEchoProbe(socket)
            readEchoResponse(socket).getOrElse { err -> return@runTerminal Result.failure(err) }
            Result.success(Unit)
        }

    override fun initiatePurchase(host: String, port: Int, timeoutMs: Long, amountKopecks: Int): Result<AqsiPaymentResult> {
        if (amountKopecks < 0) {
            return Result.failure(AqsiTransportException("invalid_amount_kopecks"))
        }
        return runTerminal(host, port, timeoutMs) { socket ->
            sendEchoProbe(socket)
            readEchoResponse(socket).getOrElse { err -> return@runTerminal Result.failure(err) }

            sendJpayAscii(socket, "BEGINTR:")
            when (readJpayAck(socket).getOrElse { err -> return@runTerminal Result.failure(err) }) {
                JpayAckOutcome.DeclinedByEr ->
                    return@runTerminal Result.success(
                        AqsiPaymentResult.Declined(publicCode = JpayPaymentOutcomeParser.DECLINED_PUBLIC_CODE_ER),
                    )

                JpayAckOutcome.Ok -> Unit
            }

            sendJpayAscii(socket, buildPaymentOperationStart(amountKopecks))

            repeat(MAX_PAYMENT_DIALOG_STEPS) {
                val inner =
                    readInnerFromSocket(socket).getOrElse { err -> return@runTerminal Result.failure(err) }
                val text = JpayPaymentOutcomeParser.decodeInnerPayload(inner)
                val interpretation = JpayPaymentOutcomeParser.interpretPaymentLine(text)
                JpayPaymentOutcomeParser.toPaymentResult(interpretation)?.let { payResult ->
                    return@runTerminal payResult
                }
                when (interpretation) {
                    is JpayInterpretation.OtherCommand -> sendJpayAscii(socket, "OK")
                    else -> Unit
                }
            }
            Result.failure(AqsiTransportException("payment_dialog_exhausted"))
        }
    }

    override fun cancelPurchase(host: String, port: Int, timeoutMs: Long): Result<Unit> =
        runTerminal(host, port, timeoutMs) { socket ->
            sendEchoProbe(socket)
            readEchoResponse(socket).getOrElse { err -> return@runTerminal Result.failure(err) }
            sendJpayAscii(socket, "ENDTR:")
            val inner = readInnerFromSocket(socket).getOrElse { err -> return@runTerminal Result.failure(err) }
            val text = JpayPaymentOutcomeParser.decodeInnerPayload(inner)
            when {
                text.equals("OK", ignoreCase = true) || text.startsWith("OK:", ignoreCase = true) ->
                    Result.success(Unit)

                text.equals("ER", ignoreCase = true) ->
                    Result.failure(AqsiTransportException("cancel_er"))

                else -> Result.success(Unit)
            }
        }

    private inline fun <T> runTerminal(
        host: String,
        port: Int,
        timeoutMs: Long,
        block: (Socket) -> Result<T>,
    ): Result<T> {
        val socket = socketFactory()
        resetProtocolState()
        val toMs = timeoutMs.toSocketTimeoutMs()
        interruptibleSockets.add(socket)
        return try {
            socket.connect(InetSocketAddress(host, port), toMs)
            socket.soTimeout = toMs
            block(socket)
        } catch (e: SocketTimeoutException) {
            Result.failure(AqsiTransportException("timeout", e))
        } catch (e: java.io.IOException) {
            Result.failure(AqsiTransportException("io_error", e))
        } finally {
            interruptibleSockets.remove(socket)
            try {
                socket.close()
            } catch (_: Exception) {
 // ignore
            }
        }
    }

    private fun sendEchoProbe(socket: Socket) {
        val inner = JpayBinLen.wrap(ByteArray(0))
        sendArcusPayload(socket, inner)
    }

    private fun readEchoResponse(socket: Socket): Result<Unit> {
        val decoded = readArcusFrame(socket).getOrElse { err -> return Result.failure(err) }
        JpayBinLen.unwrap(decoded.payload).getOrElse { err -> return Result.failure(err) }
        return Result.success(Unit)
    }

    private fun sendJpayAscii(socket: Socket, ascii: String) {
        val inner = JpayBinLen.wrap(ascii.toByteArray(jpayCharset))
        sendArcusPayload(socket, inner)
    }

    private fun sendArcusPayload(socket: Socket, innerBinLen: ByteArray) {
        val frame = Arcus2FrameCodec.encode(innerBinLen, nextPcb())
        debugLogFrame("tx", frame)
        writeAll(socket.getOutputStream(), frame)
    }

    private fun readJpayAck(socket: Socket): Result<JpayAckOutcome> {
        val inner = readInnerFromSocket(socket).getOrElse { err -> return Result.failure(err) }
        val text = JpayPaymentOutcomeParser.decodeInnerPayload(inner)
        return when {
            text.equals("OK", ignoreCase = true) || text.startsWith("OK:", ignoreCase = true) ->
                Result.success(JpayAckOutcome.Ok)

            text.equals("ER", ignoreCase = true) ->
                Result.success(JpayAckOutcome.DeclinedByEr)

            else -> Result.failure(AqsiTransportException("unexpected_ack"))
        }
    }

 /** Исход фазы ACK (после `BEGINTR:`) до платёжного диалога. */
    private enum class JpayAckOutcome {
        Ok,
        DeclinedByEr,
    }

    private fun readInnerFromSocket(socket: Socket): Result<ByteArray> {
        val decoded = readArcusFrame(socket).getOrElse { err -> return Result.failure(err) }
        return JpayBinLen.unwrap(decoded.payload)
    }

    private fun readArcusFrame(socket: Socket): Result<Arcus2DecodedFrame> {
        val raw = readRawArcusFrame(socket.getInputStream()).getOrElse { err -> return Result.failure(err) }
        debugLogFrame("rx", raw)
 // [Arcus2FrameCodec.decode] возвращает только [Arcus2ProtocolException] в ветке failure.
        return Arcus2FrameCodec.decode(raw).fold(
            onSuccess = { Result.success(it) },
            onFailure = { e ->
                Result.failure(
                    e as? Arcus2ProtocolException
                        ?: Arcus2ProtocolException(e.message ?: "decode"),
                )
            },
        )
    }

    private fun readRawArcusFrame(ins: InputStream): Result<ByteArray> {
        return try {
            val stx = ByteArray(1)
            readFully(ins, stx)
            if (stx[0] != Arcus2FrameCodec.STX) {
                return Result.failure(Arcus2ProtocolException("bad STX"))
            }
            val hdr = ByteArray(3)
            readFully(ins, hdr)
            val dataLen = (hdr[1].toInt() and 0xFF) or ((hdr[2].toInt() and 0xFF) shl 8)
            val tail = ByteArray(dataLen + 1)
            readFully(ins, tail)
            Result.success(stx + hdr + tail)
        } catch (e: SocketTimeoutException) {
            Result.failure(AqsiTransportException("timeout", e))
        } catch (e: java.io.IOException) {
            Result.failure(AqsiTransportException("io_error", e))
        } catch (e: EOFException) {
            Result.failure(AqsiTransportException("eof", e))
        }
    }

    private fun writeAll(out: OutputStream, data: ByteArray) {
        out.write(data)
        out.flush()
    }

    private fun readFully(ins: InputStream, buf: ByteArray) {
        var r = 0
        while (r < buf.size) {
            val n = ins.read(buf, r, buf.size - r)
            if (n < 0) throw EOFException()
            r += n
        }
    }

    private fun debugLogFrame(direction: String, frame: ByteArray) {
        logArcus2WireFrameHead(direction, frame, TAG_ARCUS2, arcus2WireBytesLoggingEnabled())
    }

    companion object {
        private const val MAX_PAYMENT_DIALOG_STEPS = 96
    }
}

/**
 * Формат команды начала операции «Оплата» (класс 1, код 1), сумма как в KB.
 *
 * Допустимо только [amountKopecks] >= 0. Публичные входы ([Arcus2Client.initiatePurchase], репозиторий)
 * обязаны отсечь отрицательные суммы до вызова; иначе [IllegalArgumentException] — защита от внутреннего переиспользования без guard.
 */
internal fun buildPaymentOperationStart(amountKopecks: Int): String {
    require(amountKopecks >= 0) {
        "amountKopecks must be >= 0 (validate in initiatePurchase / repository before buildPaymentOperationStart)"
    }
    val rub = amountKopecks / 100
    val kop = amountKopecks % 100
    val amount = "$rub.${kop.toString().padStart(2, '0')}"
    val sep = 0x1b.toChar()
    return "1${sep}1${sep}643${sep}$amount"
}
