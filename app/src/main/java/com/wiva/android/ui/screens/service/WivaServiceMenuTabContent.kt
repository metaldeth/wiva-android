package com.wiva.android.ui.screens.service

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wiva.android.ui.screens.service.tabs.WivaControllerDebugTab
import com.wiva.android.ui.screens.service.tabs.WivaIdleTab
import com.wiva.android.ui.screens.service.tabs.WivaSyrupCalibrationTab
import com.wiva.android.ui.screens.service.tabs.WivaWaterCalibrationTab
import com.wiva.android.ui.screens.service.tabs.WivaEquipmentScannerTab
import com.wiva.android.ui.screens.service.tabs.WivaInventoryVolumesTab
import com.wiva.android.ui.screens.service.tabs.WivaTelemetryInventoryTab
import com.wiva.android.ui.screens.service.tabs.WivaAnimationTab
import com.wiva.android.ui.screens.service.tabs.WivaAqsiDiagnosticsTab
import com.wiva.android.ui.screens.service.tabs.WivaAqsiSettingsTab
import com.wiva.android.ui.screens.service.tabs.WivaCardPaymentMethodTab
import com.wiva.android.ui.screens.service.tabs.WivaTwoCanPaymentSettingsTab
import com.wiva.android.ui.screens.service.tabs.WivaEquipmentRfidTab
import com.wiva.android.ui.screens.service.tabs.WivaPreparingTimeTab
import com.wiva.android.ui.screens.service.tabs.WivaFlowStripRgbSection
import com.wiva.android.ui.screens.service.tabs.WivaThemePrimaryButtonColorSection
import com.wiva.android.ui.screens.service.tabs.WivaSubscriptionDebugTab
import com.wiva.android.ui.screens.service.tabs.WivaKeyboardTestTab
import com.wiva.android.ui.screens.service.tabs.WivaTelemetryWsLogTab
import com.wiva.android.ui.screens.service.tabs.WivaServiceDashboardTab

@Composable
fun WivaServiceMenuTabContent(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    when (state.selectedServiceGroupId) {
        WivaServiceGroupId.Dashboard ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.DashboardOverview -> WivaServiceDashboardTab(viewModel)
                else -> WivaServiceDashboardTab(viewModel)
            }

        WivaServiceGroupId.Telemetry ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.TelemetryConnection -> WivaTelemetryConnectionTab(state, viewModel)
                WivaServiceSubTabId.TelemetryAddresses -> WivaTelemetryAddressesTab(state, viewModel)
                WivaServiceSubTabId.TelemetryInventory -> WivaTelemetryInventoryTab(viewModel)
                WivaServiceSubTabId.TelemetryTests -> WivaTelemetryTestsTab(state, viewModel)
                else -> WivaTelemetryConnectionTab(state, viewModel)
            }

        WivaServiceGroupId.Debug ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.DebugWsLogs -> WivaTelemetryWsLogTab(viewModel)
                WivaServiceSubTabId.DebugController -> WivaControllerDebugTab(state, viewModel)
                WivaServiceSubTabId.DebugSubscription -> WivaSubscriptionDebugTab(state, viewModel)
                WivaServiceSubTabId.DebugKeyboardTest -> WivaKeyboardTestTab()
                else -> WivaTelemetryWsLogTab(viewModel)
            }

        WivaServiceGroupId.Integrations ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.Sbp -> WivaSbpIntegrationTab(state, viewModel)
                WivaServiceSubTabId.Nanokassa -> WivaNanoIntegrationTab(state, viewModel)
                WivaServiceSubTabId.Max -> WivaMaxIntegrationTab(state, viewModel)
                else -> WivaSbpIntegrationTab(state, viewModel)
            }

        WivaServiceGroupId.CardPayment ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.CardPaymentMethod -> WivaCardPaymentMethodTab()
                WivaServiceSubTabId.TwoCanServiceSettings -> WivaTwoCanPaymentSettingsTab()
                WivaServiceSubTabId.AqsiServiceSettings -> WivaAqsiSettingsTab()
                WivaServiceSubTabId.AqsiServiceDiagnostics -> WivaAqsiDiagnosticsTab()
                else -> WivaCardPaymentMethodTab()
            }

        WivaServiceGroupId.Equipment ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.Controller -> WivaEquipmentControllerTab(state, viewModel)
                WivaServiceSubTabId.Payment -> WivaEquipmentPaymentTab(state, viewModel)
                WivaServiceSubTabId.Scanner -> WivaEquipmentScannerTab(state, viewModel)
                WivaServiceSubTabId.Rfid -> WivaEquipmentRfidTab(state, viewModel)
                else -> WivaEquipmentControllerTab(state, viewModel)
            }

        WivaServiceGroupId.Maintenance ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.Inventory ->
                    WivaInventoryVolumesTab(viewModel)
                WivaServiceSubTabId.CalibrationSyrup ->
                    WivaSyrupCalibrationTab(viewModel)
                WivaServiceSubTabId.CalibrationWater ->
                    WivaWaterCalibrationTab(viewModel)
                WivaServiceSubTabId.PreparingTime ->
                    WivaPreparingTimeTab(viewModel)
                else ->
 WivaPlaceholderTab("Обслуживание — см. ServiceMenu.")
            }

        WivaServiceGroupId.Settings ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.DevMode -> WivaSettingsDevModeTab(state, viewModel)
                WivaServiceSubTabId.Idle -> WivaIdleTab()
                WivaServiceSubTabId.Window -> WivaWindowTab()
                WivaServiceSubTabId.Theme -> WivaThemeTab(state, viewModel)
                else -> WivaSettingsDevModeTab(state, viewModel)
            }

        WivaServiceGroupId.Updater ->
            Box(modifier = Modifier.fillMaxSize()) {
                WivaUpdaterSection(state = state, viewModel = viewModel)
            }

        WivaServiceGroupId.Performance ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.PerformanceGeneral ->
 WivaPlaceholderTab("Общие настройки производительности — как CommonPerfomanceTab.")
                WivaServiceSubTabId.Animation ->
                    WivaAnimationTab(state, viewModel)
                else ->
 WivaPlaceholderTab("Производительность — см.")
            }

        WivaServiceGroupId.Metrics ->
            when (state.selectedServiceSubTabId) {
                WivaServiceSubTabId.MetricsMemory ->
 WivaPlaceholderTab("Метрики памяти и ресурсов — MetricsTab.")
                else ->
 WivaPlaceholderTab("Метрики — см.")
            }

    }
}

@Composable
private fun WivaThemeTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Тема оформления", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (state.isDarkTheme) "Тёмная тема" else "Светлая тема",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = state.isDarkTheme,
                onCheckedChange = viewModel::setDarkTheme,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
 "Светлая тема — основная для продакшена. Тёмная тема.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Цвет бренда (кнопка «Налить воду», шкала подписки, акценты): настраивается отдельно для текущей темы — переключите «Тёмная тема», чтобы задать второй вариант.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        WivaThemePrimaryButtonColorSection(
            argb = state.customerPrimaryButtonArgb,
            onRgbPreview = viewModel::previewCustomerPrimaryRgb,
            onSliderFinished = viewModel::persistCustomerPrimaryButtonColor,
            onReset = viewModel::resetCustomerPrimaryButtonColor,
        )
        WivaFlowStripRgbSection(
            argb = state.flowStripRgbArgb,
            onRgbPreview = viewModel::previewFlowStripRgb,
            onSliderFinished = viewModel::persistFlowStripRgb,
            onReset = viewModel::resetFlowStripRgbToDefault,
        )
    }
}

@Composable
private fun WivaPlaceholderTab(message: String) {
    SettingsColumn {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WivaTelemetryConnectionTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    val conn by viewModel.telemetryConnectionUi.collectAsStateWithLifecycle()
    SettingsColumn {
        val connected = conn.connected
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (connected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val contentColor =
                        if (connected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    Text("WebSocket телеметрии", color = contentColor, style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (connected) "Подключено" else "Не подключено",
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        conn.label,
                        color = contentColor.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    conn.error?.let { err ->
                        Text(
                            err,
                            color = contentColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Button(onClick = { viewModel.reconnectTelemetry() }, enabled = !state.telemetryBusy) {
                    Text("Реконнект")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Регистрация и подключение", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
 "Код регистрации и серийный номер.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SettingsTextField(
            label = "Код регистрации (regKey)",
            value = state.telemetryRegKey,
            onValueChange = viewModel::setTelemetryRegKey,
        )
        SettingsTextField(
            label = "Серийный номер (clientId / serialNumber)",
            value = state.telemetrySerial,
            onValueChange = viewModel::setTelemetrySerial,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.registerTelemetryMachine() },
                modifier = Modifier.weight(1f),
                enabled = !state.telemetryBusy,
            ) {
                Text("Регистрация")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.connectTelemetry() },
                modifier = Modifier.weight(1f),
                enabled = !state.telemetryBusy,
            ) {
                Text("Подключить WS")
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ping/pong telemetry-machine-ws", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.telemetryPingPongEnabled) {
                        "Сейчас включён: при новом WS-подключении приложение отправит capabilities с pingPong=true."
                    } else {
                        "Сейчас выключен: capabilities с pingPong=true не отправляется."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.toggleTelemetryPingPong() },
                    enabled = !state.telemetryBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.telemetryPingPongEnabled) {
                            "Выключить ping/pong"
                        } else {
                            "Включить ping/pong"
                        },
                    )
                }
            }
        }
        if (state.telemetryBusy) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Запрос к серверу…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = { viewModel.disconnectTelemetry() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.telemetryBusy,
        ) {
            Text("Отключить WS")
        }
        WivaTelemetryBanner(state)
    }
}

@Composable
private fun WivaTelemetryAddressesTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Адреса телеметрии", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Куда ходит автомат: REST регистрация, WebSocket обмены, OAuth (Keycloak). Сохраните перед подключением на вкладке «Подключение».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SettingsTextField(
            label = "API (HTTPS) — регистрация машины",
            value = state.telemetryApiUrl,
            onValueChange = viewModel::setTelemetryApiUrl,
        )
        SettingsTextField(
            label = "WebSocket URL",
            value = state.telemetryWsUrl,
            onValueChange = viewModel::setTelemetryWsUrl,
        )
        SettingsTextField(
            label = "Keycloak URL",
            value = state.telemetryKeycloakUrl,
            onValueChange = viewModel::setTelemetryKeycloakUrl,
        )
        SettingsTextField(
            label = "Keycloak realm",
            value = state.telemetryRealm,
            onValueChange = viewModel::setTelemetryRealm,
        )
        Button(
            onClick = { viewModel.saveTelemetryEndpoints() },
            enabled = !state.telemetryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить адреса")
        }
        WivaTelemetryBanner(state)
    }
}

@Composable
private fun WivaTelemetryTestsTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Тестовые запросы", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
 "Те же обмены, что.ts): при connect уходят " +
                "cellStoreRequestExport, machineInfo, baseIngredientRequestExportTopic. " +
                    "Здесь можно повторить запросы вручную; ответы смотрите во вкладке «Логи сети».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.requestTelemetryFillingMatrix() },
            enabled = !state.telemetryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Запросить наполнение (cellStoreRequestExport)")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.requestTelemetryBaseIngredients() },
            enabled = !state.telemetryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Запросить базу ингредиентов (baseIngredientRequestExportTopic)")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.requestTelemetryMachineInfo() },
            enabled = !state.telemetryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Запросить machineInfo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.sendTelemetryDemoSaleImport() },
            enabled = !state.telemetryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Демо продажа (saleImportTopic)")
        }
        WivaTelemetryBanner(state)
    }
}

@Composable
private fun WivaTelemetryBanner(state: ServiceUiState) {
    state.telemetryBanner?.let { msg ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (state.telemetryBannerIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

@Composable
private fun WivaMaxIntegrationTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("MAX", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        SettingsTextField(
            label = "EXT API токен",
            value = state.maxExtApiToken,
            onValueChange = viewModel::setMaxExtApiToken,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Расширенный ответ", modifier = Modifier.weight(1f))
            Switch(
                checked = state.maxVerificationDetailsEnabled,
                onCheckedChange = viewModel::setMaxVerificationDetailsEnabled,
            )
        }
        Button(
            onClick = { viewModel.saveMaxIntegrationSettings() },
            enabled = !state.integrationsSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить MAX")
        }
        WivaIntegrationsBanner(state)
    }
}

@Composable
private fun WivaSbpIntegrationTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("СБП", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        SettingsTextField(
            label = "Spot ID (Paymaster)",
            value = state.sbpSpotId,
            onValueChange = viewModel::setSbpSpotId,
        )
        SettingsTextField(
            label = "Секретный ключ",
            value = state.sbpKey,
            onValueChange = viewModel::setSbpKey,
        )
        SettingsTextField(
            label = "Таймаут (сек)",
            value = state.sbpTimeoutSec,
            onValueChange = viewModel::setSbpTimeoutSec,
        )
        Button(
            onClick = { viewModel.saveSbpIntegrationSettings() },
            enabled = !state.integrationsSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить СБП")
        }
        WivaIntegrationsBanner(state)
    }
}

@Composable
private fun WivaNanoIntegrationTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Нанокасса", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        SettingsTextField(label = "kassaId", value = state.nanoKassaId, onValueChange = viewModel::setNanoKassaId)
        SettingsTextField(label = "kassaToken", value = state.nanoKassaToken, onValueChange = viewModel::setNanoKassaToken)
        SettingsTextField(label = "ККТ (zav_kkt)", value = state.nanoKkt, onValueChange = viewModel::setNanoKkt)
        SettingsTextField(label = "Адрес", value = state.nanoAddress, onValueChange = viewModel::setNanoAddress)
        SettingsTextField(label = "Место", value = state.nanoPlace, onValueChange = viewModel::setNanoPlace)
        Button(
            onClick = { viewModel.saveNanoKassaIntegrationSettings() },
            enabled = !state.integrationsSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить Нанокассу")
        }
        WivaIntegrationsBanner(state)
    }
}

@Composable
private fun WivaIntegrationsBanner(state: ServiceUiState) {
    state.integrationsBanner?.let { msg ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (state.integrationsBannerIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

@Composable
private fun WivaEquipmentControllerTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    val selectedUsb by viewModel.equipmentSelectedUsbPath.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("Контроллер (USB UART)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Выберите serial-порт и нажмите «Подключить» — открывается порт и путь сохраняется. Нужны разрешение USB и поддержка CDC.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { viewModel.refreshControllerSerialPorts() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Обновить порты")
            }
            Button(
                onClick = { viewModel.autoFindController() },
                enabled = !state.controllerAutoFindBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Найти контроллер")
            }
        }

        if (state.controllerAutoFindBusy) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    state.controllerAutoFindProgress ?: "Сканирование…",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        state.controllerAutoFindBanner?.let { banner ->
            Spacer(Modifier.height(6.dp))
            Text(
                banner,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.controllerAutoFindIsError)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(8.dp))
        if (state.controllerSerialPorts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "USB-устройства не найдены. Нажмите «Обновить порты».",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            state.controllerSerialPorts.forEach { port ->
                val recognized = port.driverType != "—"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            selectedUsb == port.deviceName -> MaterialTheme.colorScheme.primaryContainer
                            recognized -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        },
                    ),
                    onClick = { viewModel.setEquipmentSelectedUsbPath(port.deviceName) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedUsb == port.deviceName,
                            onClick = { viewModel.setEquipmentSelectedUsbPath(port.deviceName) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                port.deviceName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
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
        Button(
            onClick = { viewModel.connectControllerUsb() },
            enabled = !state.controllerEquipmentConnectBusy && selectedUsb.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Подключить")
        }
        if (state.controllerEquipmentConnectBusy) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Подключение…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.controllerEquipmentBanner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(banner, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Быстрый тест", style = MaterialTheme.typography.titleSmall)
        Button(
            onClick = { viewModel.runControllerSelfTest() },
            enabled = !state.controllerTestRunning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Тест контроллера (ReadFirmwareVersion)")
        }
        if (state.controllerTestRunning) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Выполняется тест…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.controllerTestBanner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = banner,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (state.controllerTestIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        WivaSerialMonitor(viewModel)
    }
}

@Composable
private fun WivaSerialMonitor(viewModel: ServiceViewModel) {
    val rawLog by viewModel.controllerRawTrafficFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(rawLog.size) {
        if (rawLog.isNotEmpty()) listState.animateScrollToItem(rawLog.size - 1)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Serial monitor (raw hex)", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { viewModel.clearControllerRawLog() }) {
            Text("Очистить", style = MaterialTheme.typography.labelSmall)
        }
    }
    Spacer(Modifier.height(4.dp))

    if (rawLog.isEmpty()) {
        Text(
            "Нет данных. Нажмите «Обновить порты» → выберите порт → «Подключить».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = 240.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(6.dp),
        ) {
            LazyColumn(state = listState) {
                items(rawLog, key = { it.id }) { entry ->
                    val color = when (entry.direction) {
                        "TX" -> MaterialTheme.colorScheme.primary
                        "RX" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = entry.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(72.dp),
                        )
                        Text(
                            text = "${entry.direction} [${entry.port}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.width(80.dp),
                        )
                        Text(
                            text = if (entry.hex.isNotEmpty()) entry.hex else entry.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.hex.isNotEmpty()) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WivaEquipmentPaymentTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    val terminalStatus by viewModel.terminalVendStatusLine.collectAsStateWithLifecycle()
    SettingsColumn {
        Text("Платёжник", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        InfoRow("PAX (0x56)", terminalStatus)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.runPaymentTerminal048Demo() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Тест 0x48 — сумма на терминал")
        }
        state.paymentTerminalTestBanner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(banner, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun WivaSettingsDevModeTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Режим разработки", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Мок контроллера", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.useMockController,
                onCheckedChange = viewModel::setUseMockController,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Как dev-флаг в ТЗ: при включении используется MockControllerTransport вместо serial.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Бесплатный режим (как freeMode в wiva)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = state.devFreeMode,
                onCheckedChange = viewModel::setDevFreeMode,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Выкл.: на главном экране кнопка «Оплатить» шлёт 0x48; в режиме мока затем симулируется PAX «Оплата прошла» (код 4), затем ChooseDrink.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WivaWindowTab() {
    val activity = LocalContext.current as? Activity
    var confirmed by remember { mutableStateOf(false) }

    SettingsColumn {
        Text("Управление окном", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Завершает процесс приложения. Используйте только если необходимо покинуть kiosk-режим для обслуживания устройства.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (!confirmed) {
            Button(
                onClick = { confirmed = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Закрыть приложение", color = MaterialTheme.colorScheme.onError)
            }
        } else {
            Text(
                "Вы уверены? Это закроет приложение.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { confirmed = false },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = { activity?.finishAffinity() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Подтвердить", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

