package com.wiva.android.services.telemetry

import com.wiva.android.data.remote.telemetry.mvp.TelemetryRegistrationQrParser
import com.wiva.android.data.remote.telemetry.mvp.TelemetryRegistrationQrParseResult
import com.wiva.android.di.AppIoScope
import com.wiva.android.domain.model.BarcodeEvent
import com.wiva.android.hardware.scanner.ScannerManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** Fills telemetry registration fields from scanner without auto-registering. */
data class TelemetryRegistrationScanUiEvent(
    val registrationKey: String,
    val serialNumber: String? = null,
    val apiUrl: String? = null,
)

@Singleton
class TelemetryRegistrationScannerCoordinator
@Inject
constructor(
    private val scannerManager: ScannerManager,
    @AppIoScope private val appScope: CoroutineScope,
) {
    private val _scanEvents =
        MutableSharedFlow<TelemetryRegistrationScanUiEvent>(
            replay = 0,
            extraBufferCapacity = 4,
        )
    val scanEvents: SharedFlow<TelemetryRegistrationScanUiEvent> = _scanEvents.asSharedFlow()

    init {
        appScope.launch {
            scannerManager.barcodeFlow.collect { event ->
                val raw =
                    when (event) {
                        is BarcodeEvent.RegistrationKey -> event.code
                        is BarcodeEvent.TelemetryRegistrationQr -> event.raw
                        else -> return@collect
                    }
                when (val parsed = TelemetryRegistrationQrParser.parse(raw)) {
                    is TelemetryRegistrationQrParseResult.Success -> {
                        Timber.i("TelemetryRegistrationScanner: QR считан")
                        _scanEvents.emit(
                            TelemetryRegistrationScanUiEvent(
                                registrationKey = parsed.scan.registrationKey,
                                serialNumber = parsed.scan.serialNumber,
                                apiUrl = parsed.scan.apiUrl,
                            ),
                        )
                    }
                    is TelemetryRegistrationQrParseResult.Invalid -> {
                        if (event is BarcodeEvent.RegistrationKey || event is BarcodeEvent.TelemetryRegistrationQr) {
                            Timber.w("TelemetryRegistrationScanner: invalid scan")
                        }
                    }
                }
            }
        }
    }
}
