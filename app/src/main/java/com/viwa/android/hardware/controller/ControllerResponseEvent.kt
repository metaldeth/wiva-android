package com.viwa.android.hardware.controller

data class ControllerResponseEvent(
    val response: ResponseCommand,
    val payload: ByteArray,
)
