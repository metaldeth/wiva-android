package com.viwa.android.data.remote.telemetry

sealed class ConnectionState {
    data object Connecting : ConnectionState()

    data object Connected : ConnectionState()

    data class Disconnected(val retryInMs: Long = 0) : ConnectionState()

    data class Error(val message: String) : ConnectionState()
}
