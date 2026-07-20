package com.viwa.android.hardware.scanner

import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.ViwaSerialPort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViwaScannerPortAutoDiscovery
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
) {
    suspend fun hasCandidates(): Boolean = candidateDevices().isNotEmpty()

    suspend fun discover(): String? {
        val candidates = candidateDevices()
        if (candidates.isEmpty()) return null
        return candidates.minBy { it.deviceName }.deviceName
    }

    private suspend fun candidateDevices() =
        serialPort.availableDevices()
            .filter { it.driverType != null }
            .filter { device ->
                val role = serialPort.assignments()[device.deviceName]
                role == null || role == PortRole.UNKNOWN || role == PortRole.UNASSIGNED
            }
}
