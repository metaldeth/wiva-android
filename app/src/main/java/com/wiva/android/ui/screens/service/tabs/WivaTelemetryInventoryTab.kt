package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wiva.android.domain.model.MachineInventoryTableRow
import com.wiva.android.ui.screens.service.ServiceViewModel
import com.wiva.android.ui.screens.service.SettingsColumn

@Composable
fun WivaTelemetryInventoryTab(
    viewModel: ServiceViewModel,
) {
    val inventoryRows by viewModel.telemetryInventoryRows.collectAsStateWithLifecycle()
    SettingsColumn {
        Text(
 "Данные после объединения базы ингредиентов и матрицы наполнения. " +
                "Обновляется при входящих WS baseIngredientRequestExportTopic и cellStoreExport / cellStoreRequestExport.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { viewModel.refreshInventoryRows() }) {
            Text("Обновить таблицу с диска")
        }
        Spacer(Modifier.height(16.dp))
        if (inventoryRows.isEmpty()) {
            Text(
                "Пока нет данных: подключите WS и запросите базу и наполнение (вкладка «Тесты»), либо дождитесь push cellStoreExport.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Контейнеров: ${inventoryRows.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            val hScroll = rememberScrollState()
            val vScroll = rememberScrollState()
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .verticalScroll(vScroll),
            ) {
                Row(Modifier.horizontalScroll(hScroll)) {
                    InventoryTable(rows = inventoryRows)
                }
            }
        }
    }
}

@Composable
private fun InventoryTable(rows: List<MachineInventoryTableRow>) {
    Column {
        InventoryHeaderRow()
        HorizontalDivider()
        rows.forEach { row ->
            InventoryDataRow(row)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun InventoryHeaderRow() {
    Row(Modifier.padding(vertical = 8.dp)) {
        HCell("Яч.", 44)
        HCell("ID", 52)
        HCell("Полное название", 260)
        HCell("Вкус", 100)
        HCell("Бренд", 120)
        HCell("300 мл", 72)
        HCell("700 мл", 72)
        HCell("Акт.", 44)
        HCell("Остаток", 72)
        HCell("Min", 56)
        HCell("Max", 56)
        HCell("Напиток мл", 80)
    }
}

@Composable
private fun InventoryDataRow(row: MachineInventoryTableRow) {
    Row(Modifier.padding(vertical = 6.dp)) {
        HCell(row.cellNumber.toString(), 44)
        HCell(row.ingredientId.toString(), 52)
        HCell(row.catalogTitle, 260, maxLines = 4)
        HCell(row.tasteName, 100, maxLines = 2)
        HCell(row.brandName, 120, maxLines = 2)
        HCell(row.price300Rub?.toString() ?: "—", 72)
        HCell(row.price700Rub?.toString() ?: "—", 72)
        HCell(if (row.active) "да" else "нет", 44)
        HCell(row.volumeMl?.toString() ?: "—", 72)
        HCell(row.minVolumeMl?.toString() ?: "—", 56)
        HCell(row.maxVolumeMl?.toString() ?: "—", 56)
        HCell(row.drinkVolumeMl.toString(), 80)
    }
}

@Composable
private fun HCell(
    text: String,
    widthDp: Int,
    maxLines: Int = 2,
) {
    Text(
        text = text,
        modifier = Modifier.width(widthDp.dp),
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
