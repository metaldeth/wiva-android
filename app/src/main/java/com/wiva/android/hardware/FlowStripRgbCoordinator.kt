package com.wiva.android.hardware

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.repository.ConfigRepository
import com.wiva.android.di.AppIoScope
import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.RequestCommand
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * RGB-лента Flow (0xD2): сохранённый цвет из [JsonStoreKeys.FLOW_STRIP_RGB_ARGB],
 * применение при старте/после переподключения, зелёный на 10 с после успешной готовки.
 */
@Singleton
class FlowStripRgbCoordinator
@Inject
constructor(
    private val configRepository: ConfigRepository,
    private val gateway: ControllerGateway,
    private val hardware: com.wiva.android.hardware.controller.ControllerHardwareManager,
    @AppIoScope private val scope: CoroutineScope,
) {
    private var greenThenRestoreJob: Job? = null

    init {
        hardware.registerAfterInitializeFromConfig {
            try {
                applySavedColorFromConfig()
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "applySavedColorFromConfig after init")
            }
        }
    }

    suspend fun getSavedArgb(): Int = readSavedArgbFromConfig()

    suspend fun applySavedColorFromConfig() {
        sendFlowRgbArgb(readSavedArgbFromConfig())
    }

    suspend fun sendFlowRgbArgb(argb: Int) {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        gateway.sendCommand(
            RequestCommand.SetFlowRgb,
            byteArrayOf(r.toByte(), g.toByte(), b.toByte(), 0, 0),
        )
    }

 /**
 * После [com.wiva.android.services.preparing.CustomerPreparingPhase.DrinkReady]: зелёный 10 с, затем цвет из настроек.
 */
 /** Отмена отложенного возврата к сохранённому цвету (например, при старте новой готовки). */
    fun cancelPendingGreenSchedule() {
        greenThenRestoreJob?.cancel()
        greenThenRestoreJob = null
    }

    fun scheduleGreenForTenSecondsThenRestoreSaved() {
        greenThenRestoreJob?.cancel()
        greenThenRestoreJob =
            scope.launch {
                runCatching {
                    val saved = readSavedArgbFromConfig()
                    val greenArgb = (0xFF shl 24) or (0 shl 16) or (255 shl 8) or 0
                    sendFlowRgbArgb(greenArgb)
                    delay(GREEN_DURATION_MS)
                    sendFlowRgbArgb(saved)
                }.onFailure { Timber.tag(TAG).w(it, "scheduleGreen strip") }
            }
    }

    private suspend fun readSavedArgbFromConfig(): Int {
        val raw = configRepository.get(JsonStoreKeys.FLOW_STRIP_RGB_ARGB)
        return parseFlowStripArgbFromStore(raw)
    }

    companion object {
        const val TAG = "FlowStripRgb"

 /** Как кнопка «128,0,255» и сброс в сервисном меню. */
        const val DEFAULT_FLOW_STRIP_RGB_ARGB =
            (0xFF shl 24) or (128 shl 16) or (0 shl 8) or 255

        private const val GREEN_DURATION_MS = 10_000L

        private fun parseFlowStripArgbFromStore(raw: String?): Int {
            val t = raw?.trim()?.removePrefix("#")?.uppercase().orEmpty()
            if (t.length != 8) return DEFAULT_FLOW_STRIP_RGB_ARGB
            return try {
                java.lang.Long.parseUnsignedLong(t, 16).toInt()
            } catch (_: NumberFormatException) {
                DEFAULT_FLOW_STRIP_RGB_ARGB
            }
        }
    }
}
