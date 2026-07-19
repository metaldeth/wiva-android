package com.viwa.android.domain.repository

import com.viwa.android.domain.model.SBPLink
import com.viwa.android.domain.model.SBPSettings
import com.viwa.android.domain.model.SBPStatus

interface SBPRepository {
    suspend fun getSBPLink(amountKopecks: Int): Result<SBPLink>

    suspend fun getSBPLinkStatus(orderId: String): Result<SBPStatus>

    suspend fun cancelSBPLink(orderId: String): Result<Unit>

    suspend fun getSettings(): SBPSettings

    suspend fun updateSettings(settings: SBPSettings)
}
