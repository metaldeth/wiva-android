package com.viwa.android.hardware.serial

import com.hoho.android.usbserial.driver.UsbSerialPort

data class SerialConfig(
    val baudRate: Int = 9600,
    val dataBits: Int = UsbSerialPort.DATABITS_8,
    val stopBits: Int = UsbSerialPort.STOPBITS_1,
    val parity: Int = UsbSerialPort.PARITY_NONE,
)
