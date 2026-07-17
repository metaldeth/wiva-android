package com.wiva.android.hardware.controller

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import timber.log.Timber

/**
 * *
 * Накопленный расход в [JsonStoreKeys.WATER_USAGE_ML] пополняется только через
 * [accumulateHardwareReadingAfterSuccessfulPreparation] после успешной готовки (чтение + сброс на контроллере).
 */
@Singleton
class WivaWaterCounterService
@Inject
constructor(
    private val hardware: ControllerHardwareManager,
    private val configRepository: ConfigRepository,
) {
    suspend fun getWaterUsageMl(): Int =
        coroutineScope {
            val awaitAnswer =
                async {
                    hardware.incomingResponses.first {
                        it.response == ResponseCommand.WaterCounterAnswer
                    }
                }
            yield()
            hardware.sendCommand(RequestCommand.ReadWaterCounter, ControllerConstants.DEFAULT_BODY)
            val answer =
                withTimeoutOrNull(ControllerConstants.WATER_COUNTER_TIMEOUT_MS) {
                    awaitAnswer.await()
                }
            if (answer == null) {
                return@coroutineScope 0
            }
            val p = answer.payload
            val ml =
                if (p.size >= 2) {
                    ((p[0].toInt() and 0xff) shl 8) or (p[1].toInt() and 0xff)
                } else {
                    0
                }
            hardware.sendCommand(RequestCommand.ResetWaterCounter, ControllerConstants.DEFAULT_BODY)
            ml
        }

 /**
 * Вызывать после каждой успешной готовки: считать мл с контроллера (и сбросить его счётчик),
 * прибавить к [JsonStoreKeys.WATER_USAGE_ML].
 * @return прочитанное приращение, мл
 */
    suspend fun accumulateHardwareReadingAfterSuccessfulPreparation(): Int {
        val delta = getWaterUsageMl()
        if (delta <= 0) return 0
        val current = configRepository.get(JsonStoreKeys.WATER_USAGE_ML)?.toDoubleOrNull() ?: 0.0
        val next = current + delta
        configRepository.set(JsonStoreKeys.WATER_USAGE_ML, next.toString())
        Timber.tag(TAG).i("water usage +%d ml → total %.1f ml", delta, next)
        return delta
    }

    suspend fun getAccumulatedWaterUsageMl(): Double =
        configRepository.get(JsonStoreKeys.WATER_USAGE_ML)?.toDoubleOrNull() ?: 0.0

    suspend fun resetWaterUsage() {
        hardware.sendCommand(RequestCommand.ResetWaterCounter, ControllerConstants.DEFAULT_BODY)
    }

    companion object {
        private const val TAG = "WivaWaterCounter"
    }
}
