package com.viwa.android.services.preparing

sealed class PrepareDrinkResult {
    data class Ok(
        val estSeconds: Int,
    ) : PrepareDrinkResult()

    data class Error(
        val errorCode: String?,
        val message: String?,
    ) : PrepareDrinkResult()
}
