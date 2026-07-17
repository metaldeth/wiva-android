package com.wiva.android.hardware.serial

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class UsbSerialManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    fun getAvailableDevices(): List<UsbSerialDriver> {
        val mgr = usbManager ?: return emptyList()
        return try {
            UsbSerialProber.getDefaultProber().findAllDrivers(mgr)
        } catch (e: Exception) {
            Timber.e(e, "getAvailableDevices failed")
            emptyList()
        }
    }

    fun openConnection(driver: UsbSerialDriver, portIndex: Int = 0): Pair<SerialConnection, UsbDeviceConnection>? {
        val mgr = usbManager ?: return null
        val usbConnection = mgr.openDevice(driver.device) ?: return null
        val port = driver.ports[portIndex]
        val connection = SerialConnection(port)
        return try {
            connection.open(usbConnection)
            Pair(connection, usbConnection)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open serial port")
            usbConnection.close()
            null
        }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager?.hasPermission(device) == true

    fun requestPermission(device: UsbDevice, pendingIntent: PendingIntent) {
        usbManager?.requestPermission(device, pendingIntent)
    }
}
