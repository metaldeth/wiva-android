package com.viwa.android.ui.screens.service.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.ui.screens.service.SettingsColumn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViwaPortsBlockContent(
    controllerPortScanController: ViwaControllerPortScanController,
    labels: ViwaDevicesUiLabels = ViwaDevicesUiLabels(),
    modifier: Modifier = Modifier,
) {
    val scanState by controllerPortScanController.state.collectAsStateWithLifecycle()
    LaunchedEffect(controllerPortScanController) {
        controllerPortScanController.refreshPorts()
    }
    DisposableEffect(controllerPortScanController) {
        onDispose { controllerPortScanController.cancelAllProbes() }
    }

    SettingsColumn(modifier = modifier.fillMaxSize()) {
        ViwaControllerPortScanPanel(
            state = scanState,
            actions = controllerPortScanController,
            labels = labels,
        )
        Spacer(Modifier.height(12.dp))
        ViwaManualPortProbePanel(
            state = scanState,
            actions = controllerPortScanController,
            labels = labels,
        )
        Spacer(Modifier.height(12.dp))
        ViwaPortUsageTableHeader(labels = labels)
        ViwaPortUsageTable(
            rows = scanState.portRows,
            labels = labels,
            modifier = Modifier.height(160.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = labels.portsLogTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        ViwaPortScanLogList(
            entries = scanState.logEntries,
            labels = labels,
            modifier = Modifier.height(140.dp),
        )
    }
}

@Composable
private fun ViwaControllerPortScanPanel(
    state: ViwaControllerPortScanState,
    actions: ViwaControllerPortScanActions,
    labels: ViwaDevicesUiLabels,
) {
    Button(
        onClick = actions::startScan,
        enabled = !state.isRunning,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text(labels.controllerScanButton)
        }
    }
    state.statusMessage?.let { message ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ViwaManualPortProbePanel(
    state: ViwaControllerPortScanState,
    actions: ViwaControllerPortScanActions,
    labels: ViwaDevicesUiLabels,
) {
    var portMenuExpanded by remember { mutableStateOf(false) }
    val selectedPort = state.selectedPort
    Text(
        text = labels.portsManualTitle,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { portMenuExpanded = true },
        enabled = state.availablePorts.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text =
                selectedPort?.let { "${labels.portsManualSelectPort}: $it" }
                    ?: labels.portsManualSelectPort,
            fontFamily = FontFamily.Monospace,
        )
    }
    DropdownMenu(expanded = portMenuExpanded, onDismissRequest = { portMenuExpanded = false }) {
        state.availablePorts.forEach { path ->
            DropdownMenuItem(
                text = { Text(path, fontFamily = FontFamily.Monospace) },
                onClick = {
                    actions.selectPort(path)
                    portMenuExpanded = false
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = actions::probePrimaryVersionManual,
            enabled = selectedPort != null,
            modifier = Modifier.weight(1f),
        ) {
            Text(labels.portsManualPrimaryVersion)
        }
        Button(
            onClick = actions::probeConnectedPrimaryVersionManual,
            modifier = Modifier.weight(1f),
        ) {
            Text(labels.portsManualConnectedPrimaryVersion)
        }
    }
    if (state.isManualRunning) {
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = labels.portsManualLogTitle,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
    ViwaPortScanLogList(
        entries = state.manualLogEntries,
        labels = labels,
        modifier = Modifier.fillMaxWidth().height(100.dp),
    )
}

@Composable
private fun ViwaPortUsageTableHeader(labels: ViwaDevicesUiLabels) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = labels.portsTableHeaderPath,
            modifier = Modifier.weight(0.34f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = labels.portsTableHeaderUsage,
            modifier = Modifier.weight(0.33f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = labels.portsTableHeaderScanResult,
            modifier = Modifier.weight(0.33f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ViwaPortUsageTable(
    rows: List<ViwaPortUsageRow>,
    labels: ViwaDevicesUiLabels,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) {
        Text(
            text = labels.controllerScanNoPorts,
            modifier = modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(rows, key = { it.devicePath }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = row.devicePath,
                    modifier = Modifier.weight(0.34f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = row.currentUsage,
                    modifier = Modifier.weight(0.33f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = row.lastScanResult,
                    modifier = Modifier.weight(0.33f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ViwaPortScanLogList(
    entries: List<ViwaPortScanLogEntry>,
    labels: ViwaDevicesUiLabels,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Text(
            text = labels.portsLogEmpty,
            modifier = modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(entries.size) { index ->
            val entry = entries[index]
            val prefix =
                when (entry.kind) {
                    ViwaPortScanLogKind.Tx -> labels.portScanLogKindTx
                    ViwaPortScanLogKind.Rx -> labels.portScanLogKindRx
                    ViwaPortScanLogKind.Info -> labels.portScanLogKindInfo
                }
            Text(
                text = "${timeFormatter.format(Date(entry.timestampMs))} $prefix ${entry.port} ${labels.portScanProtocolPrimary} ${entry.payload}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
