package com.wiva.android.hardware.controller

/***/
data class ConnectToPortResult(
    val success: Boolean,
    val firmware: String? = null,
    val errorCode: Int? = null,
    val mode: Int? = null,
    val failedStep: ConnectFailedStep? = null,
) {
    enum class ConnectFailedStep {
        FIRMWARE,
        ERRORS,
        MODE,
    }
}
