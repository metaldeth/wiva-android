package com.viwa.android.hardware.serial

/** Единый serial-порт для UI «Устройства» и AQSI PAYMENT role. */
interface ViwaSerialPort {
    suspend fun availableDevices(): List<SerialDeviceInfo>

    suspend fun assignments(): Map<String, PortRole>

    suspend fun assign(deviceName: String, role: PortRole): Result<Unit>

    suspend fun assignedDeviceName(role: PortRole): String?

    suspend fun probeOpen(deviceName: String): Result<Unit>

    suspend fun controllerDevicePath(): String?
}
