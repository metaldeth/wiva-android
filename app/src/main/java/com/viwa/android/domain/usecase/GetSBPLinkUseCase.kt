package com.viwa.android.domain.usecase

import com.viwa.android.domain.repository.SBPRepository
import javax.inject.Inject

class GetSBPLinkUseCase @Inject constructor(
    private val repo: SBPRepository,
) {
    suspend operator fun invoke(amountKopecks: Int) = repo.getSBPLink(amountKopecks)
}
