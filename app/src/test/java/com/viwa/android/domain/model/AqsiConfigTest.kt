package com.viwa.android.domain.model

import com.viwa.android.data.local.db.JsonStoreKeys
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AqsiConfigTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Test
    fun defaultsConstructor() {
        val c = AqsiConfig()
        assertEquals("", c.host)
        assertEquals(AQSI_DEFAULT_TCP_PORT, c.port)
        assertEquals(AQSI_DEFAULT_TIMEOUT_MS, c.timeoutMs)
    }

    @Test
    fun decodeEmptyJsonObject_usesDefaults() {
        val c = json.decodeFromString<AqsiConfig>("{}")
        assertEquals("", c.host)
        assertEquals(AQSI_DEFAULT_TCP_PORT, c.port)
        assertEquals(AQSI_DEFAULT_TIMEOUT_MS, c.timeoutMs)
    }

    @Test
    fun decodePartial_preservesUnsetDefaults() {
        val c = json.decodeFromString<AqsiConfig>("""{"host":"10.0.0.12"}""")
        assertEquals("10.0.0.12", c.host)
        assertEquals(AQSI_DEFAULT_TCP_PORT, c.port)
        assertEquals(AQSI_DEFAULT_TIMEOUT_MS, c.timeoutMs)
    }

    @Test
    fun copyAndEquality_dataClassStable() {
        val a = AqsiConfig(host = "h", port = 2000, timeoutMs = 3000L)
        val b = a.copy(port = AQSI_DEFAULT_TCP_PORT)
        assertEquals(a.host, b.host)
        assertEquals(AQSI_DEFAULT_TCP_PORT, b.port)
        assertEquals(a.timeoutMs, b.timeoutMs)
        assertEquals(a, a.copy())
        assertNotEquals(AqsiConfig(host = "a"), AqsiConfig(host = "b"))
    }

    @Test
    fun encodeRoundTrip() {
        val orig = AqsiConfig(host = "192.168.1.1", port = 16107, timeoutMs = 5000L)
        val text = json.encodeToString(AqsiConfig.serializer(), orig)
        val back = json.decodeFromString<AqsiConfig>(text)
        assertEquals(orig, back)
    }

    @Test
    fun jsonStoreKey_literalMatchesArchitecture() {
        assertEquals("aqsiSettings", JsonStoreKeys.AQSI_SETTINGS)
    }
}
