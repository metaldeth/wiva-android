package com.viwa.android.hardware.serial

import android.content.Context
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class SerialPortManager
@Inject
constructor(
    private val usbSerialManager: UsbSerialManager,
    private val configRepository: ConfigRepository,
    @ApplicationContext
    @Suppress("unused")
    private val context: Context,
) {
    private val _scannerDiscoveryFlow =
        MutableSharedFlow<List<UsbSerialDriver>>(replay = 0, extraBufferCapacity = 8)
    val scannerDiscoveryFlow: SharedFlow<List<UsbSerialDriver>> = _scannerDiscoveryFlow.asSharedFlow()

    private var scannerDiscoveryJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getAvailablePorts(): List<UsbSerialDriver> = usbSerialManager.getAvailableDevices()

    suspend fun getPortAssignments(): Map<String, PortRole> {
        val json = configRepository.getJson(JsonStoreKeys.PORT_ASSIGNMENTS) ?: return emptyMap()
        return runCatching {
            Json.decodeFromString<Map<String, String>>(json)
                .mapValues { PortRole.valueOf(it.value) }
        }.getOrDefault(emptyMap())
    }

    suspend fun setPortAssignment(deviceName: String, role: PortRole) {
        val current = getPortAssignments().toMutableMap()
        current[deviceName] = role
        val encoded = Json.encodeToString(current.mapValues { it.value.name })
        configRepository.setJson(JsonStoreKeys.PORT_ASSIGNMENTS, encoded)
    }

    fun startScannerDiscovery(intervalMs: Long = 2000) {
        scannerDiscoveryJob?.cancel()
        scannerDiscoveryJob =
            scope.launch {
                while (isActive) {
                    val drivers = usbSerialManager.getAvailableDevices()
                    _scannerDiscoveryFlow.emit(drivers)
                    delay(intervalMs)
                }
            }
    }

    fun stopScannerDiscovery() {
        scannerDiscoveryJob?.cancel()
        scannerDiscoveryJob = null
    }
}
