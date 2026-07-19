package com.viwa.android.data.repository

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.domain.model.CardPaymentMethod
import com.viwa.android.domain.repository.CardPaymentMethodRepository

class CardPaymentMethodRepositoryImpl(
    private val configRepository: ConfigRepository,
) : CardPaymentMethodRepository {

    override suspend fun getSelected(): CardPaymentMethod {
        val raw = configRepository.get(JsonStoreKeys.CARD_PAYMENT_METHOD)
        return CardPaymentMethod.fromStorageString(raw)
    }

    override suspend fun setSelected(method: CardPaymentMethod) {
        configRepository.set(
            JsonStoreKeys.CARD_PAYMENT_METHOD,
            CardPaymentMethod.toStorageString(method),
        )
    }
}
