package com.viwa.android.data.payment.aqsi.serial

import com.hoho.android.usbserial.driver.UsbSerialPort

data class AqsiSerialConfig(
    val baudRate: Int = 9600,
    val dataBits: Int = UsbSerialPort.DATABITS_8,
    val stopBits: Int = UsbSerialPort.STOPBITS_1,
    val parity: Int = UsbSerialPort.PARITY_NONE,
)
