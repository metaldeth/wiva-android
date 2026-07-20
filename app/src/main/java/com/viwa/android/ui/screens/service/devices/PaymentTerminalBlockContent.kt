package com.viwa.android.ui.screens.service.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.hardware.serial.SerialDeviceInfo
import com.viwa.android.ui.screens.service.InfoRow
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun PaymentTerminalBlockContent(
    controller: PaymentTerminalBlockController,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        controller.loadUsbDevices()
    }

    val state by controller.state.collectAsStateWithLifecycle()

    SettingsColumn(modifier = modifier.fillMaxSize()) {
        Text(
            "Платёжный терминал (AQSI PILL / T7100)",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        InfoRow("Статус", state.paymentStatus.name)
        if (state.terminalStatus.isNotBlank()) {
            InfoRow("Детали", state.terminalStatus)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Тестовая оплата запускает Arcus2-сценарий на 1 ₽ через USB/VCOM. Карта должна лежать на считывателе.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))

        state.testResult?.let {
            Text(it, color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = controller::testPayment,
                enabled = !state.isTestRunning,
            ) {
                Text("Тест оплаты 1 ₽")
            }
            OutlinedButton(
                onClick = controller::loadUsbDevices,
                enabled = !state.isUsbLoading,
            ) {
                Text("Обновить USB")
            }
        }
        Spacer(Modifier.height(16.dp))
        UsbDevicesList(devices = state.usbDevices)
        Spacer(Modifier.height(16.dp))
        ExchangeLogSection(lines = state.exchangeLogLines)
        Spacer(Modifier.height(12.dp))
        Text(
            "Кнопка запускает реальную тестовую оплату на 1 ₽ через Arcus2. Перед проверкой ридер должен быть настроен в режиме arcus2.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun UsbDevicesList(devices: List<SerialDeviceInfo>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "USB serial устройства:",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
        )
        if (devices.isEmpty()) {
            Text(
                "Пока не найдены",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            devices.forEach { device ->
                Text(
                    "${device.deviceName}  VID=${device.vendorId.hex4()} PID=${device.productId.hex4()}  ${device.driverType ?: "Unknown"}",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun Int.hex4(): String = "%04X".format(this)

@Composable
private fun ExchangeLogSection(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Лог обмена:",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
        )
        if (lines.isEmpty()) {
            Text(
                "Нажмите «Тест оплаты 1 ₽» для записи в лог обмена",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            lines.forEach { line ->
                Text(
                    line,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}
