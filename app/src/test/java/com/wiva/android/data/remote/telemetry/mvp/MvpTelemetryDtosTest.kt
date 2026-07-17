package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpTelemetryDtosTest {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    @Test
    fun `reserve request serializes installationId field name`() {
        // given
        val dto = ReserveSerialRequestDto(installationId = "550e8400-e29b-41d4-a716-446655440000")
        // when
        val raw = json.encodeToString(dto)
        // then
        assertTrue(raw.contains("\"installationId\""))
        assertTrue(raw.contains("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun `reserve response uses exact field names`() {
        // given
        val raw =
            """
            {"serialNumber":"WIVA-000042","reservationToken":"rtok","expiresAt":"2026-07-17T12:00:00Z"}
            """.trimIndent()
        // when
        val dto = json.decodeFromString(ReserveSerialResponseDto.serializer(), raw)
        // then
        assertEquals("WIVA-000042", dto.serialNumber)
        assertEquals("rtok", dto.reservationToken)
        assertEquals("2026-07-17T12:00:00Z", dto.expiresAt)
    }

    @Test
    fun `enroll request uses contract field names`() {
        // given
        val dto =
            EnrollRequestDto(
                installationId = "inst-1",
                serialNumber = "WIVA-000001",
                reservationToken = "tok",
                credential = "mch_abc",
                rebind = true,
                device = EnrollDeviceDto("M", "X", "7.1"),
                app = EnrollAppDto("1.0", 1),
            )
        // when
        val raw = json.encodeToString(dto)
        // then
        listOf(
            "installationId",
            "serialNumber",
            "reservationToken",
            "credential",
            "rebind",
            "device",
            "manufacturer",
            "model",
            "androidVersion",
            "app",
            "versionName",
            "versionCode",
        ).forEach { field -> assertTrue("missing $field in $raw", raw.contains("\"$field\"")) }
    }

    @Test
    fun `ws envelope serializes type messageId sentAt payload`() {
        // given
        val dto =
            MvpWsEnvelopeDto(
                type = "heartbeat",
                messageId = "mid-1",
                sentAt = "2026-07-17T12:00:00.000Z",
                payload = null,
            )
        // when
        val raw = json.encodeToString(dto)
        // then
        assertTrue(raw.contains("\"type\":\"heartbeat\""))
        assertTrue(raw.contains("\"messageId\":\"mid-1\""))
        assertTrue(raw.contains("\"sentAt\":"))
    }
}
