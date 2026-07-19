package com.viwa.android.hardware.controller

import android_serialport_api.SerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Транспорт для нативных UART-портов платы (/dev/ttyS*, /dev/ttyACM*).
 * Использует JNI через android-serialport-api — открывает файл устройства с нужным бодрейтом
 * через termios без root (если плата выдаёт права на эти ноды).
 */
class TtySerialControllerTransport(
    private val ioScope: CoroutineScope,
) : ControllerSerialTransport {

    private var serialPort: SerialPort? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var readJob: Job? = null
    private var rxListener: ((ByteArray) -> Unit)? = null

    override var isOpen: Boolean = false
        private set

    override suspend fun open(settings: ControllerPortSettings): Boolean =
        withContext(Dispatchers.IO) {
            close()
            if (!java.io.File(settings.devicePath).exists()) {
                Timber.tag(TAG).w("TTY: нет файла %s", settings.devicePath)
                return@withContext false
            }
            try {
 // SerialPort(File, baudRate, stopBits=1, dataBits=8, parity=0-none, flowCon=0-none, flags=0)
                val port = SerialPort(java.io.File(settings.devicePath), settings.baudRate, 1, 8, 0, 0, 0)
                serialPort = port
                input = port.getInputStream()
                output = port.getOutputStream()
                isOpen = true
                Timber.tag(TAG).i("TTY open %s %d baud", settings.devicePath, settings.baudRate)
                startReadLoop()
                true
            } catch (e: SecurityException) {
                Timber.tag(TAG).w("TTY: нет доступа %s — %s", settings.devicePath, e.message)
                false
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "TTY open failed %s", settings.devicePath)
                false
            }
        }

    private fun startReadLoop() {
        readJob = ioScope.launch(Dispatchers.IO) {
            val buf = ByteArray(4096)
            val stream = input ?: return@launch
            while (isActive && isOpen) {
                try {
                    val n = stream.read(buf)
                    if (n > 0) rxListener?.invoke(buf.copyOf(n))
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    override fun close() {
        readJob?.cancel()
        readJob = null
        runCatching { serialPort?.close() }
        serialPort = null
        input = null
        output = null
        if (isOpen) Timber.tag(TAG).i("TTY close")
        isOpen = false
    }

    override suspend fun write(bytes: ByteArray) {
        val out = output ?: error("TTY not open")
        withContext(Dispatchers.IO) {
            out.write(bytes)
            out.flush()
        }
    }

    override fun setOnBytesReceived(listener: ((ByteArray) -> Unit)?) {
        rxListener = listener
    }

    companion object {
        private const val TAG = "ViwaController"
    }
}
