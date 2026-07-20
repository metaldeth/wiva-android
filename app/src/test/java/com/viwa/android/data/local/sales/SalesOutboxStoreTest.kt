package com.viwa.android.data.local.sales

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalesOutboxStoreTest {
    private lateinit var configRepository: FakeConfigRepository
    private lateinit var store: SalesOutboxStore

    @Before
    fun setUp() {
        configRepository = FakeConfigRepository()
        store = SalesOutboxStore(configRepository)
    }

    @Test
    fun `enqueue persists pending sale as json list`() = runTest {
        // given
        val sale = sampleSale(saleId = "sale-1")

        // when
        store.enqueue(sale)

        // then
        val raw = configRepository.getJson(JsonStoreKeys.PENDING_SALES)
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString<List<PendingSale>>(raw!!)
        assertEquals(1, decoded.size)
        assertEquals("sale-1", decoded.first().saleId)
        assertEquals(PendingSaleStatus.PENDING, decoded.first().status)
    }

    @Test
    fun `markSent removes sale from store`() = runTest {
        // given
        store.enqueue(sampleSale(saleId = "sale-1"))

        // when
        store.markSent("sale-1")

        // then
        assertNull(configRepository.getJson(JsonStoreKeys.PENDING_SALES))
        assertTrue(store.listPending().isEmpty())
    }

    @Test
    fun `bumpAttempt applies retry backoff schedule`() = runTest {
        // given
        store.enqueue(sampleSale(saleId = "sale-1"))
        val now = 1_000_000L

        // when / then
        store.bumpAttempt("sale-1", nowMillis = now)
        var pending = store.listPending(nowMillis = now)
        assertTrue(pending.isEmpty())
        pending = store.listPending(nowMillis = now + 1_000)
        assertEquals(1, pending.size)
        assertEquals(1, pending.first().attempts)

        store.bumpAttempt("sale-1", nowMillis = now + 1_000)
        pending = store.listPending(nowMillis = now + 2_999)
        assertTrue(pending.isEmpty())
        pending = store.listPending(nowMillis = now + 3_000)
        assertEquals(2, pending.first().attempts)
    }

    @Test
    fun `retryDelayMillis caps at thirty seconds`() {
        assertEquals(1_000L, store.retryDelayMillis(1))
        assertEquals(2_000L, store.retryDelayMillis(2))
        assertEquals(5_000L, store.retryDelayMillis(3))
        assertEquals(10_000L, store.retryDelayMillis(4))
        assertEquals(30_000L, store.retryDelayMillis(5))
        assertEquals(30_000L, store.retryDelayMillis(99))
    }

    private fun sampleSale(saleId: String): PendingSale =
        PendingSale(
            saleId = saleId,
            soldAt = "2026-07-20T12:00:00.000Z",
            drinkId = 20,
            volumeMl = 200,
            amountRub = 150.0,
            payMethod = "CARD",
        )

    private class FakeConfigRepository : ConfigRepository {
        private val store = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = store[key]

        override suspend fun set(key: String, value: String) {
            store[key] = value
        }

        override suspend fun delete(key: String) {
            store.remove(key)
        }

        override suspend fun getJson(key: String): String? = store[key]

        override suspend fun setJson(key: String, json: String) {
            store[key] = json
        }
    }
}
