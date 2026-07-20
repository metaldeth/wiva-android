package com.viwa.android.ui.screens.service.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.ui.screens.service.SettingsColumn
import com.viwa.android.ui.screens.service.serviceMenuSp

@Composable
fun ScannerDebugBlockContent(
    controller: ScannerDebugBlockController,
    scannerActive: Boolean,
    onStartScanner: () -> Unit,
    onStopScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val entriesReversed = remember(state.entries) { state.entries.asReversed() }
    val expandedIds = remember { mutableStateMapOf<Long, Boolean>() }

    SettingsColumn(modifier = modifier) {
        Text("Сканер — отладка", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Live-события со сканера USB serial. Назначение порта — на вкладке «Устройства».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (scannerActive) "Чтение активно" else "Чтение остановлено",
            color =
                if (scannerActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = onStartScanner, modifier = Modifier.weight(1f)) {
                Text("Старт")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onStopScanner,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Стоп")
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text("Лог сканера", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = {
                    controller.clearLog()
                    expandedIds.clear()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Очистить")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (entriesReversed.isEmpty()) {
            Text(
                "Пока нет событий. Отсканируйте штрихкод или подключите USB-сканер.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            entriesReversed.forEach { entry ->
                val expanded = expandedIds[entry.stableKey] == true
                ScannerDebugRow(entry = entry, expanded = expanded) {
                    expandedIds[entry.stableKey] = !expanded
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ScannerDebugRow(
    entry: ScannerDebugRowUi,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.tertiary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(scheme.tertiaryContainer.copy(alpha = if (expanded) 0.95f else 0.72f))
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column {
            Row {
                Text(
                    if (entry.isSystem) "SYS" else "SCAN",
                    color = accent,
                    fontSize = serviceMenuSp(10),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.timeLabel,
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.classification,
                    fontSize = serviceMenuSp(12),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
            }
            if (expanded && entry.raw.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = accent.copy(alpha = 0.25f))
                Spacer(Modifier.height(6.dp))
                Text(
                    "Строка: ${entry.raw}",
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
