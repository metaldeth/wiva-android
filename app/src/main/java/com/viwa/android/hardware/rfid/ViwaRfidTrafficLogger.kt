package com.viwa.android.hardware.rfid

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RfidTrafficEntry(
    val id: Int,
    val time: String,
    val direction: String,
    val port: String,
    val hex: String,
    val ascii: String,
    val len: Int,
    val note: String = "",
)

@Singleton
class ViwaRfidTrafficLogger
@Inject
constructor() {
    private val maxEntries = 300
    private val counter = AtomicInteger(0)
    private val fmt = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    private val _entries = MutableStateFlow<List<RfidTrafficEntry>>(emptyList())
    val entries: StateFlow<List<RfidTrafficEntry>> = _entries.asStateFlow()

    fun logRx(port: String, bytes: ByteArray, note: String = "") = append("RX", port, bytes, note)

    fun logTx(port: String, bytes: ByteArray, note: String = "") = append("TX", port, bytes, note)

    fun logEvent(port: String, message: String) {
        push(
            RfidTrafficEntry(
                id = counter.getAndIncrement(),
                time = currentTime(),
                direction = "—",
                port = port.substringAfterLast('/'),
                hex = "",
                ascii = "",
                len = 0,
                note = message,
            ),
        )
    }

    private fun append(direction: String, port: String, bytes: ByteArray, note: String) {
        push(
            RfidTrafficEntry(
                id = counter.getAndIncrement(),
                time = currentTime(),
                direction = direction,
                port = port.substringAfterLast('/'),
                hex = bytes.joinToString(" ") { "%02X".format(it) },
                ascii = bytes.toAsciiPreview(),
                len = bytes.size,
                note = note,
            ),
        )
    }

    private fun push(entry: RfidTrafficEntry) {
        synchronized(this) {
            val next = _entries.value + entry
            _entries.value = if (next.size > maxEntries) next.takeLast(maxEntries) else next
        }
    }

    fun clear() {
        synchronized(this) { _entries.value = emptyList() }
    }

    private fun currentTime(): String = checkNotNull(fmt.get()).format(Date())
}

private fun ByteArray.toAsciiPreview(): String =
    buildString {
        this@toAsciiPreview.forEach { byte ->
            val code = byte.toInt() and 0xff
            append(
                when (code) {
                    9 -> "\\t"
                    10 -> "\\n"
                    13 -> "\\r"
                    in 32..126 -> code.toChar().toString()
                    else -> "."
                },
            )
        }
    }
