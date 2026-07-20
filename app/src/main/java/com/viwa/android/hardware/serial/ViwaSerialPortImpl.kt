package com.viwa.android.hardware.serial

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ViwaSerialPortImpl
@Inject
constructor(
    private val usbSerialManager: UsbSerialManager,
    private val serialPortManager: SerialPortManager,
    private val configRepository: ConfigRepository,
    private val assignmentEvents: SerialPortAssignmentEvents,
) : ViwaSerialPort {

    override suspend fun availableDevices(): List<SerialDeviceInfo> =
        withContext(Dispatchers.IO) {
            usbSerialManager.enumerateSerialDevices()
        }

    override suspend fun assignments(): Map<String, PortRole> =
        serialPortManager.getPortAssignments().mapValues { (_, role) ->
            role.fromAssignmentStorage()
        }

    override suspend fun assign(deviceName: String, role: PortRole): Result<Unit> =
        runCatching {
            val stored = role.toAssignmentStorage()
            val current = serialPortManager.getPortAssignments().toMutableMap()
            if (stored == PortRole.UNASSIGNED) {
                current.remove(deviceName)
                if (configRepository.get(JsonStoreKeys.CONTROLLER_USB_DEVICE_PATH) == deviceName) {
                    configRepository.set(JsonStoreKeys.CONTROLLER_USB_DEVICE_PATH, "")
                }
            } else {
                current.entries.removeAll { it.value == stored && it.key != deviceName }
                current[deviceName] = stored
                if (role == PortRole.CONTROLLER) {
                    configRepository.set(JsonStoreKeys.CONTROLLER_USB_DEVICE_PATH, deviceName)
                }
            }
            serialPortManager.replacePortAssignments(current)
            assignmentEvents.notifyChanged()
        }

    override suspend fun assignedDeviceName(role: PortRole): String? {
        val normalized =
            when (role) {
                PortRole.UNKNOWN -> PortRole.UNASSIGNED
                else -> role.toAssignmentStorage()
            }
        val fromAssignments =
            serialPortManager.getPortAssignments().entries
                .firstOrNull { it.value == normalized || it.value == role }
                ?.key
        if (fromAssignments != null) return fromAssignments
        if (role == PortRole.CONTROLLER) {
            return controllerDevicePath()
        }
        return null
    }

    override suspend fun probeOpen(deviceName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (deviceName.startsWith("/dev/tty")) {
                return@withContext Result.success(Unit)
            }
            val driver =
                usbSerialManager.getAvailableDevices()
                    .firstOrNull { it.device.deviceName == deviceName }
            if (driver == null) {
                val connected =
                    usbSerialManager.getConnectedUsbDevices()
                        .any { it.deviceName == deviceName }
                return@withContext if (connected) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Устройство не найдено"))
                }
            }
            if (!usbSerialManager.hasPermission(driver.device)) {
                return@withContext Result.failure(IllegalStateException("Нет разрешения USB"))
            }
            val opened =
                usbSerialManager.openConnection(driver)
                    ?: return@withContext Result.failure(IllegalStateException("Не удалось открыть порт"))
            opened.first.close()
            opened.second.close()
            Result.success(Unit)
        }

    override suspend fun controllerDevicePath(): String? {
        val assigned = assignedDeviceName(PortRole.CONTROLLER)
        if (!assigned.isNullOrBlank()) return assigned
        return configRepository.get(JsonStoreKeys.CONTROLLER_USB_DEVICE_PATH)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
