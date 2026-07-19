package com.viwa.android.hardware.serial

import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialPort

class SerialConnection(
    private val port: UsbSerialPort,
    private val config: SerialConfig = SerialConfig(),
) {
    private val readBuffer = ByteArray(4096)

    fun open(usbConnection: UsbDeviceConnection) {
        port.open(usbConnection)
        port.setParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
        port.setDTR(true)
        port.setRTS(true)
    }

    fun read(timeoutMs: Int = 100): ByteArray? {
        val len = port.read(readBuffer, timeoutMs)
        return if (len > 0) readBuffer.copyOf(len) else null
    }

    fun write(data: ByteArray, timeoutMs: Int = 200) {
        port.write(data, timeoutMs)
    }

    fun close() {
        runCatching { port.close() }
    }

    val isOpen: Boolean get() = port.isOpen
}
