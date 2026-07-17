package com.wiva.android.services.telemetry

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
    fun saleImportTopic_matchesWivaShape() {
        val msg =
            SaleImportOutboundMessage(
                clientId = "SN-DEMO-1",
                body =
                    listOf(
                        SaleImportItemJson(
                            dateSale = "2026-04-01T12:00:00.000Z",
                            orgId = 10,
                            machineId = 20,
                            promocodeId = null,
                            name = "Demo",
                            volume = 200,
                            discountId = null,
                            price = 1.5,
                            totalPrice = 1.5,
                            unit = "ML",
                            writeOffs =
                                listOf(
                                    SaleImportWriteOffJson(1, 2, 200),
                                ),
                            payments =
                                listOf(
                                    SaleImportPaymentJson(1.5, "CARD"),
                                ),
                        ),
                    ),
            )
        val s = json.encodeToString(SaleImportOutboundMessage.serializer(), msg)
        val decoded = json.decodeFromString(SaleImportOutboundMessage.serializer(), s)
        assertEquals("saleImportTopic", decoded.type)
        assertEquals("SN-DEMO-1", decoded.clientId)
        val item = decoded.body.single()
        assertEquals("ML", item.unit)
        assertEquals(200, item.volume)
        assertEquals(null, item.promocodeId)
        val w = item.writeOffs.single()
        assertEquals("INGREDIENT", w.cellType)
        assertEquals("G", w.unit)
        assertEquals("CARD", item.payments.single().method)
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
