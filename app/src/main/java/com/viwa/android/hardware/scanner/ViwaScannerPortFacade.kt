package com.viwa.android.hardware.scanner

import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.UsbSerialManager
import com.viwa.android.hardware.serial.ViwaSerialPort
import javax.inject.Inject
import javax.inject.Singleton

/** Адаптер scanner stack для DevicesBlockController (статус «сканер подключён»). */
@Singleton
class ViwaScannerPortFacade
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
    private val scannerManager: ScannerManager,
    private val usbSerialManager: UsbSerialManager,
) {
    val isReadingActive: Boolean
        get() = scannerManager.scannerSerialActive.value

    suspend fun activeOrAssignedDeviceName(): String? {
        serialPort.assignedDeviceName(PortRole.SCANNER)?.let { return it }
        if (!scannerManager.scannerSerialActive.value) return null
        return usbSerialManager.getAvailableDevices()
            .firstOrNull()
            ?.device
            ?.deviceName
    }

    suspend fun holdsUsbDevice(deviceName: String): Boolean {
        if (!scannerManager.scannerSerialActive.value) return false
        val assigned = serialPort.assignedDeviceName(PortRole.SCANNER)
        if (assigned != null) return assigned == deviceName
        return usbSerialManager.getAvailableDevices()
            .firstOrNull()
            ?.device
            ?.deviceName == deviceName
    }

    fun restart() {
        scannerManager.stop()
        scannerManager.startReading()
    }
}
