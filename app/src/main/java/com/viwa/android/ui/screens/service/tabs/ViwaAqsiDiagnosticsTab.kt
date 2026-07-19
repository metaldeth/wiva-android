package com.viwa.android.ui.screens.service.tabs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.domain.model.CardPaymentMockOutcome
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun ViwaAqsiDiagnosticsTab(
    viewModel: ViwaAqsiDiagnosticsViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("Новый считыватель — тесты и журнал", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Порядок: (1) выберите aQsi активным методом; (2) включите или выключите mock отдельно — это не отправляет оплату; " +
                "(3) запускайте mock-сценарии или реальный тест 1 коп.; журнал — те же карточки OUT/IN/MOCK/SYS, что и для 2can.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Text("Шаг 1 · Активный считыватель", style = MaterialTheme.typography.titleMedium)
        Text(ui.cardMethodLabel, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::selectAqsiAsActive,
            enabled = !ui.pennyTestBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сделать aQsi активным методом оплаты картой")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Шаг 2 · Mock для нового считывателя", style = MaterialTheme.typography.titleMedium)
        Text(
            "Только флаг симуляции в приложении; не меняет активный метод выше и не ходит на TCP.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Состояние: ${mockModeLabel(ui.mockMode)}",
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (ui.mockMode === CardPaymentMockMode.Aqsi) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::enableAqsiMock,
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Включить mock")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = viewModel::disableMock,
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Выключить mock")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Шаг 3 · Mock-сценарии оплаты", style = MaterialTheme.typography.titleMedium)
        Text(
            "Требуется включённый mock (шаг 2). Перед сценарием метод будет переключён на aQsi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::runMockPayment,
            enabled = !ui.pennyTestBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Mock: успех")
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Declined) },
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: отказ")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Cancelled) },
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: отмена")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Timeout) },
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: таймаут")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Последняя операция aQsi", style = MaterialTheme.typography.titleMedium)
        ui.lastOperationLine1?.let { line ->
            Text(line, style = MaterialTheme.typography.bodyMedium)
        }
        ui.lastOperationLine2?.let { line ->
            Spacer(Modifier.height(4.dp))
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Шаг 4 · Реальный терминал и отмена", style = MaterialTheme.typography.titleMedium)
        Text(
            "Тест 1 коп.: TCP к сохранённому host/port ридера. Отмена — отдельная кнопка.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::runPennyTest,
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Реальный тест 1 коп.")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = viewModel::cancelAqsiFromServiceMenu,
                enabled = !ui.pennyTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Запрос отмены aQsi")
            }
            if (ui.pennyTestBusy) {
                Spacer(Modifier.width(10.dp))
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }

        ui.pennyTestBanner?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (ui.pennyTestBannerIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Журнал платёжника", style = MaterialTheme.typography.titleMedium)
        Text(
            "Общий журнал с вкладкой 2can. Раскройте карточку для провайдера, исхода и полного текста ошибки.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = viewModel::clearLog) {
                Text("Очистить журнал")
            }
        }
        Spacer(Modifier.height(8.dp))
        CardPaymentLogPanel(logEntries)
    }
}

private fun mockModeLabel(mode: CardPaymentMockMode): String =
    when (mode) {
        CardPaymentMockMode.Disabled -> "mock выключен"
        CardPaymentMockMode.TwoCan -> "mock для 2can (не для этой вкладки)"
        CardPaymentMockMode.Aqsi -> "mock включён для нового считывателя"
    }
