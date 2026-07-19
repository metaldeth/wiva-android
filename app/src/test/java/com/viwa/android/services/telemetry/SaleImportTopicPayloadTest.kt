package com.viwa.android.services.telemetry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleImportTopicPayloadTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun saleImportItem_holdsCallerFields() {
        val item =
            SaleImportItem(
                drinkId = 20,
                volume = 200,
                price = 1.5,
                payMethod = "CARD",
                totalChargedRub = 1.5,
            )
        assertEquals(20, item.drinkId)
        assertEquals(200, item.volume)
        assertEquals(1.5, item.price, 0.0)
        assertEquals("CARD", item.payMethod)
        assertEquals(1.5, item.totalChargedRub!!, 0.0)
    }

    @Test
    fun machineInfo_parsing_roundTripIds() {
        val raw =
            """
            {"type":"machineInfo","body":{"id":"1","organizationId":"2","modelId":"3","serialNumber":"ABC"}}
            """.trimIndent()
        val root = json.parseToJsonElement(raw).jsonObject
        val body = root["body"]!!.jsonObject
        assertEquals("1", body["id"]?.jsonPrimitive?.content)
        assertEquals("2", body["organizationId"]?.jsonPrimitive?.content)
        assertEquals("3", body["modelId"]?.jsonPrimitive?.content)
        assertEquals("ABC", body["serialNumber"]?.jsonPrimitive?.content)
    }

    @Test
    fun authCodeResponse_parsing() {
        val raw = """{"type":"authCodeRequestExport","success":true,"message":"ok"}"""
        val root = json.parseToJsonElement(raw).jsonObject
        assertEquals(true, root["success"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("ok", root["message"]?.jsonPrimitive?.content)
    }
}
