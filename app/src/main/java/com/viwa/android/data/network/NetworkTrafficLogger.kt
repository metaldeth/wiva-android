package com.viwa.android.data.network

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkTrafficChannel { WS, HTTP }

enum class NetworkTrafficDirection { OUT, IN, SYSTEM }

data class NetworkTrafficEntry(
    val id: Int,
    val timestampMs: Long,
    val channel: NetworkTrafficChannel,
    val direction: NetworkTrafficDirection,
    val summary: String,
    val payload: String,
)

@Singleton
class NetworkTrafficLogger
@Inject
constructor() {
    private val maxEntries = 1000
    private val counter = AtomicInteger(0)

    private val _entries = MutableStateFlow<List<NetworkTrafficEntry>>(emptyList())
    val entries: StateFlow<List<NetworkTrafficEntry>> = _entries.asStateFlow()

    fun log(
        channel: NetworkTrafficChannel,
        direction: NetworkTrafficDirection,
        summary: String,
        payload: String = summary,
    ) {
        val line =
            NetworkTrafficEntry(
                id = counter.getAndIncrement(),
                timestampMs = System.currentTimeMillis(),
                channel = channel,
                direction = direction,
                summary = summary,
                payload = payload,
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
