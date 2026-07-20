package com.viwa.android.ui.screens.service.devices

import com.viwa.android.hardware.devices.ViwaControllerManualPortProbe
import com.viwa.android.hardware.devices.ViwaControllerPortScanPort
import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.ViwaSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ViwaControllerPortScanController(
    private val scanner: ViwaControllerPortScanPort,
    private val manualProbe: ViwaControllerManualPortProbe,
    private val serialPort: ViwaSerialPort,
    private val labels: ViwaDevicesUiLabels,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViwaControllerPortScanActions {
    private val _state = MutableStateFlow(ViwaControllerPortScanState())
    val state: StateFlow<ViwaControllerPortScanState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var manualJob: Job? = null

    init {
        refreshPorts()
    }

    override fun refreshPorts() {
        scope.launch { refreshPortRows(_state.value.results.associate { it.deviceName to it.type }) }
    }

    override fun cancelAllProbes() {
        scanJob?.cancel()
        scanJob = null
        manualJob?.cancel()
        manualJob = null
        _state.update { it.copy(isRunning = false, isManualRunning = false) }
    }

    override fun selectPort(devicePath: String) {
        _state.update { it.copy(selectedPort = devicePath) }
    }

    override fun probePrimaryVersionManual() {
        startManualProbe { path, onLog -> manualProbe.probePrimaryVersion(path, onLog) }
    }

    override fun probeConnectedPrimaryVersionManual() {
        manualJob?.cancel()
        manualJob =
            scope.launch {
                runManualProbe("connected") { onLog -> manualProbe.probeConnectedPrimaryVersion(onLog) }
            }
    }

    private fun startManualProbe(
        block: suspend (String, suspend (ViwaPortScanLogEntry) -> Unit) -> Unit,
    ) {
        val path = _state.value.selectedPort ?: return
        scanJob?.cancel()
        scanJob = null
        manualJob?.cancel()
        manualJob =
            scope.launch {
                runManualProbe(path) { onLog -> block(path, onLog) }
            }
    }

    private suspend fun runManualProbe(
        path: String,
        block: suspend (suspend (ViwaPortScanLogEntry) -> Unit) -> Unit,
    ) {
        try {
            _state.update {
                it.copy(isRunning = false, isManualRunning = true, manualLogEntries = emptyList())
            }
            val logBuffer = ViwaPortScanLogBuffer()
            runCatching {
                block { entry -> publishManualLog(logBuffer, entry) }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) return@onFailure
                publishManualLog(
                    logBuffer,
                    ViwaPortScanLogEntry(
                        kind = ViwaPortScanLogKind.Info,
                        port = path,
                        protocol = ViwaPortScanProtocol.Primary,
                        payload = error.message ?: error::class.java.simpleName,
                    ),
                    force = true,
                )
            }
            _state.update { current -> current.copy(manualLogEntries = logBuffer.flush()) }
        } finally {
            _state.update { it.copy(isManualRunning = false) }
        }
    }

    override fun startScan() {
        if (_state.value.isRunning) return
        manualJob?.cancel()
        manualJob = null
        scanJob?.cancel()
        scanJob =
            scope.launch {
                try {
                    _state.update {
                        it.copy(
                            isRunning = true,
                            isManualRunning = false,
                            statusMessage = labels.controllerScanStarted,
                            logEntries = emptyList(),
                            results = emptyList(),
                        )
                    }
                    refreshPortRows(emptyMap())
                    runCatching {
                        val logBuffer = ViwaPortScanLogBuffer()
                        val results =
                            scanner.scan(
                                onProgress = { progress ->
                                    _state.update { current ->
                                        current.copy(
                                            statusMessage = formatProgress(progress),
                                            logEntries = logBuffer.flush(),
                                        )
                                    }
                                },
                                onLog = { entry -> publishScanLog(logBuffer, entry) },
                            )
                        applyDiscoveredAssignments(results, logBuffer)
                        val scanResults = results.associate { it.deviceName to it.type }
                        refreshPortRows(scanResults)
                        _state.update {
                            it.copy(statusMessage = formatComplete(results), results = results)
                        }
                    }.onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) return@onFailure
                        _state.update {
                            it.copy(
                                statusMessage =
                                    labels.controllerScanFailed.format(
                                        error.message ?: error::class.java.simpleName,
                                    ),
                            )
                        }
                    }
                } finally {
                    _state.update { it.copy(isRunning = false) }
                }
            }
    }

    private suspend fun applyDiscoveredAssignments(
        results: List<ViwaControllerPortScanResult>,
        logBuffer: ViwaPortScanLogBuffer,
    ) {
        val primaryPath =
            results.firstOrNull { it.type == ViwaControllerPortScanType.Primary }?.deviceName
        if (primaryPath != null) {
            serialPort.assign(primaryPath, PortRole.CONTROLLER)
                .onSuccess {
                    publishScanLog(
                        logBuffer,
                        ViwaPortScanLogEntry(
                            kind = ViwaPortScanLogKind.Info,
                            port = primaryPath,
                            protocol = ViwaPortScanProtocol.Primary,
                            payload = "assigned controller",
                        ),
                        force = true,
                    )
                }
        }
        _state.update { current -> current.copy(logEntries = logBuffer.flush()) }
    }

    private fun publishScanLog(
        logBuffer: ViwaPortScanLogBuffer,
        entry: ViwaPortScanLogEntry,
        force: Boolean = false,
    ) {
        val decision =
            if (force) {
                logBuffer.append(entry)
                ViwaPortScanLogBuffer.EmitDecision.Emit(logBuffer.flush())
            } else {
                logBuffer.append(entry)
            }
        if (decision is ViwaPortScanLogBuffer.EmitDecision.Emit) {
            _state.update { current -> current.copy(logEntries = decision.entries) }
        }
    }

    private fun publishManualLog(
        logBuffer: ViwaPortScanLogBuffer,
        entry: ViwaPortScanLogEntry,
        force: Boolean = false,
    ) {
        val decision =
            if (force) {
                logBuffer.append(entry)
                ViwaPortScanLogBuffer.EmitDecision.Emit(logBuffer.flush())
            } else {
                logBuffer.append(entry)
            }
        if (decision is ViwaPortScanLogBuffer.EmitDecision.Emit) {
            _state.update { current -> current.copy(manualLogEntries = decision.entries) }
        }
    }

    private suspend fun refreshPortRows(scanResults: Map<String, ViwaControllerPortScanType>) {
        val devices = serialPort.availableDevices()
        val portRows =
            ViwaPortUsageMapper.mapRows(
                devices = devices,
                assignments = serialPort.assignments(),
                controllerPath = serialPort.controllerDevicePath(),
                scannerPath = serialPort.assignedDeviceName(PortRole.SCANNER),
                paymentPath = serialPort.assignedDeviceName(PortRole.PAYMENT),
                scanResults = scanResults,
                labels = labels,
            )
        val paths = devices.map { it.deviceName }.sorted()
        val selected = _state.value.selectedPort?.takeIf { it in paths } ?: paths.firstOrNull()
        _state.update {
            it.copy(portRows = portRows, availablePorts = paths, selectedPort = selected)
        }
    }

    private fun formatProgress(progress: ViwaControllerPortScanProgress): String =
        when (progress) {
            ViwaControllerPortScanProgress.Started -> labels.controllerScanStarted
            ViwaControllerPortScanProgress.NoPorts -> labels.controllerScanNoPorts
            is ViwaControllerPortScanProgress.ProbingPrimary ->
                labels.controllerScanProbingPrimary.format(progress.deviceName)
            is ViwaControllerPortScanProgress.PrimaryFound ->
                labels.controllerScanPrimaryFound.format(progress.deviceName)
            is ViwaControllerPortScanProgress.NotFound ->
                labels.controllerScanNotFound.format(progress.deviceName)
        }

    private fun formatComplete(results: List<ViwaControllerPortScanResult>): String {
        if (results.isEmpty()) return labels.controllerScanNoPorts
        val summary =
            results.joinToString(separator = "\n") { result ->
                val typeLabel =
                    when (result.type) {
                        ViwaControllerPortScanType.Primary -> labels.controllerScanTypePrimary
                        ViwaControllerPortScanType.None -> labels.controllerScanTypeNone
                    }
                "${result.deviceName}: $typeLabel"
            }
        return "${labels.controllerScanComplete}\n$summary"
    }
}
