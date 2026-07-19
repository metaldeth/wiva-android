package com.viwa.android.hardware.controller

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class DelegatingControllerGateway
@Inject
constructor(
    private val hardware: ControllerHardwareManager,
) : ControllerGateway {
    override val incomingResponses: SharedFlow<ControllerResponseEvent> =
        hardware.incomingResponses

    override val isPhysicalControllerConnected: StateFlow<Boolean> =
        hardware.isPhysicalControllerConnected

    override suspend fun sendCommand(command: RequestCommand, payload: ByteArray) {
        hardware.sendCommand(command, payload)
    }

    override suspend fun simulateResponseForTests(command: ResponseCommand, payload: ByteArray) {
        hardware.simulateResponseForTests(command, payload)
    }
}
