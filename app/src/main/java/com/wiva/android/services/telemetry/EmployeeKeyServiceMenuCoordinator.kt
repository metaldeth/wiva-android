package com.wiva.android.services.telemetry

import com.wiva.android.data.remote.telemetry.ConnectionState
import com.wiva.android.di.AppIoScope
import com.wiva.android.domain.model.BarcodeEvent
import com.wiva.android.hardware.scanner.ScannerManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "EmployeeKeyMenu"

/**
 * Скан `KEY-*` / `EMP:*` на экране покупки: [WivaTelemetryService.sendAuthCodeRequest], при успехе — открыть сервисное меню
 * без пароля (.requestAuthCodeExport).
 */
@Singleton
class EmployeeKeyServiceMenuCoordinator
    @Inject
    constructor(
        private val scannerManager: ScannerManager,
        private val telemetryService: WivaTelemetryService,
        @AppIoScope private val appScope: CoroutineScope,
    ) {
        private val _openServiceMenu =
            MutableSharedFlow<Unit>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val openServiceMenuRequests: SharedFlow<Unit> = _openServiceMenu.asSharedFlow()

        init {
            appScope.launch {
                scannerManager.barcodeFlow.collect { event ->
                    if (event !is BarcodeEvent.EmployeeKey) return@collect
                    if (telemetryService.connectionState.value !is ConnectionState.Connected) {
                        Timber.tag(TAG).w("scan ignored: telemetry WS not connected")
                        return@collect
                    }
                    val result = telemetryService.sendAuthCodeRequest(event.code)
                    if (result.success) {
                        _openServiceMenu.emit(Unit)
                    } else {
                        Timber.tag(TAG).i("authCode rejected: %s", result.message)
                    }
                }
            }
        }
    }
