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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.hardware.rfid.RfidTrafficEntry
import com.viwa.android.ui.screens.service.ServiceUiState
import com.viwa.android.ui.screens.service.ServiceViewModel
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun ViwaEquipmentRfidTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    val selectedPort by viewModel.rfidSelectedUsbPath.collectAsStateWithLifecycle()
    val entries by viewModel.rfidTrafficFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }

    DisposableEffect(Unit) {
        viewModel.onEquipmentRfidTabVisible()
        onDispose {}
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    SettingsColumn {
        Text("RFID-ридер (UART / USB serial)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Выберите порт, подключите модуль и прикладывайте карты. Внизу показываются сырые RX-данные " +
                "в hex и ASCII, чтобы методом тыка определить реальный формат выдачи ридера.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = viewModel::refreshRfidSerialPorts,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Обновить порты")
        }

        Spacer(Modifier.height(8.dp))
        if (state.rfidSerialPorts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "Порты не найдены. Нажмите «Обновить порты».",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            state.rfidSerialPorts.forEach { port ->
                val recognized = port.driverType != "—"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            selectedPort == port.deviceName -> MaterialTheme.colorScheme.primaryContainer
                            recognized -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        },
                    ),
                    onClick = { viewModel.setRfidSelectedUsbPath(port.deviceName) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedPort == port.deviceName,
                            onClick = { viewModel.setRfidSelectedUsbPath(port.deviceName) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(port.deviceName, style = MaterialTheme.typography.bodyMedium)
                            val isNative = port.driverType == "Native UART"
                            Text(
                                text = when {
                                    isNative -> "Native UART (hardware serial)"
                                    recognized -> "${port.driverType}  ·  VID 0x${port.vendorId.toString(16).uppercase()}  ·  PID 0x${port.productId.toString(16).uppercase()}"
                                    else -> "Неизвестный драйвер  ·  VID 0x${port.vendorId.toString(16).uppercase()}  ·  PID 0x${port.productId.toString(16).uppercase()}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isNative -> MaterialTheme.colorScheme.primary
                                    recognized -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = viewModel::connectRfidReader,
                enabled = !state.rfidConnectBusy && selectedPort.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.rfidConnected) "Переподключить" else "Подключить")
            }
            OutlinedButton(
                onClick = viewModel::disconnectRfidReader,
                enabled = state.rfidConnected || state.rfidConnectBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Отключить")
            }
        }

        if (state.rfidConnectBusy) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Подключение RFID-ридера…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (state.rfidConnected) "Статус: подключён" else "Статус: не подключён",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.rfidConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.rfidBanner?.let { banner ->
            Spacer(Modifier.height(6.dp))
            Text(
                banner,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.rfidConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Serial monitor RFID", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = viewModel::clearRfidTrafficLog) {
                Text("Очистить")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Прикладывайте карты и смотрите, что реально прилетает в RX. Если устройство шлёт UID как строку, " +
                "это будет видно в ASCII; если шлёт бинарные кадры, они будут видны в HEX.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                "Лог пуст. Подключите ридер и приложите карту.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(6.dp),
            ) {
                LazyColumn(state = listState) {
                    items(entries, key = { it.id }) { entry ->
                        val isExpanded = expanded[entry.id] == true
                        RfidTrafficRow(
                            entry = entry,
                            expanded = isExpanded,
                            onToggle = { expanded[entry.id] = !isExpanded },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RfidTrafficRow(
    entry: RfidTrafficEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val directionColor =
        when (entry.direction) {
            "RX" -> MaterialTheme.colorScheme.tertiary
            "TX" -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp),
            )
            Text(
                "${entry.direction} [${entry.port}]",
                style = MaterialTheme.typography.labelSmall,
                color = directionColor,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(88.dp),
            )
            Text(
                if (entry.hex.isNotEmpty()) entry.hex else entry.note,
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.hex.isNotEmpty()) directionColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
        }
        if (expanded && (entry.ascii.isNotEmpty() || entry.note.isNotEmpty())) {
            Spacer(Modifier.height(4.dp))
            if (entry.ascii.isNotEmpty()) {
                Text(
                    "ASCII: ${entry.ascii}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (entry.note.isNotEmpty()) {
                Text(
                    entry.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
