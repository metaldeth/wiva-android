package com.wiva.android.data.remote.telemetry.mvp

import java.util.UUID
import kotlinx.serialization.json.JsonObject

internal object MvpWsEnvelopeFactory {
    fun create(
        type: String,
        payload: JsonObject,
        messageId: String = UUID.randomUUID().toString(),
        sentAt: String = TelemetryIsoTimestamps.nowUtc(),
    ): MvpWsEnvelopeDto =
        MvpWsEnvelopeDto(
            type = type,
            messageId = messageId,
            sentAt = sentAt,
            payload = payload,
        )
}
