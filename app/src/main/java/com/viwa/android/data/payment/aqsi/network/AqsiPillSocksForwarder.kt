package com.viwa.android.data.payment.aqsi.network

import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SOCKS5 on [AqsiPillNetworkConstants.PILL_GATEWAY_HOST]:1080 so Pill can reach the bank
 * when `misc;proxy_enabled=true` (same role as antinat on the provisioning PC).
 */
@Singleton
class AqsiPillSocksForwarder
@Inject
constructor(
    private val networkRouter: AqsiPillNetworkRouter,
) {
    private val running = AtomicBoolean(false)

    fun ensureStarted() {
        if (running.get()) return
        synchronized(this) {
            if (running.get()) return
            Thread(
                {
                    try {
                        ServerSocket().use { server ->
                            server.reuseAddress = true
                            server.bind(
                                InetSocketAddress(
                                    AqsiPillNetworkConstants.PILL_GATEWAY_HOST,
                                    AqsiPillNetworkConstants.SOCKS_PORT,
                                ),
                            )
                            running.set(true)
                            Log.i(TAG, "SOCKS listening on $HOST:$PORT")
                            while (running.get()) {
                                val client = server.accept()
                                Thread({ relayClient(client) }, "aqsi-socks-client").start()
                            }
                        }
                    } catch (error: IOException) {
                        running.set(false)
                        Log.w(TAG, "SOCKS server stopped: ${error.message}")
                    }
                },
                "aqsi-socks",
            ).apply {
                isDaemon = true
                start()
            }
        }
    }

    fun stop() {
        running.set(false)
    }

    private fun relayClient(client: Socket) {
        client.soTimeout = 30_000
        try {
        client.use { upstream ->
            if (!performSocksHandshake(upstream)) return
            val target = readSocksConnect(upstream) ?: return
            Log.i(TAG, "SOCKS connect ${target.first}:${target.second}")
                Socket().use { remote ->
                    networkRouter.connectForInternet(
                        remote,
                        target.first,
                        target.second,
                        20_000,
                    )
                    remote.soTimeout = 30_000
                    writeSocksConnectOk(upstream)
                    pipe(upstream, remote)
                }
            }
        } catch (error: IOException) {
            Log.d(TAG, "SOCKS client closed: ${error.message}")
        } finally {
            runCatching { client.close() }
        }
    }

    private fun performSocksHandshake(socket: Socket): Boolean {
        val greeting = ByteArray(2)
        if (!readFully(socket, greeting) || greeting[0] != 0x05.toByte()) return false
        val methods = ByteArray(greeting[1].toInt() and 0xFF)
        if (!readFully(socket, methods)) return false
        socket.getOutputStream().write(byteArrayOf(0x05, 0x00))
        socket.getOutputStream().flush()
        return true
    }

    private fun readSocksConnect(socket: Socket): Pair<String, Int>? {
        val header = ByteArray(4)
        if (!readFully(socket, header)) return null
        if (header[0] != 0x05.toByte() || header[1] != 0x01.toByte()) return null
        return when (header[3].toInt() and 0xFF) {
            0x01 -> {
                val addr = ByteArray(4)
                if (!readFully(socket, addr)) return null
                val host = addr.joinToString(".") { (it.toInt() and 0xFF).toString() }
                readPort(socket)?.let { host to it }
            }
            0x03 -> {
                val lenByte = ByteArray(1)
                if (!readFully(socket, lenByte)) return null
                val len = lenByte[0].toInt() and 0xFF
                val hostBytes = ByteArray(len)
                if (!readFully(socket, hostBytes)) return null
                val host = hostBytes.toString(Charsets.US_ASCII)
                readPort(socket)?.let { host to it }
            }
            else -> null
        }
    }

    private fun readPort(socket: Socket): Int? {
        val portBytes = ByteArray(2)
        if (!readFully(socket, portBytes)) return null
        return ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)
    }

    private fun writeSocksConnectOk(socket: Socket) {
        socket.getOutputStream().write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        socket.getOutputStream().flush()
    }

    private fun pipe(left: Socket, right: Socket) {
        val threadA =
            Thread(
                {
                    runCatching {
                        left.getInputStream().copyTo(right.getOutputStream())
                    }
                    runCatching { right.shutdownOutput() }
                },
                "aqsi-socks-pipe-a",
            )
        val threadB =
            Thread(
                {
                    runCatching {
                        right.getInputStream().copyTo(left.getOutputStream())
                    }
                    runCatching { left.shutdownOutput() }
                },
                "aqsi-socks-pipe-b",
            )
        threadA.start()
        threadB.start()
        threadA.join(180_000)
        threadB.join(1_000)
    }

    private fun readFully(socket: Socket, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = socket.getInputStream().read(buffer, offset, buffer.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }

    companion object {
        private const val TAG = "AQSI_NET"
        private val HOST = AqsiPillNetworkConstants.PILL_GATEWAY_HOST
        private val PORT = AqsiPillNetworkConstants.SOCKS_PORT
    }
}
