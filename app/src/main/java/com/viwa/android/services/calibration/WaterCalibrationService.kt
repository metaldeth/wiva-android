package com.viwa.android.services.calibration

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.WaterCalibrationData
import com.viwa.android.hardware.controller.ControllerConstants
import com.viwa.android.hardware.controller.ControllerHardwareManager
import com.viwa.android.hardware.controller.RequestCommand
import com.viwa.android.hardware.controller.ResponseCommand
import com.viwa.android.services.preparing.PreparingTimeHistoryStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

sealed class WaterPourResult {
    data class Success(
        val durationSec: Double,
        val startMs: Long,
        val endMs: Long,
    ) : WaterPourResult()

    data class Failure(
        val message: String,
        val startMs: Long? = null,
        val endMs: Long? = null,
    ) : WaterPourResult()
}

sealed class WaterCalibrationWriteResult {
    data class Success(
        val data: WaterCalibrationData,
    ) : WaterCalibrationWriteResult()

    data class Failure(
        val message: String,
    ) : WaterCalibrationWriteResult()
}

@Singleton
class WaterCalibrationService
@Inject
constructor(
    private val configRepository: ConfigRepository,
    private val hardware: ControllerHardwareManager,
    private val preparingTimeHistoryStore: PreparingTimeHistoryStore,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val mutex = Mutex()

    companion object {
        private const val TAG = "WaterCalibration"
        private const val POUR_TIMEOUT_MS = 120_000L
        private const val READ_PUMP_TIMEOUT_MS = 3_000L
        private const val ACK_TIMEOUT_MS = 2_000L
    }

    suspend fun loadCalibration(): WaterCalibrationData {
        val raw = configRepository.getJson(JsonStoreKeys.WATER_CALIBRATION) ?: return WaterCalibrationData()
        return runCatching { json.decodeFromString<WaterCalibrationData>(raw) }.getOrDefault(WaterCalibrationData())
    }

    suspend fun loadAdaptiveWindowSize(): Int {
        val raw = configRepository.get(JsonStoreKeys.PREPARING_FLOW_WINDOW_SIZE)
        val parsed = raw?.toIntOrNull()
        return (parsed ?: WaterCalibrationCalculations.DEFAULT_ADAPTIVE_WINDOW_SIZE)
            .coerceIn(
                WaterCalibrationCalculations.MIN_ADAPTIVE_WINDOW_SIZE,
                WaterCalibrationCalculations.MAX_ADAPTIVE_WINDOW_SIZE,
            )
    }

    suspend fun saveAdaptiveWindowSize(windowSize: Int): Int {
        val normalized =
            windowSize.coerceIn(
                WaterCalibrationCalculations.MIN_ADAPTIVE_WINDOW_SIZE,
                WaterCalibrationCalculations.MAX_ADAPTIVE_WINDOW_SIZE,
            )
        configRepository.set(JsonStoreKeys.PREPARING_FLOW_WINDOW_SIZE, normalized.toString())
        return normalized
    }

    private suspend fun saveCalibration(data: WaterCalibrationData) {
        configRepository.setJson(
            JsonStoreKeys.WATER_CALIBRATION,
            json.encodeToString(WaterCalibrationData.serializer(), data),
        )
    }

 /**
 * Тестовый налив: [RequestCommand.ServiceCommand] mode 0x0A, тело.
 */
    suspend fun runTestPour(volumeMl: Int): WaterPourResult =
        mutex.withLock {
            if (!hardware.hasActiveConnection()) {
                return WaterPourResult.Failure("Контроллер недоступен")
            }
            val clampedMl = volumeMl.coerceIn(0, 65_535)
            val fifth = (clampedMl / 10).coerceIn(0, 255)
            val body = byteArrayOf(0x0a, 0, 0, 0, fifth.toByte())

            val pourStartWallMs = System.currentTimeMillis()
            val outcome =
                withTimeoutOrNull(POUR_TIMEOUT_MS) {
                    coroutineScope {
                        val beginCh =
                            async {
                                hardware.incomingResponses.first {
                                    it.response == ResponseCommand.DrinkPreparingBegin
                                }
                            }
                        yield()
                        hardware.sendCommand(RequestCommand.ServiceCommand, body)
                        beginCh.await()
                        val t0 = System.currentTimeMillis()
                        hardware.incomingResponses.first {
                            it.response == ResponseCommand.DrinkPreparingSuccess
                        }
                        val endMs = System.currentTimeMillis()
                        val durationSec = (endMs - t0) / 1000.0
                        Triple(t0, endMs, durationSec)
                    }
                }

            if (outcome == null) {
                val endMs = System.currentTimeMillis()
                return WaterPourResult.Failure("Таймаут налива", startMs = pourStartWallMs, endMs = endMs)
            }

            val (t0, endMs, durationSec) = outcome
            val current = loadCalibration()
            val mergedTarget = clampedMl.takeIf { it > 0 }

            if (durationSec >= WaterCalibrationCalculations.MIN_POUR_DURATION_SEC) {
                saveCalibration(
                    current.copy(
                        lastPourDurationSec = durationSec,
                        lastPourTimestampMs = endMs,
                        lastTargetMl = mergedTarget,
                    ),
                )
                return WaterPourResult.Success(durationSec = durationSec, startMs = t0, endMs = endMs)
            }

            saveCalibration(
                current.copy(
                    lastPourTimestampMs = endMs,
                    lastTargetMl = mergedTarget,
                ),
            )
            return WaterPourResult.Failure(
                "Длительность налива меньше минимальной",
                startMs = t0,
                endMs = endMs,
            )
        }

    suspend fun writeCoefficient(
        targetVolumeMl: Int,
        actualVolumeMl: Int,
    ): WaterCalibrationWriteResult =
        mutex.withLock {
            if (actualVolumeMl <= 0) {
                return WaterCalibrationWriteResult.Failure("Фактический объём должен быть больше 0")
            }
            if (!hardware.hasActiveConnection()) {
                return WaterCalibrationWriteResult.Failure("Контроллер недоступен")
            }

            val stored = loadCalibration()
            val lastPour = stored.lastPourDurationSec
            if (lastPour == null || lastPour <= 0) {
                return WaterCalibrationWriteResult.Failure("Сначала выполните тестовый налив")
            }

            val target = targetVolumeMl.coerceAtLeast(0).toDouble()
            val actual = actualVolumeMl.toDouble()

            val answer =
                coroutineScope {
                    val awaitAnswer =
                        async {
                            hardware.incomingResponses.first {
                                it.response == ResponseCommand.WaterPumpModelAnswer
                            }
                        }
                    yield()
                    hardware.sendCommand(RequestCommand.ReadWaterPumpModel, ControllerConstants.DEFAULT_BODY)
                    withTimeoutOrNull(READ_PUMP_TIMEOUT_MS) { awaitAnswer.await() }
                }
            if (answer == null || answer.payload.isEmpty()) {
                return WaterCalibrationWriteResult.Failure("Таймаут чтения коэффициента")
            }

            val currentTenths = answer.payload[0].toInt() and 0xff
            val newTenths =
                WaterCalibrationCalculations.computeNewTenths(
                    currentTenths = currentTenths,
                    targetVolumeMl = target,
                    actualVolumeMl = actual,
                )
            val writeBody = ByteArray(5) { newTenths.toByte() }

            val ackReceived =
                coroutineScope {
                    val awaitAck =
                        async {
                            hardware.incomingResponses.first {
                                it.response == ResponseCommand.ControllerACK
                            }
                        }
                    yield()
                    hardware.sendCommand(RequestCommand.WriteWaterPumpModel, writeBody)
                    withTimeoutOrNull(ACK_TIMEOUT_MS) { awaitAck.await() }
                }

            if (ackReceived == null) {
                return WaterCalibrationWriteResult.Failure("Таймаут подтверждения записи коэффициента")
            }

            val flowRate =
                WaterCalibrationCalculations.computeFlowRateMlPerSec(
                    actualVolumeMl = actual,
                    lastPourDurationSec = lastPour,
                )

            val updated =
                stored.copy(
                    lastTargetMl = targetVolumeMl,
                    lastActualMl = actualVolumeMl,
                    flowRateMlPerSec = flowRate,
                    calibratedFlowRateMlPerSec = flowRate,
                    adaptiveFlowRateMlPerSec = flowRate,
                )
            saveCalibration(updated)
            Timber.tag(TAG).i("water calibration saved flowRate=%s", flowRate)
            return WaterCalibrationWriteResult.Success(data = updated)
        }

    suspend fun applyObservedFlowRate(
        actualWaterMl: Double,
        actualTimeSec: Double,
    ): WaterCalibrationData? =
        mutex.withLock {
            val observedFlow =
                WaterCalibrationCalculations.computeFlowRateMlPerSec(
                    actualVolumeMl = actualWaterMl,
                    lastPourDurationSec = actualTimeSec,
                ) ?: return@withLock null
            val stored = loadCalibration()
            val baseFlow = stored.adaptiveFlowRateMlPerSec ?: stored.flowRateMlPerSec
            val windowSize = loadAdaptiveWindowSize()
            val adaptiveFlow =
                WaterCalibrationCalculations.computeAdaptiveFlowRateMlPerSec(
                    currentFlowRateMlPerSec = baseFlow,
                    observedFlowRateMlPerSec = observedFlow,
                    windowSize = windowSize,
                ) ?: return@withLock null

            val updated =
                stored.copy(
                    flowRateMlPerSec = adaptiveFlow,
                    adaptiveFlowRateMlPerSec = adaptiveFlow,
                    lastObservedFlowRateMlPerSec = observedFlow,
                    lastAdaptiveUpdateTimestampMs = System.currentTimeMillis(),
                )
            saveCalibration(updated)
            Timber.tag(TAG).i(
                "adaptive flow updated observed=%s base=%s new=%s window=%s",
                observedFlow,
                baseFlow,
                adaptiveFlow,
                windowSize,
            )
            return@withLock updated
        }

    suspend fun recomputeFlowRateFromHistory(windowSize: Int): WaterCalibrationData? =
        mutex.withLock {
            val normalizedWindow = saveAdaptiveWindowSize(windowSize)
            val records = preparingTimeHistoryStore.loadAll().takeLast(normalizedWindow)
            val observedFlows =
                records.mapNotNull { rec ->
                    val observed =
                        WaterCalibrationCalculations.computeFlowRateMlPerSec(
                            actualVolumeMl = rec.actualWaterMl,
                            lastPourDurationSec = rec.actualTimeSec,
                        )
                    WaterCalibrationCalculations.sanitizeFlowRate(observed)
                }
            if (observedFlows.isEmpty()) return@withLock null

            val averagedObservedFlow = observedFlows.average()
            val stored = loadCalibration()
            val updated =
                stored.copy(
                    flowRateMlPerSec = averagedObservedFlow,
                    adaptiveFlowRateMlPerSec = averagedObservedFlow,
                    lastObservedFlowRateMlPerSec = observedFlows.last(),
                    lastAdaptiveUpdateTimestampMs = System.currentTimeMillis(),
                )
            saveCalibration(updated)
            Timber.tag(TAG).i(
                "flow recomputed from history window=%s records=%s new=%s",
                normalizedWindow,
                observedFlows.size,
                averagedObservedFlow,
            )
            return@withLock updated
        }
}
