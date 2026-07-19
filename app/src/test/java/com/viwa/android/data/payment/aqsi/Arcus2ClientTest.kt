package com.viwa.android.data.payment.aqsi

import com.viwa.android.domain.model.AqsiPaymentResult
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

/** Точечные проверки [Arcus2Client] после ревью task-03: сумма, прерывание TCP. */
class Arcus2ClientTest {

    private class RecordingTree : Timber.Tree() {
        val entries = mutableListOf<Pair<String, String>>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (tag != null) entries += tag to message
        }
    }

    @Test
    fun initiatePurchase_negativeAmount_doesNotOpenSocket() {
        var socketsCreated = 0
        val client =
            Arcus2Client(socketFactory = {
                socketsCreated++
                Socket()
            })
        val r = client.initiatePurchase("127.0.0.1", 9, 1000L, -5)
        assertTrue(r.isFailure)
        val ex = r.exceptionOrNull() as AqsiTransportException
        assertEquals("invalid_amount_kopecks", ex.message)
        assertEquals(0, socketsCreated)
    }

 /**
 * Task-07: wire-лог по тому же TCP-пути, что и при оплате (tx/rx кадры), с флагом [arcus2WireBytesLoggingEnabled].
 */
    @Test(timeout = 20_000)
    fun testTcpChannel_wireHeadLogsFollowArcus2WireBytesLoggingEnabled() {
        val srv = ServerSocket(0)
        val port = srv.localPort
        val serverThread =
            Thread {
                try {
                    srv.accept().use { sock ->
                        val ins = sock.getInputStream()
                        val out = sock.getOutputStream()
                        readArcusFrameBytes(ins)
                        val echoInner = JpayBinLen.wrap(ByteArray(0))
                        writeAll(out, Arcus2FrameCodec.encode(echoInner, 0))
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { srv.close() }
                }
            }
        serverThread.isDaemon = true
        serverThread.start()

        Thread.sleep(150)

        val tree = RecordingTree()
        Timber.plant(tree)
        try {
            val client = Arcus2Client()
            val r = client.testTcpChannel("127.0.0.1", port, 15_000L)
            assertTrue(r.isSuccess)
            val arcusLogs = tree.entries.filter { it.first == "Arcus2" }
            if (arcus2WireBytesLoggingEnabled()) {
                assertTrue("expected wire head logs on tcp test path", arcusLogs.isNotEmpty())
                assertTrue(arcusLogs.any { it.second.contains("arcus2 len=") })
            } else {
                assertTrue("wire logs must be off when DEBUG is false", arcusLogs.isEmpty())
            }
        } finally {
            Timber.uproot(tree)
            runCatching { srv.close() }
            serverThread.interrupt()
            serverThread.join(3000)
        }
    }

    @Test(timeout = 25_000)
    fun initiatePurchase_interruptTcp_closesSocketAndFailsFast() {
        val srv = ServerSocket(0)
        val port = srv.localPort
        val serverThread =
            Thread {
                try {
                    srv.accept().use { sock ->
                        val ins = sock.getInputStream()
                        val chunk = ByteArray(512)
                        while (true) {
                            val n = ins.read(chunk)
                            if (n < 0) break
                        }
                    }
                } catch (_: Exception) {
 // клиент мог оборвать соединение
                } finally {
                    runCatching { srv.close() }
                }
            }
        serverThread.isDaemon = true
        serverThread.start()

        Thread.sleep(150)

        val exec = Executors.newSingleThreadExecutor()
        try {
            val arc = Arcus2Client()
            val fut =
                exec.submit(
                    Callable {
                        arc.initiatePurchase("127.0.0.1", port, 120_000L, 100)
                    },
                )

            Thread.sleep(800)

            arc.interruptCurrentTcpSession()

            val r = fut.get(20, TimeUnit.SECONDS)
            assertTrue(r.isFailure)
        } finally {
            exec.shutdownNow()
            exec.awaitTermination(5, TimeUnit.SECONDS)
            runCatching { srv.close() }
            serverThread.interrupt()
            serverThread.join(3000)
        }
    }

 /**
 * Протокольный `ER` сразу после `BEGINTR:` → [AqsiPaymentResult.Declined], не [AqsiPaymentResult.Error].
 */
    @Test(timeout = 20_000)
    fun initiatePurchase_beginTrAckEr_returnsDeclined() {
        val srv = ServerSocket(0)
        val port = srv.localPort
        val win1251 = Charset.forName("windows-1251")
        val serverThread =
            Thread {
                try {
                    srv.accept().use { sock ->
                        val ins = sock.getInputStream()
                        val out = sock.getOutputStream()
                        readArcusFrameBytes(ins)
                        val echoInner = JpayBinLen.wrap(ByteArray(0))
                        /*
 * PCB в ответах фикстуры: 0x00. На линии host↔терминал KB задаёт чередование блоков (бит в PCB);
 * исходящие кадры клиента чередуют PCB по своему состоянию протокола. Декодер при приёме проверяет
 * STX/длину/CRC, но не валидирует строгое чередование PCB с железом — для юнит-среды достаточно
 * согласованного CRC; живой терминал может отдавать другой PCB при том же payload.
 */
                        writeAll(out, Arcus2FrameCodec.encode(echoInner, 0))
                        readArcusFrameBytes(ins)
                        val erInner = JpayBinLen.wrap("ER".toByteArray(win1251))
                        writeAll(out, Arcus2FrameCodec.encode(erInner, 0))
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { srv.close() }
                }
            }
        serverThread.isDaemon = true
        serverThread.start()

        Thread.sleep(150)

        val client = Arcus2Client()
        val r = client.initiatePurchase("127.0.0.1", port, 15_000L, 100_00)
        assertTrue(r.isSuccess)
        val pay = r.getOrNull()
        assertTrue(pay is AqsiPaymentResult.Declined)
        assertTrue(pay !is AqsiPaymentResult.Error)
        assertEquals(
            JpayPaymentOutcomeParser.DECLINED_PUBLIC_CODE_ER,
            (pay as AqsiPaymentResult.Declined).publicCode,
        )
        serverThread.join(5_000)
    }

    private fun readFully(ins: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = ins.read(buf, off, buf.size - off)
            if (n < 0) throw EOFException()
            off += n
        }
    }

    private fun readArcusFrameBytes(ins: InputStream): ByteArray {
        val stx = ByteArray(1)
        readFully(ins, stx)
        assertEquals(Arcus2FrameCodec.STX, stx[0])
        val hdr = ByteArray(3)
        readFully(ins, hdr)
        val dataLen = (hdr[1].toInt() and 0xFF) or ((hdr[2].toInt() and 0xFF) shl 8)
        val tail = ByteArray(dataLen + 1)
        readFully(ins, tail)
        return stx + hdr + tail
    }

    private fun writeAll(out: OutputStream, data: ByteArray) {
        out.write(data)
        out.flush()
    }
}
