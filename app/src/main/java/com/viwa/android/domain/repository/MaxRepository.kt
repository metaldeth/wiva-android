package com.viwa.android.domain.repository

import com.viwa.android.domain.model.AgeVerificationResult
import com.viwa.android.domain.model.MaxSettings

interface MaxRepository {
    suspend fun verifyAge(sessionId: String): Result<AgeVerificationResult>

    suspend fun getSettings(): MaxSettings

    suspend fun updateSettings(settings: MaxSettings)
}
