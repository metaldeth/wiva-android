package com.viwa.android.hardware.devices

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.payment.aqsi.setup.AqsiPaymentStartupInitializer
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.hardware.controller.ControllerHardwareManager
import com.viwa.android.hardware.scanner.ScannerManager
import com.viwa.android.hardware.scanner.ViwaScannerStartupInitializer
import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.ViwaSerialPort
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeviceRuntimeDiscoveryResult {
    data class Connected(val devicePath: String?) : DeviceRuntimeDiscoveryResult

    data object NotFound : DeviceRuntimeDiscoveryResult
}

@Singleton
class ViwaDeviceRuntimeDiscovery
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
    private val scannerStartupInitializer: ViwaScannerStartupInitializer,
    private val aqsiPaymentStartupInitializer: AqsiPaymentStartupInitializer,
    private val scannerManager: ScannerManager,
    private val controllerPortProbe: ViwaControllerPortProbe,
    private val controllerHardware: ControllerHardwareManager,
    private val configRepository: ConfigRepository,
) {
    suspend fun discoverScanner(): DeviceRuntimeDiscoveryResult {
        scannerStartupInitializer.assignIfNeeded()
        scannerManager.stop()
        scannerManager.startReading()
        val path = serialPort.assignedDeviceName(PortRole.SCANNER)
        return if (path != null) {
            DeviceRuntimeDiscoveryResult.Connected(path)
        } else {
            DeviceRuntimeDiscoveryResult.NotFound
        }
    }

    suspend fun discoverController(): DeviceRuntimeDiscoveryResult {
        if (configRepository.get(JsonStoreKeys.USE_MOCK_CONTROLLER)?.toBooleanStrictOrNull() == true) {
            return DeviceRuntimeDiscoveryResult.Connected(devicePath = null)
        }
        val path = controllerPortProbe.discover()
        if (path != null) {
            serialPort.assign(path, PortRole.CONTROLLER).getOrThrow()
            controllerHardware.connectToPort(path)
            return DeviceRuntimeDiscoveryResult.Connected(path)
        }
        return DeviceRuntimeDiscoveryResult.NotFound
    }

    suspend fun discoverPayment(): DeviceRuntimeDiscoveryResult {
        aqsiPaymentStartupInitializer.assignIfNeeded()
        val path = serialPort.assignedDeviceName(PortRole.PAYMENT)
        return if (path != null) {
            DeviceRuntimeDiscoveryResult.Connected(path)
        } else {
            DeviceRuntimeDiscoveryResult.NotFound
        }
    }
}
