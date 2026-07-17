package com.wiva.android.domain.repository

import com.wiva.android.domain.model.AgeVerificationResult
import com.wiva.android.domain.model.MaxSettings

interface MaxRepository {
    suspend fun verifyAge(sessionId: String): Result<AgeVerificationResult>

    suspend fun getSettings(): MaxSettings

    suspend fun updateSettings(settings: MaxSettings)
}
