package com.wiva.android.services.drink

import com.wiva.android.hardware.controller.ControllerConstants
import com.wiva.android.hardware.controller.ControllerHardwareManager
import com.wiva.android.hardware.controller.RequestCommand
import com.wiva.android.hardware.controller.ResponseCommand
import com.wiva.android.di.AppIoScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class WivaDrinkPreparingService
@Inject
constructor(
    private val hardware: ControllerHardwareManager,
    @AppIoScope private val scope: CoroutineScope,
) {
    private var mockTimers: Job? = null

    fun startDrinkPreparing(preparingTimeSeconds: Int?) {
        mockTimers?.cancel()
        mockTimers = null

        if (hardware.isMockPortActive()) {
            hardware.logTxOnlyForDebugUi(
                RequestCommand.StartDrinkPreparing,
                ControllerConstants.START_DRINK_PREPARING_BODY,
            )
            val durationSec =
                if (preparingTimeSeconds != null && preparingTimeSeconds > 0) {
                    preparingTimeSeconds
                } else {
                    10
                }
            val beginMs = 1000L
            val empty = ByteArray(5)
            mockTimers =
                scope.launch {
                    delay(beginMs)
                    hardware.simulateResponseForTests(ResponseCommand.DrinkPreparingBegin, empty)
                    delay(durationSec * 1000L)
                    hardware.simulateResponseForTests(ResponseCommand.DrinkPreparingSuccess, empty)
                    Timber.tag(TAG).i("mock: DrinkPreparingSuccess")
                    mockTimers = null
                }
            return
        }

        scope.launch {
            try {
                hardware.sendCommand(
                    RequestCommand.StartDrinkPreparing,
                    ControllerConstants.START_DRINK_PREPARING_BODY,
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "StartDrinkPreparing failed")
            }
        }
    }

 /** Остановка мок-таймеров BEGIN/SUCCESS (аварийный выход с экрана готовки). */
    fun cancelMockPreparing() {
        mockTimers?.cancel()
        mockTimers = null
    }

    companion object {
        private const val TAG = "WivaDrinkPreparing"
    }
}
