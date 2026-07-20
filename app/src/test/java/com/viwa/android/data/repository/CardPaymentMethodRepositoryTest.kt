package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.model.CardPaymentMockMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CardPaymentMethodRepositoryTest {

    private fun fakeConfig(): ConfigRepository =
        object : ConfigRepository {
            private val store = mutableMapOf<String, String>()

            override suspend fun get(key: String): String? = store[key]

            override suspend fun set(key: String, value: String) {
                store[key] = value
            }

            override suspend fun delete(key: String) {
                store.remove(key)
            }

            override suspend fun getJson(key: String): String? = store[key]

            override suspend fun setJson(key: String, jsonStr: String) {
                store[key] = jsonStr
            }
        }

    private fun repo(raw: String? = null): CardPaymentMethodRepositoryImpl {
        val cfg = fakeConfig()
        if (raw != null) {
            runBlocking { cfg.set(JsonStoreKeys.CARD_PAYMENT_METHOD, raw) }
        }
        return CardPaymentMethodRepositoryImpl(cfg)
    }

    @Test
    fun getSelected_defaultsToAqsi() =
        runBlocking {
            assertEquals(CardPaymentMethod.Aqsi, repo().getSelected())
        }

    @Test
    fun setSelected_persistsAqsi() =
        runBlocking {
            val r = repo()
            r.setSelected(CardPaymentMethod.Aqsi)
            assertEquals(CardPaymentMethod.Aqsi, r.getSelected())
        }

    @Test
    fun fromStorageString_legacyPaxMigratesToAqsi() {
        assertEquals(CardPaymentMethod.Aqsi, CardPaymentMethod.fromStorageString("PAX"))
        assertEquals(CardPaymentMethod.Aqsi, CardPaymentMethod.fromStorageString("pax"))
        assertEquals("AQSI", CardPaymentMethod.toStorageString(CardPaymentMethod.Aqsi))
    }

    @Test
    fun mockMode_legacyTwoCanMapsToDisabled() =
        runBlocking {
            val cfg = fakeConfig()
            cfg.set(JsonStoreKeys.CARD_PAYMENT_MOCK_MODE, "TWOCAN")
            val repo = CardPaymentMockModeRepositoryImpl(cfg)
            assertEquals(CardPaymentMockMode.Disabled, repo.getMode())
        }
}
