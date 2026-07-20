package com.viwa.android.data.payment.aqsi

import com.viwa.android.data.payment.aqsi.network.AqsiPillNetworkRouter
import java.net.Socket
import java.util.ArrayDeque

internal const val AQSI_PROXY_HOST = "192.168.137.1"
internal const val AQSI_PROXY_PORT = 1080
internal const val ADB_REVERSE_PROXY_HOST = "127.0.0.1"

private const val MIN_SOCKET_READ_TIMEOUT_MS = 100
private const val MAX_SOCKET_READ_TIMEOUT_MS = 300_000
private const val DEFAULT_SOCKET_READ_TIMEOUT_MS = 500

internal fun arcusIoctlReadTimeoutMs(readUnits: Int): Int {
    val ms = readUnits * 10
    return ms.coerceIn(MIN_SOCKET_READ_TIMEOUT_MS, MAX_SOCKET_READ_TIMEOUT_MS)
}

/**
 * Arcus2 bank tunnel: tries adb-reversed SOCKS on 127.0.0.1:1080, else emulates SOCKS5
 * and opens direct sockets through Android network (gnirehtet VPN).
 */
internal class ArcusSocket(
    private val networkRouter: AqsiPillNetworkRouter? = null,
) {
    private val queued = ArrayDeque<Byte>()
    private var socket: Socket? = null
    private var socksState = 0
    @Volatile
    private var closed = false
    private var readTimeoutUnits: Int? = null
    private var writeTimeoutUnits: Int? = null

    fun configureIo(readUnits: Int, writeUnits: Int) {
        readTimeoutUnits = readUnits
        writeTimeoutUnits = writeUnits
        applySocketReadTimeout()
    }

    fun appliedReadTimeoutMs(): Int =
        readTimeoutUnits?.let(::arcusIoctlReadTimeoutMs) ?: DEFAULT_SOCKET_READ_TIMEOUT_MS

    private fun applySocketReadTimeout() {
        socket?.soTimeout = appliedReadTimeoutMs()
    }

    fun connect(host: String, port: Int) {
        if (closed) return
        if (host == AQSI_PROXY_HOST && port == AQSI_PROXY_PORT) {
            if (connectToOnDeviceSocks()) return
            if (!connectToAdbReverseProxy()) {
                socksState = 1
            }
        } else {
            openSocket(host, port, 10_000)
        }
    }

    fun write(data: ByteArray): Boolean {
        if (closed) return false
        if (socksState > 0) return handleSocks(data)
        return runCatching {
            socket?.getOutputStream()?.apply {
                write(data)
                flush()
            } != null
        }.getOrDefault(false)
    }

    fun read(maxLen: Int): ByteArray {
        if (closed) return ByteArray(0)
        if (queued.isNotEmpty()) {
            val count = minOf(maxLen, queued.size)
            return ByteArray(count) { queued.removeFirst() }
        }
        val activeSocket = socket ?: return ByteArray(0)
        return runCatching {
            val buffer = ByteArray(maxLen.coerceIn(1, 4096))
            val read = activeSocket.getInputStream().read(buffer)
            if (read > 0) buffer.copyOf(read) else ByteArray(0)
        }.getOrDefault(ByteArray(0))
    }

    fun close() {
        closed = true
        runCatching { socket?.close() }
        socket = null
        queued.clear()
        socksState = 0
    }

    val socksEmulationActive: Boolean
        get() = socksState > 0

    private fun openSocket(
        host: String,
        port: Int,
        timeoutMs: Int,
    ) {
        val nextSocket = Socket()
        val router = networkRouter
        if (router != null && AqsiPillNetworkRouter.routesViaPillSubnet(host, port)) {
            router.connectToPill(nextSocket, host, port, timeoutMs)
        } else if (router != null) {
            router.connectForInternet(nextSocket, host, port, timeoutMs)
        } else {
            nextSocket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
        }
        nextSocket.soTimeout = appliedReadTimeoutMs()
        socket = nextSocket
    }

    private fun connectToOnDeviceSocks(): Boolean =
        runCatching {
            val router = networkRouter ?: return false
            socket =
                Socket().apply {
                    router.connectToPill(this, AQSI_PROXY_HOST, AQSI_PROXY_PORT, 3_000)
                    soTimeout = appliedReadTimeoutMs()
                }
            true
        }.getOrElse {
            runCatching { socket?.close() }
            socket = null
            false
        }

    private fun connectToAdbReverseProxy(): Boolean =
        runCatching {
            socket = Socket().apply {
                connect(java.net.InetSocketAddress(ADB_REVERSE_PROXY_HOST, AQSI_PROXY_PORT), 1_000)
                soTimeout = appliedReadTimeoutMs()
            }
            true
        }.getOrElse {
            runCatching { socket?.close() }
            socket = null
            false
        }

    private fun handleSocks(data: ByteArray): Boolean {
        return try {
            when (socksState) {
                1 -> {
                    enqueue(byteArrayOf(0x05, 0x00))
                    socksState = 2
                }
                2 -> {
                    val target = parseSocksConnect(data) ?: return false
                    openSocket(target.first, target.second, 20_000)
                    enqueue(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    socksState = 3
                }
                else -> {
                    socket?.getOutputStream()?.apply {
                        write(data)
                        flush()
                    }
                }
            }
            true
        } catch (_: Exception) {
            enqueue(byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            false
        }
    }

    private fun parseSocksConnect(data: ByteArray): Pair<String, Int>? {
        if (data.size < 7 || data[0] != 0x05.toByte() || data[1] != 0x01.toByte()) return null
        var offset = 4
        val host =
            when (data[3].toInt() and 0xFF) {
                0x01 ->
                    data.copyOfRange(offset, offset + 4).joinToString(".") { (it.toInt() and 0xFF).toString() }
                        .also { offset += 4 }
                0x03 -> {
                    val len = data[offset].toInt() and 0xFF
                    offset += 1
                    data.copyOfRange(offset, offset + len).toString(Charsets.US_ASCII).also { offset += len }
                }
                else -> return null
            }
        if (data.size < offset + 2) return null
        val port = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        return host to port
    }

    private fun enqueue(data: ByteArray) {
        data.forEach { queued.add(it) }
    }
}
