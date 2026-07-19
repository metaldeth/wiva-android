package com.viwa.android.data.remote.telemetry.mvp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpWsEnvelopeFactoryTest {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    @Test
    fun `envelope embeds payload JsonObject without serialize roundtrip`() {
        // given
        val payload =
            buildJsonObject {
                put("state", JsonPrimitive("idle"))
                put("appVersionCode", JsonPrimitive(42))
            }
        // when
        val envelope =
            MvpWsEnvelopeFactory.create(
                type = "heartbeat",
                messageId = "mid-test",
                sentAt = "2026-07-17T12:00:00.000Z",
                payload = payload,
            )
        val raw = json.encodeToString(MvpWsEnvelopeDto.serializer(), envelope)
        // then
        assertTrue(raw.contains("\"type\":\"heartbeat\""))
        assertTrue(raw.contains("\"state\":\"idle\""))
        assertTrue(raw.contains("\"appVersionCode\":42"))
        assertFalse(raw.contains("\"payload\":\"{"))
    }
}
