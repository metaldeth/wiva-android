package com.viwa.android.data.payment.aqsi.serial

import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialPort

/** Low-level AQSI USB serial read/write session (legacy kiosk parity). */
class AqsiSerialSession(
    private val port: UsbSerialPort,
    private val config: AqsiSerialConfig = AqsiSerialConfig(),
) : AqsiSerialLink {
    private val readBuffer = ByteArray(4096)

    fun open(usbConnection: UsbDeviceConnection) {
        port.open(usbConnection)
        port.setParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
        port.setDTR(true)
        port.setRTS(true)
    }

    override fun read(timeoutMs: Int): ByteArray? {
        val length = port.read(readBuffer, timeoutMs)
        return if (length > 0) readBuffer.copyOf(length) else null
    }

    override fun write(data: ByteArray, timeoutMs: Int) {
        port.write(data, timeoutMs)
    }

    override fun close() {
        runCatching { port.close() }
    }

    override val isOpen: Boolean
        get() = port.isOpen
}
