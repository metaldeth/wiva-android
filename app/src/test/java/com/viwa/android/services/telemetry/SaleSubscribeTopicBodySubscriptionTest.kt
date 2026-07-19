package com.viwa.android.services.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Контракт тела [saleSubscribeTopic] для покупки подписки (карта / СБП) — без ViewModel.
 */
class SaleSubscribeTopicBodySubscriptionTest {

    @Test
    fun cardPurchase_hasExpectedFields() {
        val body =
            SaleSubscribeTopicBody(
                machineClientId = "sn-1",
                userUuid = "user-1",
                machineId = 42,
                requestUuid = "req-1",
                operationType = SaleSubscribeOperationType.SALE,
                price = 299.0,
                monthCount = 1,
                payMethod = "CARD",
                subscribeLevelUuid = "level-uuid",
            )
        assertEquals(SaleSubscribeOperationType.SALE, body.operationType)
        assertEquals(1, body.monthCount)
        assertEquals("CARD", body.payMethod)
        assertEquals("level-uuid", body.subscribeLevelUuid)
        assertEquals(299.0, body.price ?: 0.0, 0.001)
    }

    @Test
    fun sbpPurchase_hasExpectedFields() {
        val body =
            SaleSubscribeTopicBody(
                machineClientId = "sn-1",
                userUuid = "user-1",
                machineId = 42,
                requestUuid = "req-2",
                operationType = SaleSubscribeOperationType.SALE,
                price = 150.0,
                monthCount = 1,
                payMethod = "SBP",
                subscribeLevelUuid = "level-uuid",
            )
        assertEquals("SBP", body.payMethod)
        assertEquals(1, body.monthCount)
    }

    @Test
    fun bodySerializesClientIdKey_forBackendContract() {
        val json =
            Json.encodeToString(
                SaleSubscribeTopicBody(
                    machineClientId = "sn-1",
                    userUuid = "user-1",
                    machineId = 42,
                    requestUuid = "req-3",
                    operationType = SaleSubscribeOperationType.SALE,
                    price = 150.0,
                    monthCount = 1,
                    payMethod = "SBP",
                    subscribeLevelUuid = "level-uuid",
                ),
            )

        assertTrue(json.contains("\"clientId\":\"user-1\""))
        assertFalse(json.contains("\"userUuid\""))
    }
}
