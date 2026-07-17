package com.wiva.android.hardware.controller

data class ControllerResponseEvent(
    val response: ResponseCommand,
    val payload: ByteArray,
)
