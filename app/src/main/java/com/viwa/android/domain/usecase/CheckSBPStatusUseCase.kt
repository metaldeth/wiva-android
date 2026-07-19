package com.viwa.android.domain.usecase

import com.viwa.android.domain.repository.SBPRepository
import javax.inject.Inject

class CheckSBPStatusUseCase @Inject constructor(
    private val repo: SBPRepository,
) {
    suspend operator fun invoke(orderId: String) = repo.getSBPLinkStatus(orderId)
}
