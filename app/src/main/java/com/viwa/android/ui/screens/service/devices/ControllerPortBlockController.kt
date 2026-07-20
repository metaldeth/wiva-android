package com.viwa.android.ui.screens.service.devices

import com.viwa.android.hardware.controller.ConnectToPortResult
import com.viwa.android.hardware.controller.ControllerHardwareManager
import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.SerialDeviceInfo
import com.viwa.android.hardware.serial.SerialPortAssignmentEvents
import com.viwa.android.hardware.serial.ViwaSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ControllerPortBlockState(
    val availablePorts: List<SerialDeviceInfo> = emptyList(),
    val selectedPort: String? = null,
    val assignedPort: String? = null,
    val isPhysicalConnected: Boolean = false,
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val banner: String? = null,
    val bannerIsError: Boolean = false,
)

interface ControllerPortBlockActions {
    fun refresh()

    fun selectPort(deviceName: String)

    fun connectSelected()
}

class ControllerPortBlockController(
    private val serialPort: ViwaSerialPort,
    private val controllerHardware: ControllerHardwareManager,
    private val assignmentEvents: SerialPortAssignmentEvents,
    private val scope: CoroutineScope,
) : ControllerPortBlockActions {
    private val _state = MutableStateFlow(ControllerPortBlockState())
    val state: StateFlow<ControllerPortBlockState> = _state.asStateFlow()

    init {
        scope.launch {
            controllerHardware.isPhysicalControllerConnected.collect { connected ->
                _state.update { it.copy(isPhysicalConnected = connected) }
            }
        }
        refresh()
    }

    override fun refresh() {
        scope.launch {
            _state.update { it.copy(isLoading = true, banner = null, bannerIsError = false) }
            runCatching {
                val devices = serialPort.availableDevices()
                val assigned = serialPort.controllerDevicePath()
                val selected =
                    _state.value.selectedPort?.takeIf { path -> devices.any { it.deviceName == path } }
                        ?: assigned?.takeIf { path -> devices.any { it.deviceName == path } }
                        ?: devices.firstOrNull()?.deviceName
                _state.update {
                    it.copy(
                        availablePorts = devices,
                        assignedPort = assigned,
                        selectedPort = selected,
                        isPhysicalConnected = controllerHardware.isPhysicalControllerConnected.value,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        banner = error.message ?: "Не удалось обновить список портов",
                        bannerIsError = true,
                    )
                }
            }
        }
    }

    override fun selectPort(deviceName: String) {
        _state.update { it.copy(selectedPort = deviceName, banner = null, bannerIsError = false) }
    }

    override fun connectSelected() {
        val path = _state.value.selectedPort?.trim().orEmpty()
        if (path.isEmpty()) {
            _state.update {
                it.copy(banner = "Выберите serial-порт контроллера", bannerIsError = true)
            }
            return
        }
        scope.launch {
            _state.update { it.copy(isConnecting = true, banner = null, bannerIsError = false) }
            runCatching {
                serialPort.assign(path, PortRole.CONTROLLER)
                    .getOrThrow()
                val result = controllerHardware.connectToPort(path)
                if (!result.success) {
                    throw IllegalStateException(formatConnectFailure(result))
                }
                assignmentEvents.notifyChanged()
                _state.update {
                    it.copy(
                        assignedPort = path,
                        isPhysicalConnected = controllerHardware.isPhysicalControllerConnected.value,
                        isConnecting = false,
                        banner = "Контроллер подключён: $path",
                        bannerIsError = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isConnecting = false,
                        banner = error.message ?: "Не удалось подключить контроллер",
                        bannerIsError = true,
                    )
                }
            }
        }
    }

    private fun formatConnectFailure(result: ConnectToPortResult): String {
        val step = result.failedStep?.name ?: "UNKNOWN"
        return "Не удалось открыть порт (step=$step)"
    }
}
