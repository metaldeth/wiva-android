package com.wiva.android.domain.usecase

import com.wiva.android.domain.repository.SBPRepository
import javax.inject.Inject

class CheckSBPStatusUseCase @Inject constructor(
    private val repo: SBPRepository,
) {
    suspend operator fun invoke(orderId: String) = repo.getSBPLinkStatus(orderId)
}
