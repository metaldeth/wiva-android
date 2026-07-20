package com.viwa.android.hardware.serial

/** Minimal serial port facade for AQSI Pill USB assignment (PAYMENT role). */
interface PaymentSerialPort {
    suspend fun availableDevices(): List<PaymentSerialDeviceInfo>

    suspend fun assignments(): Map<String, PortRole>

    suspend fun assign(deviceName: String, role: PortRole): Result<Unit>

    suspend fun assignedDeviceName(role: PortRole): String?
}
