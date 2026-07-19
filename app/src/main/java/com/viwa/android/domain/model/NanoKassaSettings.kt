package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NanoKassaSettings(
    val kassaId: String = "123521",
    val kassaToken: String = "51a6f396e8cbd8f3967a724250e44009",
    val kkt: String = "550101022040",
    val address: String = "Тест Технопарк",
    val place: String = "Место установки",
 /** Последняя [com.viwa.android.domain.repository.NanoKassaRepository.verifyIntegration] успешна. */
    val lastIntegrationVerifyOk: Boolean = false,
    val lastVerifyAtEpochMs: Long = 0L,
)
