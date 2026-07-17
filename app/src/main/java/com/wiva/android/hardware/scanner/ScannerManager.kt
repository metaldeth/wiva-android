package com.wiva.android.hardware.scanner

import android.content.Context
import com.wiva.android.domain.model.BarcodeEvent
import com.wiva.android.hardware.serial.PortRole
import com.wiva.android.hardware.serial.SerialConnection
import com.wiva.android.hardware.serial.SerialPortManager
import com.wiva.android.hardware.serial.UsbSerialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Чтение строк по USB-serial; роль порта [PortRole.SCANNER] в JsonStore
 * (ключ [com.wiva.android.data.local.db.JsonStoreKeys.PORT_ASSIGNMENTS]).
 */
@Singleton
class ScannerManager
@Inject
constructor(
    private val serialPortManager: SerialPortManager,
    private val usbSerialManager: UsbSerialManager,
    private val trafficLogger: WivaScannerTrafficLogger,
    @ApplicationContext @Suppress("unused") private val context: Context,
) {
    private val _barcodeFlow = MutableSharedFlow<BarcodeEvent>(replay = 0, extraBufferCapacity = 16)
    val barcodeFlow: SharedFlow<BarcodeEvent> = _barcodeFlow.asSharedFlow()

 /** Есть открытое USB-serial соединение для чтения штрихкодов (QR и т.д.). */
    private val _scannerSerialActive = MutableStateFlow(false)
    val scannerSerialActive: StateFlow<Boolean> = _scannerSerialActive.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readingJob: Job? = null
    private var currentConnection: SerialConnection? = null
    private var buffer = ByteArray(0)

    fun startReading() {
        readingJob?.cancel()
        _scannerSerialActive.value = false
        readingJob =
            scope.launch {
                while (isActive) {
                    try {
                        val drivers = usbSerialManager.getAvailableDevices()
                        val assignments = serialPortManager.getPortAssignments()
                        val scannerDriver =
                            drivers.firstOrNull { driver ->
                                assignments[driver.device.deviceName] == PortRole.SCANNER
                            } ?: drivers.firstOrNull()

                        if (scannerDriver == null) {
                            _scannerSerialActive.value = false
                            Timber.d("Scanner: no USB device found, retrying in 3s")
                            delay(3000)
                            continue
                        }

                        if (!usbSerialManager.hasPermission(scannerDriver.device)) {
                            _scannerSerialActive.value = false
                            Timber.d("Scanner: no USB permission for ${scannerDriver.device.deviceName}")
                            delay(5000)
                            continue
                        }

                        val openResult = usbSerialManager.openConnection(scannerDriver)
                        if (openResult == null) {
                            _scannerSerialActive.value = false
                            Timber.e("Scanner: failed to open connection")
                            delay(3000)
                            continue
                        }
                        val connection = openResult.first
                        val usbConn = openResult.second
                        currentConnection = connection
                        _scannerSerialActive.value = true
                        Timber.i("Scanner: connected to ${scannerDriver.device.deviceName}")

                        try {
                            while (isActive && connection.isOpen) {
                                val bytes = connection.read(timeoutMs = 100) ?: continue
                                processBytes(bytes)
                            }
                        } finally {
                            connection.close()
                            usbConn.close()
                            currentConnection = null
                            _scannerSerialActive.value = false
                            Timber.d("Scanner: connection closed")
                        }
                    } catch (e: Exception) {
                        _scannerSerialActive.value = false
                        if (e is CancellationException) throw e
                        Timber.e(e, "Scanner: error, reconnecting in 3s")
                        delay(3000)
                    }
                }
            }
    }

    private suspend fun processBytes(bytes: ByteArray) {
        buffer += bytes
        while (true) {
            val crIdx = buffer.indexOfFirst { it == '\r'.code.toByte() || it == '\n'.code.toByte() }
            if (crIdx < 0) break
            val line = if (crIdx > 0) buffer.copyOfRange(0, crIdx) else ByteArray(0)
            buffer =
                if (crIdx + 1 < buffer.size) {
                    buffer.copyOfRange(crIdx + 1, buffer.size)
                } else {
                    ByteArray(0)
                }

            val parsed = ScannerProtocol.parse(line) ?: continue
            val event = ScannerProtocol.classifyBarcode(parsed)
            Timber.d("Scanner barcode: $parsed → $event")
            trafficLogger.log(parsed, event)
            _barcodeFlow.emit(event)
        }
    }

    fun stop() {
        readingJob?.cancel()
        readingJob = null
        currentConnection?.close()
        currentConnection = null
        _scannerSerialActive.value = false
    }
}
