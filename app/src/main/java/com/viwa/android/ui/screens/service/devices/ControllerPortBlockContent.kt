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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.ui.screens.service.InfoRow
import com.viwa.android.ui.screens.service.ServiceMenuTestTags
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun ControllerPortBlockContent(
    controller: ControllerPortBlockController,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(controller) {
        controller.refresh()
    }

    val state by controller.state.collectAsStateWithLifecycle()

    SettingsColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(ServiceMenuTestTags.CONTROLLER_PORT_ROOT),
    ) {
        Text(
            text = "Контроллер",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                "Выберите serial/USB-порт контроллера и нажмите «Подключить». " +
                    "Путь сохраняется в настройках и используется ControllerHardwareManager.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))

        ControllerPortStatusCard(state = state)

        Spacer(Modifier.height(12.dp))
        ControllerPortDropdown(
            ports = state.availablePorts,
            selectedPort = state.selectedPort,
            enabled = !state.isLoading && !state.isConnecting,
            onSelect = controller::selectPort,
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = controller::refresh,
                enabled = !state.isLoading && !state.isConnecting,
                modifier = Modifier.weight(1f),
            ) {
                Text("Обновить порты")
            }
            Button(
                onClick = controller::connectSelected,
                enabled = !state.isLoading && !state.isConnecting && !state.selectedPort.isNullOrBlank(),
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(ServiceMenuTestTags.CONTROLLER_PORT_CONNECT),
            ) {
                Text("Подключить")
            }
        }

        if (state.isLoading || state.isConnecting) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (state.isConnecting) "Подключение…" else "Загрузка портов…",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        state.banner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = banner,
                color =
                    if (state.bannerIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ControllerPortStatusCard(state: ControllerPortBlockState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow("Назначенный порт", state.assignedPort ?: "—")
            InfoRow(
                "Соединение",
                if (state.isPhysicalConnected) "Подключён (UART/USB)" else "Не подключён",
            )
        }
    }
}

@Composable
private fun ControllerPortDropdown(
    ports: List<com.viwa.android.hardware.serial.SerialDeviceInfo>,
    selectedPort: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDevice = ports.firstOrNull { it.deviceName == selectedPort }

    OutlinedButton(
        onClick = { expanded = true },
        enabled = enabled && ports.isNotEmpty(),
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(ServiceMenuTestTags.CONTROLLER_PORT_DROPDOWN),
    ) {
        Text(
            text =
                selectedDevice?.let { device ->
                    buildString {
                        append(device.deviceName)
                        device.driverType?.let { append(" · $it") }
                    }
                } ?: if (ports.isEmpty()) {
                    "Serial/USB порты не найдены"
                } else {
                    "Выберите порт контроллера"
                },
            fontFamily = FontFamily.Monospace,
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ports.forEach { device ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(device.deviceName, fontFamily = FontFamily.Monospace)
                        Text(
                            text =
                                device.driverType?.let { "Serial driver: $it" }
                                    ?: "Serial driver: не найден",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = {
                    onSelect(device.deviceName)
                    expanded = false
                },
            )
        }
    }
}
