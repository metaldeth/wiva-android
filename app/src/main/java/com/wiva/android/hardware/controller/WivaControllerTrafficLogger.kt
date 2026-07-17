package com.wiva.android.hardware.controller

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ControllerTrafficDirection {
    TX,
    RX,
}

data class ControllerTrafficEntry(
    val id: Int,
    val timestampMs: Long,
    val direction: ControllerTrafficDirection,
    val commandHex: String,
    val commandName: String,
    val payloadBytes: List<Int>,
)

@Singleton
class WivaControllerTrafficLogger
@Inject
constructor() {
    private val maxEntries = 500
    private val counter = AtomicInteger(0)

    private val _entries = MutableStateFlow<List<ControllerTrafficEntry>>(emptyList())
    val entries: StateFlow<List<ControllerTrafficEntry>> = _entries.asStateFlow()

    fun log(entry: CommandLogEntry) {
        val line =
            ControllerTrafficEntry(
                id = counter.getAndIncrement(),
                timestampMs = System.currentTimeMillis(),
                direction =
                    when (entry.direction) {
                        CommandLogEntry.CommandLogDirection.TX -> ControllerTrafficDirection.TX
                        CommandLogEntry.CommandLogDirection.RX -> ControllerTrafficDirection.RX
                    },
                commandHex = entry.commandHex,
                commandName = entry.commandName,
                payloadBytes = entry.payload,
            )
        synchronized(this) {
            val next = _entries.value + line
            _entries.value = if (next.size > maxEntries) next.takeLast(maxEntries) else next
        }
    }

    fun clear() {
        synchronized(this) { _entries.value = emptyList() }
    }
}
