package com.viwa.android.ui.screens.service.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.viwa.android.hardware.serial.SerialDeviceInfo

@Composable
fun FindScannerDialog(
    drivers: List<SerialDeviceInfo>,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    labels: DevicesBlockUiLabels = DevicesBlockUiLabels.KioskDefaults,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = labels.findScannerDialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(12.dp))
                if (drivers.isEmpty()) {
                    Text(
                        text = labels.findScannerNoDevices,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(drivers, key = { it.deviceName }) { device ->
                            TextButton(
                                onClick = { onSelect(device.deviceName) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = device.deviceName)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isRunning) {
                        Button(onClick = onStart) { Text(labels.findStartSearch) }
                    } else {
                        Button(
                            onClick = onStop,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text(labels.findStopSearch)
                        }
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    TextButton(onClick = onDismiss) { Text(labels.findCancel) }
                }
            }
        }
    }
}

