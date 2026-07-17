package com.wiva.android.domain.usecase

import com.wiva.android.domain.repository.SBPRepository
import javax.inject.Inject

class GetSBPLinkUseCase @Inject constructor(
    private val repo: SBPRepository,
) {
    suspend operator fun invoke(amountKopecks: Int) = repo.getSBPLink(amountKopecks)
}
