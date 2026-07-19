package com.viwa.android.hardware.controller

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ControllerGateway {
    suspend fun sendCommand(command: RequestCommand, payload: ByteArray)

    val incomingResponses: SharedFlow<ControllerResponseEvent>

 /** `true`, когда активно реальное USB-соединение (не мок). */
    val isPhysicalControllerConnected: StateFlow<Boolean>

 /** Симуляция входящего ответа (мок); вне мок-режима — no-op. */
    suspend fun simulateResponseForTests(command: ResponseCommand, payload: ByteArray) {}
}
