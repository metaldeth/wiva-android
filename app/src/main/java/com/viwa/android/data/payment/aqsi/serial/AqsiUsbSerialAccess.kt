package com.viwa.android.data.payment.aqsi.serial

import com.hoho.android.usbserial.driver.UsbSerialDriver

/** USB serial access for AQSI Arcus2 (mockable in unit tests). */
interface AqsiUsbSerialAccess {
    fun getAvailableDevices(): List<UsbSerialDriver>

    fun openConnection(
        driver: UsbSerialDriver,
        portIndex: Int = 0,
        config: AqsiSerialConfig = AqsiSerialConfig(),
    ): Pair<AqsiSerialLink, android.hardware.usb.UsbDeviceConnection>?
}
