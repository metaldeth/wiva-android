package com.viwa.android.ui.screens.service.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.ui.system.DialogWindowImmersiveSideEffect
import com.viwa.android.hardware.scanner.ScannerTrafficEntry
import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.ui.screens.service.ServiceUiState
import com.viwa.android.ui.screens.service.ServiceViewModel
import com.viwa.android.ui.screens.service.serviceMenuSp
import com.viwa.android.ui.screens.service.SettingsColumn
import com.viwa.android.ui.screens.service.UsbSerialDriverInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SCANNER_TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
@Composable
fun ViwaEquipmentScannerTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    DisposableEffect(Unit) {
        viewModel.onEquipmentScannerTabVisible()
        onDispose { viewModel.onEquipmentScannerTabHidden() }
    }

    var showScannerDiscovery by remember { mutableStateOf(false) }
    val scannerLogEntries by viewModel.scannerTrafficFlow.collectAsStateWithLifecycle()
    val scannerLogReversed = remember(scannerLogEntries) { scannerLogEntries.asReversed() }
    val scannerExpandedIds = remember { mutableStateMapOf<Int, Boolean>() }

    if (showScannerDiscovery) {
        ViwaFindScannerDialog(
            drivers = state.scannerDiscoveryResults,
            isRunning = state.isScannerDiscoveryRunning,
            onStart = { viewModel.startScannerDiscovery() },
            onStop = { viewModel.stopScannerDiscovery() },
            onSelect = { deviceName ->
                viewModel.setScannerPortRole(deviceName, PortRole.SCANNER)
                viewModel.stopScannerDiscovery()
                showScannerDiscovery = false
            },
            onDismiss = {
                viewModel.stopScannerDiscovery()
                showScannerDiscovery = false
            },
        )
    }

    SettingsColumn {
            Text("Сканер (USB serial)", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
 "Как в устройства CDC из usb-serial-for-android. Назначьте роль «SCANNER» порту " +
                    "или оставьте первое доступное устройство. Порт контроллера — отдельно (вкладка «Контроллер»); " +
                    "не используйте один и тот же CDC-порт для двух ролей.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (state.scannerAvailablePorts.isEmpty()) {
                Text(
                    "USB serial не обнаружен (нет CDC / нет прав).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.scannerAvailablePorts, key = { it.deviceName }) { port ->
                        ViwaScannerPortRow(
                            port = port,
                            role = state.scannerPortAssignments[port.deviceName] ?: PortRole.UNASSIGNED,
                            onRoleChange = { r -> viewModel.setScannerPortRole(port.deviceName, r) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showScannerDiscovery = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Найти сканер")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.loadScannerPorts() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Обновить список")
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Лог сканера (до 50)", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = {
                        viewModel.clearScannerTrafficLog()
                        scannerExpandedIds.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Очистить")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Свёрнутая строка — классификация; по нажатию — сырая строка с устройства.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (scannerLogReversed.isEmpty()) {
                    Text(
                        "Пока нет сканирований. Откройте вкладку и отсканируйте — записи появятся здесь.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    scannerLogReversed.forEach { entry ->
                        val ex = scannerExpandedIds[entry.id] == true
                        ScannerTrafficRow(
                            entry = entry,
                            expanded = ex,
                            onToggle = { scannerExpandedIds[entry.id] = !ex },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
    }
}

@Composable
private fun ScannerTrafficRow(
    entry: ScannerTrafficEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scannerAccent = scheme.tertiary
    val bg = scheme.tertiaryContainer.copy(alpha = if (expanded) 0.95f else 0.72f)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SCAN",
                    color = scannerAccent,
                    fontSize = serviceMenuSp(10),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier =
                        Modifier
                            .background(scannerAccent.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    SCANNER_TIME_FORMAT.format(Date(entry.timestampMs)),
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.classificationLabel,
                    fontSize = serviceMenuSp(12),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = scannerAccent,
                    modifier = Modifier.weight(1f),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = scannerAccent.copy(alpha = 0.25f))
                Spacer(Modifier.height(6.dp))
                Text(
                    "Строка: ${entry.rawLine}",
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ViwaScannerPortRow(
    port: UsbSerialDriverInfo,
    role: PortRole,
    onRoleChange: (PortRole) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(port.deviceName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "VID: ${port.vendorId.toString(16)} PID: ${port.productId.toString(16)} | ${port.driverType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PortRole.entries.forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = { onRoleChange(r) },
                        label = { Text(r.name, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViwaFindScannerDialog(
    drivers: List<UsbSerialDriverInfo>,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        DialogWindowImmersiveSideEffect()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Поиск сканера", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                if (drivers.isEmpty()) {
                    Text("Нет USB serial устройств", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(drivers) { d ->
                            TextButton(
                                onClick = { onSelect(d.deviceName) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(d.deviceName)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isRunning) {
                        Button(onClick = onStart) { Text("Начать поиск") }
                    } else {
                        Button(
                            onClick = onStop,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("Остановить")
                        }
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                }
            }
        }
    }
}
