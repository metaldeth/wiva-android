package com.viwa.android.hardware.controller

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Сырой лог байт serial-порта (до и после протокола).
 * TX — кадр целиком (со стартом/стопом), RX — каждый пришедший chunk как есть.
 * Позволяет отлаживать связь без аппаратного анализатора.
 */
@Singleton
class ViwaControllerRawLogger
@Inject
constructor() {
    data class RawEntry(
        val id: Int,
        val time: String,
        val direction: String,
        val port: String,
        val hex: String,
        val len: Int,
        val note: String = "",
    )

    private val maxEntries = 300
    private val counter = AtomicInteger(0)
    private val fmt = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    private val _entries = MutableStateFlow<List<RawEntry>>(emptyList())
    val entries: StateFlow<List<RawEntry>> = _entries.asStateFlow()

    fun logTx(port: String, bytes: ByteArray) = append("TX", port, bytes)

    fun logRx(port: String, bytes: ByteArray, note: String = "") = append("RX", port, bytes, note)

    fun logEvent(port: String, message: String) {
        val entry = RawEntry(
            id = counter.getAndIncrement(),
            time = fmt.get().format(Date()),
            direction = "—",
            port = port.substringAfterLast('/'),
            hex = "",
            len = 0,
            note = message,
        )
        push(entry)
    }

    private fun append(dir: String, port: String, bytes: ByteArray, note: String = "") {
        val entry = RawEntry(
            id = counter.getAndIncrement(),
            time = fmt.get().format(Date()),
            direction = dir,
            port = port.substringAfterLast('/'),
            hex = bytes.joinToString(" ") { "%02X".format(it) },
            len = bytes.size,
            note = note,
        )
        push(entry)
    }

    private fun push(entry: RawEntry) {
        synchronized(this) {
            val next = _entries.value + entry
            _entries.value = if (next.size > maxEntries) next.takeLast(maxEntries) else next
        }
    }

    fun clear() {
        synchronized(this) { _entries.value = emptyList() }
    }
}
