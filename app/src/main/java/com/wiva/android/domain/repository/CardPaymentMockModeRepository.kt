package com.wiva.android.domain.repository

import com.wiva.android.domain.model.CardPaymentMockMode
import com.wiva.android.domain.model.CardPaymentMockOutcome

interface CardPaymentMockModeRepository {
    suspend fun getMode(): CardPaymentMockMode

    suspend fun setMode(mode: CardPaymentMockMode)

    suspend fun getOutcome(): CardPaymentMockOutcome

    suspend fun setOutcome(outcome: CardPaymentMockOutcome)
}
