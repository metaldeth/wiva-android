package com.viwa.android.hardware.devices

import com.viwa.android.hardware.controller.ControllerHardwareManager
import com.viwa.android.hardware.serial.ViwaSerialPort
import com.viwa.android.ui.screens.service.devices.ViwaControllerPortScanProgress
import com.viwa.android.ui.screens.service.devices.ViwaControllerPortScanResult
import com.viwa.android.ui.screens.service.devices.ViwaControllerPortScanType
import com.viwa.android.ui.screens.service.devices.ViwaPortScanLogEntry
import com.viwa.android.ui.screens.service.devices.ViwaPortScanLogKind
import com.viwa.android.ui.screens.service.devices.ViwaPortScanProtocol
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

fun interface ViwaControllerPortScanPort {
    suspend fun scan(
        onProgress: suspend (ViwaControllerPortScanProgress) -> Unit,
        onLog: suspend (ViwaPortScanLogEntry) -> Unit,
    ): List<ViwaControllerPortScanResult>
}

@Singleton
class ViwaControllerPortScanner
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
    private val controllerHardware: ControllerHardwareManager,
) : ViwaControllerPortScanPort {
    override suspend fun scan(
        onProgress: suspend (ViwaControllerPortScanProgress) -> Unit,
        onLog: suspend (ViwaPortScanLogEntry) -> Unit,
    ): List<ViwaControllerPortScanResult> {
        onProgress(ViwaControllerPortScanProgress.Started)
        val devices = serialPort.availableDevices().sortedBy { it.deviceName }
        if (devices.isEmpty()) {
            onProgress(ViwaControllerPortScanProgress.NoPorts)
            return emptyList()
        }
        return devices.map { device ->
            val path = device.deviceName
            onProgress(ViwaControllerPortScanProgress.ProbingPrimary(path))
            onLog(
                ViwaPortScanLogEntry(
                    kind = ViwaPortScanLogKind.Info,
                    port = path,
                    protocol = ViwaPortScanProtocol.Primary,
                    payload = "probe start",
                ),
            )
            val ok = runCatching { controllerHardware.connectToPort(path).success }.getOrDefault(false)
            if (ok) {
                Timber.tag(TAG).i("controller found on %s", path)
                onProgress(ViwaControllerPortScanProgress.PrimaryFound(path))
                onLog(
                    ViwaPortScanLogEntry(
                        kind = ViwaPortScanLogKind.Rx,
                        port = path,
                        protocol = ViwaPortScanProtocol.Primary,
                        payload = "connect OK",
                    ),
                )
                ViwaControllerPortScanResult(path, ViwaControllerPortScanType.Primary)
            } else {
                onProgress(ViwaControllerPortScanProgress.NotFound(path))
                onLog(
                    ViwaPortScanLogEntry(
                        kind = ViwaPortScanLogKind.Info,
                        port = path,
                        protocol = ViwaPortScanProtocol.Primary,
                        payload = "not controller",
                    ),
                )
                ViwaControllerPortScanResult(path, ViwaControllerPortScanType.None)
            }
        }
    }

    companion object {
        private const val TAG = "ViwaPortScan"
    }
}

@Singleton
class ViwaControllerManualPortProbe
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
    private val controllerHardware: ControllerHardwareManager,
) {
    suspend fun probePrimaryVersion(
        devicePath: String,
        onLog: suspend (ViwaPortScanLogEntry) -> Unit,
    ) {
        onLog(info(devicePath, "TX ReadFirmwareVersion"))
        val ok = runCatching { controllerHardware.connectToPort(devicePath).success }.getOrDefault(false)
        onLog(info(devicePath, if (ok) "RX connect OK" else "RX failed"))
    }

    suspend fun probeConnectedPrimaryVersion(
        onLog: suspend (ViwaPortScanLogEntry) -> Unit,
    ) {
        val path = serialPort.controllerDevicePath() ?: run {
            onLog(info("connected", "no assigned controller"))
            return
        }
        probePrimaryVersion(path, onLog)
    }

    private fun info(port: String, message: String): ViwaPortScanLogEntry =
        ViwaPortScanLogEntry(
            kind = ViwaPortScanLogKind.Info,
            port = port,
            protocol = ViwaPortScanProtocol.Primary,
            payload = message,
        )
}
