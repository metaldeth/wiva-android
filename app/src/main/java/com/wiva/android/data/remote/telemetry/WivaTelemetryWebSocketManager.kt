package com.wiva.android.data.remote.telemetry

import com.wiva.android.data.network.NetworkTrafficChannel
import com.wiva.android.data.network.NetworkTrafficDirection
import com.wiva.android.data.network.NetworkTrafficLogger
import com.wiva.android.data.network.redactNetworkPayload
import com.wiva.android.di.AppIoScope
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Телеметрия WebSocket: переподключение (backoff 5 с…10 мин).
 * Транспорт — [WebSocketClient] (Java-WebSocket), а не OkHttp: у OkHttp нет колбэков на RFC6455
 * PING/PONG, поэтому полный лог WS (вкладка «Логи сети») был бы неполным.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WivaTelemetryWebSocketManager(
    private val eventBus: WivaTelemetryEventBus,
    @AppIoScope private val appScope: CoroutineScope,
    private val networkTrafficLogger: NetworkTrafficLogger,
) {
    private fun logWs(direction: NetworkTrafficDirection, summary: String, payload: String = summary) {
        networkTrafficLogger.log(
            channel = NetworkTrafficChannel.WS,
            direction = direction,
            summary = summary,
            payload = payload,
        )
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var activeClient: TelemetryJavaWsClient? = null
    private var connectJob: Job? = null
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private companion object {
        const val MIN_RECONNECT_MS = 5_000L
        const val MAX_RECONNECT_MS = 600_000L
        const val MAX_BACKOFF_SHIFT = 17
 /** Как прежний OkHttp [okhttp3.OkHttpClient.Builder.pingInterval] для телеметрии. */
        const val CLIENT_PING_INTERVAL_MS = 30_000L
    }

    private fun reconnectDelayAfterAttempt(attemptNumber: Int): Long {
        val exp = attemptNumber.coerceIn(1, MAX_BACKOFF_SHIFT)
        return (MIN_RECONNECT_MS * (1L shl exp)).coerceAtMost(MAX_RECONNECT_MS)
    }

    private fun redactSecretsForLog(text: String): String {
        return redactNetworkPayload(text)
    }

    private fun extractMessageTypeSummary(payload: String): String {
        val type =
            runCatching {
                json.parseToJsonElement(payload).jsonObject["type"]?.jsonPrimitive?.content
            }.getOrNull()
        return if (type.isNullOrBlank()) "WS message" else "WS type=$type"
    }

    private fun formatControlPayload(f: Framedata): String {
        val buf = f.payloadData
        val n = buf.remaining()
        if (n == 0) return "пустое тело"
        val dup = buf.duplicate()
        val take = minOf(n, 16)
        val arr = ByteArray(take)
        dup.get(arr)
        val hex = arr.joinToString("") { b -> "%02x".format(b) }
        return "$n байт, hex=$hex${if (n > take) "…" else ""}"
    }

    private fun hexPreviewBinary(buf: ByteBuffer): String {
        val dup = buf.duplicate()
        val n = dup.remaining()
        if (n == 0) return "0 байт"
        val take = minOf(n, 32)
        val arr = ByteArray(take)
        dup.get(arr)
        val hex = arr.joinToString("") { b -> "%02x".format(b) }
        return "$n байт, hex=$hex${if (n > take) "…" else ""}"
    }

    fun connect(wsUrl: String, tokenProvider: suspend () -> String?) {
        connectJob?.cancel()
        connectJob =
            appScope.launch {
                var reconnectAttempts = 0
                var nextDelayMs = MIN_RECONNECT_MS

                while (isActive) {
                    _connectionState.value = ConnectionState.Connecting
                    logWs(NetworkTrafficDirection.SYSTEM, "Подключение: $wsUrl")

                    val token = tokenProvider()
                    Timber.d("WivaTelemetry WS: connecting, token=${if (token != null) "present" else "null"}")
                    if (token == null) Timber.w("WivaTelemetry WS: без Bearer — возможен 401")

                    try {
                        val uri =
                            try {
                                URI.create(wsUrl)
                            } catch (e: Exception) {
                                reconnectAttempts++
                                nextDelayMs = reconnectDelayAfterAttempt(reconnectAttempts)
                                _connectionState.value = ConnectionState.Disconnected(nextDelayMs)
                                Timber.e(e, "WivaTelemetry WS: неверный URL")
                                delay(nextDelayMs)
                                continue
                            }

                        val connectionEnded = AtomicBoolean(false)
                        fun resumeWhenSocketEnds(cont: CancellableContinuation<Unit>) {
                            if (!connectionEnded.compareAndSet(false, true)) return
                            if (cont.isActive) cont.resume(Unit) {}
                        }

                        val sessionLossHandled = AtomicBoolean(false)
                        fun notifySessionLost(
                            cont: CancellableContinuation<Unit>,
                            logLine: String,
                            throwable: Throwable?,
                        ) {
                            if (!sessionLossHandled.compareAndSet(false, true)) {
                                resumeWhenSocketEnds(cont)
                                return
                            }
                            reconnectAttempts++
                            nextDelayMs = reconnectDelayAfterAttempt(reconnectAttempts)
                            logWs(NetworkTrafficDirection.SYSTEM, logLine)
                            _connectionState.value = ConnectionState.Disconnected(nextDelayMs)
                            throwable?.let { Timber.w(it, "WivaTelemetry WS session lost, retry in ${nextDelayMs}ms") }
                                ?: Timber.w("WivaTelemetry WS session lost, retry in ${nextDelayMs}ms")
                            Timber.i(
                                "WivaTelemetry WS: переподключение через ${nextDelayMs / 1000}с " +
                                    "(попытка $reconnectAttempts)",
                            )
                            resumeWhenSocketEnds(cont)
                        }

                        suspendCancellableCoroutine<Unit> { cont ->
                            cont.invokeOnCancellation {
                                activeClient?.close()
                            }

                            val client =
                                TelemetryJavaWsClient(
                                    uri = uri,
                                    bearer = token?.let { "Bearer $it" },
                                    onSessionLost = { line, t -> notifySessionLost(cont, line, t) },
                                    onConnected = {
                                        reconnectAttempts = 0
                                        nextDelayMs = MIN_RECONNECT_MS
                                        _connectionState.value = ConnectionState.Connected
                                        logWs(
                                            NetworkTrafficDirection.SYSTEM,
                                            "WebSocket: открыт HTTP ${it.httpStatus} ${it.httpStatusMessage}",
                                        )
                                        Timber.i("WivaTelemetry WS: connected")
                                    },
                                    onTextMessage = { text ->
                                        logWs(
                                            NetworkTrafficDirection.IN,
                                            summary = extractMessageTypeSummary(text),
                                            payload = redactSecretsForLog(text),
                                        )
                                        parseAndEmit(text)
                                    },
                                    onBinaryMessage = { buf ->
                                        logWs(
                                            NetworkTrafficDirection.IN,
                                            "[WS binary] ${hexPreviewBinary(buf)}",
                                        )
                                    },
                                    onClosingLog = { code, reason, remote ->
                                        logWs(
                                            NetworkTrafficDirection.SYSTEM,
                                            "WebSocket: закрывается code=$code reason=$reason remote=$remote",
                                        )
                                    },
                                    logControlIn = { line -> logWs(NetworkTrafficDirection.IN, line) },
                                    logControlOut = { line -> logWs(NetworkTrafficDirection.OUT, line) },
                                )
                            activeClient = client
                            client.connect()
                        }
                        activeClient = null
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        reconnectAttempts++
                        nextDelayMs = reconnectDelayAfterAttempt(reconnectAttempts)
                        _connectionState.value = ConnectionState.Disconnected(nextDelayMs)
                        Timber.e(e, "WivaTelemetry WS: исключение до сокета, retry in ${nextDelayMs}ms")
                    }

                    Timber.d("WivaTelemetry WS: пауза перед повтором ${nextDelayMs}ms")
                    delay(nextDelayMs)
                }
            }
    }

    fun disconnect() {
        logWs(NetworkTrafficDirection.SYSTEM, "Отключение по запросу")
        connectJob?.cancel()
        activeClient?.close()
        activeClient = null
        _connectionState.value = ConnectionState.Disconnected()
    }

    suspend fun sendRawJson(payload: String): Result<Unit> =
        runCatching {
            val c = activeClient
            if (c == null || c.readyState != ReadyState.OPEN) error("WebSocket not connected")
            c.send(payload)
            logWs(
                direction = NetworkTrafficDirection.OUT,
                summary = extractMessageTypeSummary(payload),
                payload = redactSecretsForLog(payload),
            )
            val t =
                runCatching {
                    json.parseToJsonElement(payload).jsonObject["type"]?.jsonPrimitive?.content
                }.getOrNull()
            Timber.d("WivaTelemetry WS: sent type=${t ?: "?"}")
        }

    private fun parseAndEmit(text: String) {
        appScope.launch {
            runCatching {
                val obj = json.parseToJsonElement(text).jsonObject
                val type = obj["type"]?.jsonPrimitive?.content
                eventBus.emit(WivaWsIncomingFrame(type, text))
            }.onFailure {
                Timber.w(it, "WivaTelemetry WS: parse failed: $text")
            }
        }
    }

 /**
 * Один экземпляр на попытку подключения ([WebSocketClient] нельзя переиспользовать после close).
 */
    private inner class TelemetryJavaWsClient(
        uri: URI,
        bearer: String?,
        private val onSessionLost: (String, Throwable?) -> Unit,
        private val onConnected: (ServerHandshake) -> Unit,
        private val onTextMessage: (String) -> Unit,
        private val onBinaryMessage: (ByteBuffer) -> Unit,
        private val onClosingLog: (Int, String, Boolean) -> Unit,
        private val logControlIn: (String) -> Unit,
        private val logControlOut: (String) -> Unit,
    ) : WebSocketClient(uri) {
        private var lastError: Throwable? = null
        private var clientPingJob: Job? = null

        init {
            setConnectionLostTimeout(0)
            if (bearer != null) addHeader("Authorization", bearer)
        }

        override fun onOpen(handshake: ServerHandshake) {
            lastError = null
            onConnected(handshake)
            clientPingJob?.cancel()
            clientPingJob =
                this@WivaTelemetryWebSocketManager.appScope.launch {
                    while (isActive) {
                        delay(CLIENT_PING_INTERVAL_MS)
                        if (readyState != ReadyState.OPEN) break
                        logControlOut("[WS RFC6455] PING → сервер (keep-alive)")
                        runCatching { sendPing() }
                            .onFailure { e -> Timber.w(e, "WivaTelemetry WS: sendPing") }
                    }
                }
        }

        override fun onWebsocketPing(conn: WebSocket, f: Framedata) {
            logControlIn("[WS RFC6455] PING ← сервер, ${formatControlPayload(f)}")
            super.onWebsocketPing(conn, f)
            logControlOut("[WS RFC6455] PONG → сервер (ответ на PING)")
        }

        override fun onWebsocketPong(conn: WebSocket, f: Framedata) {
            logControlIn("[WS RFC6455] PONG ← сервер, ${formatControlPayload(f)}")
        }

        override fun onMessage(message: String) = onTextMessage(message)

        override fun onMessage(bytes: ByteBuffer) = onBinaryMessage(bytes)

        override fun onClosing(code: Int, reason: String, remote: Boolean) {
            onClosingLog(code, reason, remote)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            clientPingJob?.cancel()
            clientPingJob = null
            val err = lastError
            lastError = null
            val line =
                if (err != null) {
                    "WebSocket: закрыт code=$code reason=$reason (${err.message ?: err.javaClass.simpleName})"
                } else {
                    "WebSocket: закрыт code=$code reason=$reason"
                }
            onSessionLost(line, err)
        }

        override fun onError(ex: Exception) {
            lastError = ex
            logWs(
                NetworkTrafficDirection.SYSTEM,
                "Ошибка: ${ex.message ?: ex.javaClass.simpleName}",
            )
            Timber.w(ex, "WivaTelemetry WS")
        }
    }
}
