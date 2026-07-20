package com.viwa.android.ui.screens.service.devices

internal class ViwaPortScanLogBuffer(
    private val maxSize: Int = 500,
    private val emitEvery: Int = 8,
) {
    private val entries = ArrayDeque<ViwaPortScanLogEntry>(maxSize.coerceAtLeast(1))
    private var pendingSinceEmit = 0

    fun append(entry: ViwaPortScanLogEntry): EmitDecision {
        if (entries.size >= maxSize) {
            entries.removeFirst()
        }
        entries.addLast(entry)
        pendingSinceEmit++
        return if (pendingSinceEmit >= emitEvery) {
            pendingSinceEmit = 0
            EmitDecision.Emit(snapshot())
        } else {
            EmitDecision.Defer
        }
    }

    fun flush(): List<ViwaPortScanLogEntry> {
        pendingSinceEmit = 0
        return snapshot()
    }

    fun snapshot(): List<ViwaPortScanLogEntry> = entries.toList()

    sealed interface EmitDecision {
        data object Defer : EmitDecision

        data class Emit(val entries: List<ViwaPortScanLogEntry>) : EmitDecision
    }
}
