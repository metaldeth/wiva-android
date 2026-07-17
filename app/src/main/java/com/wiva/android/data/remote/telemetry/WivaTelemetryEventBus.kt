package com.wiva.android.data.remote.telemetry

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Одно входящее WS-сообщение: сырой JSON + поле `type` (если есть). */
data class WivaWsIncomingFrame(
    val type: String?,
    val rawJson: String,
)

@Singleton
class WivaTelemetryEventBus
@Inject
constructor() {
    private val _incoming =
        MutableSharedFlow<WivaWsIncomingFrame>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val incoming: SharedFlow<WivaWsIncomingFrame> = _incoming.asSharedFlow()

    suspend fun emit(frame: WivaWsIncomingFrame) = _incoming.emit(frame)
}
