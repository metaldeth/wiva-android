package com.wiva.android.hardware.controller

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * USB CDC/serial через [usb-serial-for-android]. [ControllerPortSettings.devicePath] —
 * [UsbDevice.getDeviceName] (например `/dev/bus/usb/001/005`).
 */
class UsbSerialControllerTransport(
    private val context: Context,
    private val ioScope: CoroutineScope,
) : ControllerSerialTransport {
    private var usbConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var serialPort: UsbSerialPort? = null
    private var readJob: Job? = null
    private var rxListener: ((ByteArray) -> Unit)? = null

    override var isOpen: Boolean = false
        private set

    override suspend fun open(settings: ControllerPortSettings): Boolean =
        withContext(Dispatchers.IO) {
            close()
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val device =
                usbManager.deviceList.values.find { it.deviceName == settings.devicePath }
            if (device == null) {
                Timber.tag(TAG).w("USB: устройство не найдено: %s", settings.devicePath)
                return@withContext false
            }
            if (!usbManager.hasPermission(device)) {
                Timber.tag(TAG).w("USB: нет разрешения для %s", settings.devicePath)
                return@withContext false
            }
            val connection = usbManager.openDevice(device) ?: return@withContext false
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            if (driver == null) {
                connection.close()
                Timber.tag(TAG).w("USB: драйвер не найден для %s", settings.devicePath)
                return@withContext false
            }
            val port = driver.ports.getOrNull(0)
            if (port == null) {
                connection.close()
                return@withContext false
            }
            try {
                port.open(connection)
                port.setParameters(
                    settings.baudRate,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE,
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "USB: open/setParameters failed")
                try {
                    port.close()
                } catch (_: Exception) {
                }
                connection.close()
                return@withContext false
            }
            usbConnection = connection
            serialPort = port
            isOpen = true
            Timber.tag(TAG).i("USB serial open %s %d baud", settings.devicePath, settings.baudRate)

            readJob =
                ioScope.launch(Dispatchers.IO) {
                    val buf = ByteArray(4096)
                    val listener = rxListener
                    while (isActive && isOpen) {
                        try {
                            val n = port.read(buf, 250)
                            if (n > 0) {
                                listener?.invoke(buf.copyOf(n))
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            true
        }

    override fun close() {
        readJob?.cancel()
        readJob = null
        try {
            serialPort?.close()
        } catch (_: Exception) {
        }
        serialPort = null
        try {
            usbConnection?.close()
        } catch (_: Exception) {
        }
        usbConnection = null
        if (isOpen) {
            Timber.tag(TAG).i("USB serial close")
        }
        isOpen = false
    }

    override suspend fun write(bytes: ByteArray) {
        val port = serialPort ?: error("USB serial not open")
        withContext(Dispatchers.IO) {
            port.write(bytes, 2000)
        }
    }

    override fun setOnBytesReceived(listener: ((ByteArray) -> Unit)?) {
        rxListener = listener
    }

    companion object {
        private const val TAG = "WivaController"
    }
}
