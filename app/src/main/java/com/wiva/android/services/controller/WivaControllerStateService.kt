package com.wiva.android.services.controller

import com.wiva.android.hardware.controller.ControllerHardwareManager
import com.wiva.android.hardware.controller.ResponseCommand
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

@Singleton
class WivaControllerStateService
@Inject
constructor(
    private val hardware: ControllerHardwareManager,
) {
 /**
 * Убедиться, что режим устройства — Auto (1): ReadDeviceMode → при необходимости ServiceCommand [0x07,0,0,0,0].
 */
    suspend fun ensureAutoMode(timeoutMs: Long = 5000L) =
        coroutineScope {
            if (!hardware.hasActiveConnection()) {
                throw IllegalStateException("Контроллер не доступен")
            }
            val mode = hardware.readDeviceModeByte()
            if (mode == 1) {
                return@coroutineScope
            }
            if (mode < 0) {
                throw IllegalStateException("Timeout waiting for device mode")
            }
            withTimeout(timeoutMs) {
                val waiter =
                    async {
                        hardware.incomingResponses.first {
                            it.response == ResponseCommand.AutoChangeDeviceMode &&
                                (it.payload.firstOrNull()?.toInt()?.and(0xff) == 1)
                        }
                    }
                yield()
                hardware.setAutoModeCommand()
                waiter.await()
            }
        }
}
