package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.domain.repository.CardPaymentMockModeRepository

class CardPaymentMockModeRepositoryImpl(
    private val configRepository: ConfigRepository,
) : CardPaymentMockModeRepository {

    override suspend fun getMode(): CardPaymentMockMode {
        val raw = configRepository.get(JsonStoreKeys.CARD_PAYMENT_MOCK_MODE)
        return CardPaymentMockMode.fromStorageString(raw)
    }

    override suspend fun setMode(mode: CardPaymentMockMode) {
        configRepository.set(
            JsonStoreKeys.CARD_PAYMENT_MOCK_MODE,
            CardPaymentMockMode.toStorageString(mode),
        )
    }

    override suspend fun getOutcome(): CardPaymentMockOutcome {
        val raw = configRepository.get(JsonStoreKeys.CARD_PAYMENT_MOCK_OUTCOME)
        return CardPaymentMockOutcome.fromStorageString(raw)
    }

    override suspend fun setOutcome(outcome: CardPaymentMockOutcome) {
        configRepository.set(
            JsonStoreKeys.CARD_PAYMENT_MOCK_OUTCOME,
            CardPaymentMockOutcome.toStorageString(outcome),
        )
    }
}
