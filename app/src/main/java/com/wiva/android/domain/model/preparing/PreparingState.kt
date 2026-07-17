package com.wiva.android.domain.model.preparing

/***/
sealed class PreparingState {
    data object StartPreparing : PreparingState()

    data class Begin(
        val timeSec: Int,
    ) : PreparingState()

    data object Success : PreparingState()

    data class Fail(
        val errorCode: String?,
        val message: String?,
    ) : PreparingState()

    data object CupTaken : PreparingState()
}
