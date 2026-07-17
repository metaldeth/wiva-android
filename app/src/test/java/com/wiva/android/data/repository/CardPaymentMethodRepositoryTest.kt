package com.wiva.android.data.repository

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.domain.model.CardPaymentMethod
import com.wiva.android.domain.model.CardPaymentMockMode
import com.wiva.android.domain.model.CardPaymentMockOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CardPaymentMethodRepositoryTest {

    private val fakeStore = FakeConfigRepository()

    private fun repo() = CardPaymentMethodRepositoryImpl(fakeStore)

    @Test
    fun getSelected_whenMissing_returnsPax() =
        runBlocking {
            assertEquals(CardPaymentMethod.Pax, repo().getSelected())
        }

    @Test
    fun roundTrip_aqsi() =
        runBlocking {
            val r = repo()
            r.setSelected(CardPaymentMethod.Aqsi)
            assertEquals(CardPaymentMethod.Aqsi, r.getSelected())
            assertEquals(CardPaymentMethod.STORAGE_AQSI, fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_METHOD])
        }

    @Test
    fun roundTrip_pax() =
        runBlocking {
            val r = repo()
            r.setSelected(CardPaymentMethod.Pax)
            assertEquals(CardPaymentMethod.Pax, r.getSelected())
            assertEquals(CardPaymentMethod.STORAGE_PAX, fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_METHOD])
        }

    @Test
    fun getSelected_whenUnknownStored_returnsPax() =
        runBlocking {
            fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_METHOD] = "NOT_A_METHOD"
            assertEquals(CardPaymentMethod.Pax, repo().getSelected())
        }

    @Test
    fun getSelected_whenEmptyStringStored_returnsPax() =
        runBlocking {
            fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_METHOD] = ""
            assertEquals(CardPaymentMethod.Pax, repo().getSelected())
        }

    @Test
    fun storageMapping_roundTrip_strings() {
        assertEquals(CardPaymentMethod.Pax, CardPaymentMethod.fromStorageString("PAX"))
        assertEquals(CardPaymentMethod.Aqsi, CardPaymentMethod.fromStorageString("AQSI"))
        assertEquals("PAX", CardPaymentMethod.toStorageString(CardPaymentMethod.Pax))
        assertEquals("AQSI", CardPaymentMethod.toStorageString(CardPaymentMethod.Aqsi))
    }

    @Test
    fun fromStorageString_caseInsensitivePaxAndAqsi() {
        assertEquals(CardPaymentMethod.Pax, CardPaymentMethod.fromStorageString("pax"))
        assertEquals(CardPaymentMethod.Aqsi, CardPaymentMethod.fromStorageString(" aqsi "))
    }

    @Test
    fun mockModeRepository_roundTrip_twoCanAndAqsi() =
        runBlocking {
            val repo = CardPaymentMockModeRepositoryImpl(fakeStore)
            repo.setMode(CardPaymentMockMode.TwoCan)
            assertEquals(CardPaymentMockMode.TwoCan, repo.getMode())
            assertEquals(CardPaymentMockMode.STORAGE_TWO_CAN, fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_MOCK_MODE])

            repo.setMode(CardPaymentMockMode.Aqsi)
            assertEquals(CardPaymentMockMode.Aqsi, repo.getMode())
        }

    @Test
    fun mockModeRepository_roundTrip_outcome() =
        runBlocking {
            val repo = CardPaymentMockModeRepositoryImpl(fakeStore)
            repo.setOutcome(CardPaymentMockOutcome.Timeout)
            assertEquals(CardPaymentMockOutcome.Timeout, repo.getOutcome())
            assertEquals(CardPaymentMockOutcome.STORAGE_TIMEOUT, fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_MOCK_OUTCOME])
        }

    @Test
    fun mockModeRepository_unknownValue_returnsDisabled() =
        runBlocking {
            fakeStore.strings[JsonStoreKeys.CARD_PAYMENT_MOCK_MODE] = "bad"
            assertEquals(CardPaymentMockMode.Disabled, CardPaymentMockModeRepositoryImpl(fakeStore).getMode())
        }

    private class FakeConfigRepository : ConfigRepository {
        val strings = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = strings[key]

        override suspend fun set(key: String, value: String) {
            strings[key] = value
        }

        override suspend fun delete(key: String) {
            strings.remove(key)
        }

        override suspend fun getJson(key: String): String? = strings[key]

        override suspend fun setJson(key: String, json: String) {
            strings[key] = json
        }
    }
}
