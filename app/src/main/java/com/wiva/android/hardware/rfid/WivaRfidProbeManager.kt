package com.wiva.android.hardware.rfid

import android.content.Context
import android.hardware.usb.UsbManager
import android_serialport_api.SerialPort
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.wiva.android.di.AppIoScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class WivaRfidProbeManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val logger: WivaRfidTrafficLogger,
    @AppIoScope private val appScope: CoroutineScope,
) {
    private var usbConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var usbPort: UsbSerialPort? = null
    private var ttySerialPort: SerialPort? = null
    private var ttyInput: InputStream? = null
    private var readJob: Job? = null
    private val currentPort = AtomicReference<String?>(null)
    private var rxBuffer = ByteArray(0)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    suspend fun connect(devicePath: String, baudRate: Int = DEFAULT_BAUD_RATE): Boolean =
        withContext(Dispatchers.IO) {
            disconnect()
            if (devicePath.isBlank()) return@withContext false
            val ok =
                when {
                    devicePath.isNativeTtyPath() -> openTty(devicePath, baudRate)
                    else -> openUsb(devicePath, baudRate)
                }
            if (ok) {
                currentPort.set(devicePath)
                rxBuffer = ByteArray(0)
                logger.logEvent(devicePath, "connected @ ${baudRate} baud")
            }
            ok
        }

    fun disconnect() {
        val port = currentPort.getAndSet(null)
        readJob?.cancel()
        readJob = null

        runCatching { usbPort?.close() }
        usbPort = null

        runCatching { usbConnection?.close() }
        usbConnection = null

        runCatching { ttySerialPort?.close() }
        ttySerialPort = null
        ttyInput = null

        if (_isConnected.value && port != null) {
            logger.logEvent(port, "disconnected")
        }
        _isConnected.value = false
        rxBuffer = ByteArray(0)
    }

    private suspend fun openUsb(devicePath: String, baudRate: Int): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = usbManager.deviceList.values.find { it.deviceName == devicePath }
        if (device == null) {
            logger.logEvent(devicePath, "USB device not found")
            return false
        }
        if (!usbManager.hasPermission(device)) {
            logger.logEvent(devicePath, "no USB permission")
            return false
        }

        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            logger.logEvent(devicePath, "USB serial driver not found")
            return false
        }

        val port = driver.ports.firstOrNull()
        if (port == null) {
            logger.logEvent(devicePath, "USB serial port missing")
            return false
        }

        val connection = usbManager.openDevice(device)
        if (connection == null) {
            logger.logEvent(devicePath, "USB openDevice failed")
            return false
        }

        return try {
            port.open(connection)
            port.setParameters(
                baudRate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            port.setDTR(true)
            port.setRTS(true)
            usbConnection = connection
            usbPort = port
            _isConnected.value = true
            startUsbReadLoop(devicePath, port)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "RFID USB open failed %s", devicePath)
            logger.logEvent(devicePath, "USB open failed: ${e.message ?: e.javaClass.simpleName}")
            runCatching { port.close() }
            connection.close()
            false
        }
    }

    private fun openTty(devicePath: String, baudRate: Int): Boolean {
        if (!File(devicePath).exists()) {
            logger.logEvent(devicePath, "TTY not found")
            return false
        }
        return try {
            val port = SerialPort(File(devicePath), baudRate, 1, 8, 0, 0, 0)
            ttySerialPort = port
            ttyInput = port.getInputStream()
            _isConnected.value = true
            startTtyReadLoop(devicePath, ttyInput ?: return false)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "RFID TTY open failed %s", devicePath)
            logger.logEvent(devicePath, "TTY open failed: ${e.message ?: e.javaClass.simpleName}")
            false
        }
    }

    private fun startUsbReadLoop(devicePath: String, port: UsbSerialPort) {
        readJob =
            appScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(4096)
                while (isActive && _isConnected.value) {
                    try {
                        val len = port.read(buffer, READ_TIMEOUT_MS)
                        if (len > 0) {
                            handleIncomingChunk(devicePath, buffer.copyOf(len))
                        }
                    } catch (e: Exception) {
                        logger.logEvent(devicePath, "read failed: ${e.message ?: e.javaClass.simpleName}")
                        break
                    }
                }
                _isConnected.value = false
            }
    }

    private fun startTtyReadLoop(devicePath: String, input: InputStream) {
        readJob =
            appScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(4096)
                while (isActive && _isConnected.value) {
                    try {
                        val len = input.read(buffer)
                        if (len > 0) {
                            handleIncomingChunk(devicePath, buffer.copyOf(len))
                        }
                    } catch (e: Exception) {
                        logger.logEvent(devicePath, "read failed: ${e.message ?: e.javaClass.simpleName}")
                        break
                    }
                }
                _isConnected.value = false
            }
    }

    private fun handleIncomingChunk(
        devicePath: String,
        chunk: ByteArray,
    ) {
 // Сохраняем сырые байты целиком: это основной режим "метода тыка".
        logger.logRx(devicePath, chunk)
        rxBuffer += chunk

 // Протокол из документации ридера:
 // STX(0x02) + 4 ASCII HEX символа карты + CR(0x0D) + LF(0x0A) + ETX(0x03)
 // Пример: 02 33 44 31 32 0D 0A 03 -> cardId=3D12
        while (rxBuffer.size >= RFID_UART_FRAME_SIZE) {
            val stxIndex = rxBuffer.indexOfFirst { it == RFID_STX }
            if (stxIndex < 0) {
 // Мусор без STX не нужен: удерживаем только хвост для следующего чанка.
                rxBuffer = rxBuffer.takeLast(MAX_TAIL_BYTES).toByteArray()
                return
            }
            if (stxIndex > 0) {
                rxBuffer = rxBuffer.copyOfRange(stxIndex, rxBuffer.size)
            }
            if (rxBuffer.size < RFID_UART_FRAME_SIZE) return

            val frame = rxBuffer.copyOfRange(0, RFID_UART_FRAME_SIZE)
            if (frame[0] == RFID_STX && frame[5] == RFID_CR && frame[6] == RFID_LF && frame[7] == RFID_ETX) {
                val cardId = frame.copyOfRange(1, 5).decodeToString()
                if (cardId.length == 4 && cardId.all { it.isHexDigit() }) {
                    logger.logEvent(
                        devicePath,
                        "cardId=$cardId (UART frame: 02 + 4 ASCII HEX + 0D 0A + 03)",
                    )
                } else {
                    logger.logEvent(devicePath, "frame parsed, non-hex payload='$cardId'")
                }
                rxBuffer = rxBuffer.copyOfRange(RFID_UART_FRAME_SIZE, rxBuffer.size)
            } else {
 // Если заголовок найден, но кадр не совпал с ожидаемым форматом — двигаемся на 1 байт.
                rxBuffer = rxBuffer.copyOfRange(1, rxBuffer.size)
            }
        }
    }

    companion object {
        private const val TAG = "WivaRfidProbe"
        private const val READ_TIMEOUT_MS = 250
        private const val DEFAULT_BAUD_RATE = 9600
        private const val RFID_UART_FRAME_SIZE = 8
        private const val MAX_TAIL_BYTES = 32
        private const val RFID_STX: Byte = 0x02
        private const val RFID_CR: Byte = 0x0D
        private const val RFID_LF: Byte = 0x0A
        private const val RFID_ETX: Byte = 0x03
    }
}

private fun String.isNativeTtyPath(): Boolean =
    startsWith("/dev/ttyS") || startsWith("/dev/ttyUSB") || startsWith("/dev/ttyACM")

private fun Char.isHexDigit(): Boolean =
    (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')
