package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.domain.model.CellVolumeUpdate
import com.wiva.android.domain.model.MachineInventoryTableRow
import com.wiva.android.ui.screens.service.ServiceViewModel
import com.wiva.android.ui.screens.service.SettingsColumn
import com.wiva.android.ui.screens.service.SettingsTextField

@Composable
fun WivaInventoryVolumesTab(
    viewModel: ServiceViewModel,
) {
    val rows by viewModel.telemetryInventoryRows.collectAsStateWithLifecycle()
    val serviceState by viewModel.state.collectAsStateWithLifecycle()
    var draftVolumes by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var banner by remember { mutableStateOf<String?>(null) }
    var syrupPrimeBusyCell by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshInventoryRows()
    }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            draftVolumes = rows.associate { it.cellNumber to (it.volumeMl?.toString() ?: "0") }
        }
    }

    SettingsColumn {
        Text(
            "Текущие объёмы по контейнерам (мл). Сохранение записывает merge-конфиг и отправляет cellVolumeImportTopic в телеметрию.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                banner = null
                viewModel.refreshInventoryRows()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Обновить с диска")
        }
        Spacer(Modifier.height(16.dp))
        if (rows.isEmpty()) {
            Text(
                "Нет данных наполнения. Подключите телеметрию и получите базу/матрицу или импортируйте конфиг.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                rows.sortedBy { it.cellNumber }.forEach { row ->
                    val isSyrupCell =
                        serviceState.syrupContainers.any { it.containerNumber == row.cellNumber }
                    VolumeRow(
                        row = row,
                        text = draftVolumes[row.cellNumber].orEmpty(),
                        syrupPrimeEnabled = isSyrupCell,
                        syrupPrimeBusy = syrupPrimeBusyCell == row.cellNumber,
                        onTextChange = { t ->
                            draftVolumes = draftVolumes + (row.cellNumber to t)
                            banner = null
                        },
                        onFillToMax =
                            row.maxVolumeMl?.let { maxMl ->
                                {
                                    banner = null
                                    viewModel.fillInventoryCellToMax(row.cellNumber, maxMl) { ok, msg ->
                                        banner = msg
                                        if (ok) {
                                            viewModel.refreshInventoryRows()
                                        }
                                    }
                                }
                            },
                        onSyrupPrime =
                            if (isSyrupCell) {
                                {
                                    banner = null
                                    syrupPrimeBusyCell = row.cellNumber
                                    viewModel.runInventorySyrupPrime(row.cellNumber) { _, msg ->
                                        banner = msg
                                        syrupPrimeBusyCell = null
                                    }
                                }
                            } else {
                                null
                            },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val updates = buildUpdates(rows, draftVolumes)
                    if (updates == null) {
                        banner = "Введите целые числа ≥ 0 для всех ячеек"
                    } else {
                        viewModel.saveInventoryVolumes(updates) { ok, msg ->
                            banner = msg
                            if (ok) {
                                viewModel.refreshInventoryRows()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
            banner?.let { b ->
                Spacer(Modifier.height(8.dp))
                Text(b, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VolumeRow(
    row: MachineInventoryTableRow,
    text: String,
    syrupPrimeEnabled: Boolean,
    syrupPrimeBusy: Boolean,
    onTextChange: (String) -> Unit,
    onFillToMax: (() -> Unit)?,
    onSyrupPrime: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Ячейка ${row.cellNumber} · ${row.catalogTitle}",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsTextField(
                label = "Объём, мл",
                value = text,
                onValueChange = onTextChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                fieldKey = "inventory_volume_${row.cellNumber}",
                maxLength = 12,
            )
            if (onFillToMax != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onFillToMax) {
                    Text("До полного")
                }
            }
            if (syrupPrimeEnabled && onSyrupPrime != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onSyrupPrime,
                    enabled = !syrupPrimeBusy,
                ) {
                    Text(if (syrupPrimeBusy) "Прокачка..." else "Прокачка 30 мл")
                }
            }
        }
    }
}

private fun buildUpdates(
    rows: List<MachineInventoryTableRow>,
    draft: Map<Int, String>,
): List<CellVolumeUpdate>? {
    val out = ArrayList<CellVolumeUpdate>(rows.size)
    for (r in rows) {
        val s = draft[r.cellNumber]?.trim().orEmpty()
        val v = s.toIntOrNull() ?: return null
        if (v < 0) return null
        out.add(CellVolumeUpdate(containerNumber = r.cellNumber, volumeMl = v))
    }
    return out
}
