package com.viwa.android.domain.repository

import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome

interface CardPaymentMockModeRepository {
    suspend fun getMode(): CardPaymentMockMode

    suspend fun setMode(mode: CardPaymentMockMode)

    suspend fun getOutcome(): CardPaymentMockOutcome

    suspend fun setOutcome(outcome: CardPaymentMockOutcome)
}
