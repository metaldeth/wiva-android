package com.viwa.android.ui.screens.service.devices

/** Русские подписи для вкладки «Порты» (viwa, без snack). */
data class ViwaDevicesUiLabels(
    val controllerScanButton: String = "Сканировать порты контроллера",
    val controllerScanStarted: String = "Сканирование портов…",
    val controllerScanNoPorts: String = "Serial-порты не обнаружены",
    val controllerScanProbingPrimary: String = "Проверка контроллера: %1\$s",
    val controllerScanPrimaryFound: String = "Контроллер найден: %1\$s",
    val controllerScanNotFound: String = "Не контроллер: %1\$s",
    val controllerScanComplete: String = "Сканирование завершено",
    val controllerScanFailed: String = "Ошибка сканирования: %1\$s",
    val controllerScanTypePrimary: String = "Контроллер",
    val controllerScanTypeNone: String = "—",
    val portsTableHeaderPath: String = "Порт",
    val portsTableHeaderUsage: String = "Назначение",
    val portsTableHeaderScanResult: String = "Результат",
    val portsLogTitle: String = "Лог сканирования",
    val portsManualTitle: String = "Ручная проверка порта",
    val portsManualSelectPort: String = "Выберите порт",
    val portsManualPrimaryVersion: String = "ReadFirmwareVersion",
    val portsManualConnectedPrimaryVersion: String = "Текущий контроллер",
    val portsManualLogTitle: String = "Лог ручной проверки",
    val portUsagePrimary: String = "Контроллер",
    val portUsageScanner: String = "Сканер",
    val portUsagePayment: String = "Платёжник",
    val portUsageUnassigned: String = "Свободен",
    val portScanResultPending: String = "—",
    val portsLogEmpty: String = "Лог пуст",
    val portScanLogKindTx: String = "TX",
    val portScanLogKindRx: String = "RX",
    val portScanLogKindInfo: String = "INFO",
    val portScanProtocolPrimary: String = "CTRL",
)

object ViwaPortUsageMapper {
    fun mapRows(
        devices: List<com.viwa.android.hardware.serial.SerialDeviceInfo>,
        assignments: Map<String, com.viwa.android.hardware.serial.PortRole>,
        controllerPath: String?,
        scannerPath: String?,
        paymentPath: String?,
        scanResults: Map<String, ViwaControllerPortScanType>,
        labels: ViwaDevicesUiLabels,
    ): List<ViwaPortUsageRow> {
        return devices
            .sortedWith(compareBy({ it.vendorId }, { it.productId }, { it.deviceName }))
            .map { device ->
                val path = device.deviceName
                ViwaPortUsageRow(
                    devicePath = path,
                    currentUsage =
                        resolveUsage(
                            path = path,
                            assignments = assignments,
                            controllerPath = controllerPath,
                            scannerPath = scannerPath,
                            paymentPath = paymentPath,
                            labels = labels,
                        ),
                    lastScanResult =
                        scanResults[path]?.let { type ->
                            when (type) {
                                ViwaControllerPortScanType.Primary -> labels.controllerScanTypePrimary
                                ViwaControllerPortScanType.None -> labels.controllerScanTypeNone
                            }
                        } ?: labels.portScanResultPending,
                )
            }
    }

    private fun resolveUsage(
        path: String,
        assignments: Map<String, com.viwa.android.hardware.serial.PortRole>,
        controllerPath: String?,
        scannerPath: String?,
        paymentPath: String?,
        labels: ViwaDevicesUiLabels,
    ): String {
        when (assignments[path]) {
            com.viwa.android.hardware.serial.PortRole.CONTROLLER -> return labels.portUsagePrimary
            com.viwa.android.hardware.serial.PortRole.SCANNER -> return labels.portUsageScanner
            com.viwa.android.hardware.serial.PortRole.PAYMENT -> return labels.portUsagePayment
            else -> Unit
        }
        if (path == controllerPath) return labels.portUsagePrimary
        if (path == scannerPath) return labels.portUsageScanner
        if (path == paymentPath) return labels.portUsagePayment
        return labels.portUsageUnassigned
    }
}

data class ViwaPortUsageRow(
    val devicePath: String,
    val currentUsage: String,
    val lastScanResult: String,
)

enum class ViwaControllerPortScanType {
    Primary,
    None,
}

data class ViwaControllerPortScanResult(
    val deviceName: String,
    val type: ViwaControllerPortScanType,
)

sealed interface ViwaControllerPortScanProgress {
    data object Started : ViwaControllerPortScanProgress

    data object NoPorts : ViwaControllerPortScanProgress

    data class ProbingPrimary(val deviceName: String) : ViwaControllerPortScanProgress

    data class PrimaryFound(val deviceName: String) : ViwaControllerPortScanProgress

    data class NotFound(val deviceName: String) : ViwaControllerPortScanProgress
}

enum class ViwaPortScanLogKind {
    Tx,
    Rx,
    Info,
}

enum class ViwaPortScanProtocol {
    Primary,
}

data class ViwaPortScanLogEntry(
    val kind: ViwaPortScanLogKind,
    val port: String,
    val protocol: ViwaPortScanProtocol,
    val payload: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class ViwaControllerPortScanState(
    val isRunning: Boolean = false,
    val statusMessage: String? = null,
    val portRows: List<ViwaPortUsageRow> = emptyList(),
    val logEntries: List<ViwaPortScanLogEntry> = emptyList(),
    val results: List<ViwaControllerPortScanResult> = emptyList(),
    val availablePorts: List<String> = emptyList(),
    val selectedPort: String? = null,
    val manualLogEntries: List<ViwaPortScanLogEntry> = emptyList(),
    val isManualRunning: Boolean = false,
)

interface ViwaControllerPortScanActions {
    fun refreshPorts()

    fun cancelAllProbes()

    fun startScan()

    fun selectPort(devicePath: String)

    fun probePrimaryVersionManual()

    fun probeConnectedPrimaryVersionManual()
}
