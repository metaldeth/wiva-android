package com.wiva.android.ui.screens.service.tabs

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
import com.wiva.android.domain.model.CardPaymentMockMode
import com.wiva.android.domain.model.CardPaymentMockOutcome
import com.wiva.android.ui.screens.service.SettingsColumn

@Composable
fun WivaTwoCanPaymentSettingsTab(
    viewModel: WivaTwoCanPaymentSettingsViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("2can — настройки и тесты", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Порядок: (1) выберите 2can активным методом оплаты картой; (2) при необходимости включите mock 2can отдельной кнопкой — это не запускает оплату; " +
                "(3) запускайте mock-сценарии или реальный тест; журнал показывает OUT/IN/MOCK/SYS как в дебаге контроллера.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Text("Шаг 1 · Активный считыватель", style = MaterialTheme.typography.titleMedium)
        Text(
            if (ui.isSelected) {
                "Сейчас активен: 2can (PAX по шине контроллера)"
            } else {
                "Сейчас активен другой метод — см. вкладку «Метод»"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (ui.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::selectTwoCan,
            enabled = !ui.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Выбрать 2can для оплаты картой")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Шаг 2 · Mock для 2can", style = MaterialTheme.typography.titleMedium)
        Text(
            "Только переключает симуляцию: без отправки суммы и без смены активного метода выше.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Состояние: ${mockModeLabel(ui.mockMode)}",
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (ui.mockMode === CardPaymentMockMode.TwoCan) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::enableTwoCanMock,
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Включить mock 2can")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = viewModel::disableMock,
                enabled = !ui.isBusy,
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
            "Требуется включённый mock 2can (шаг 2). Перед оплатой метод будет переключён на 2can.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::runMockPayment,
            enabled = !ui.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Mock: успех")
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Declined) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: отказ")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Cancelled) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: отмена")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.runMockPaymentScenario(CardPaymentMockOutcome.Timeout) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mock: таймаут")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Шаг 4 · Реальный платёжник", style = MaterialTheme.typography.titleMedium)
        Text(
            "Статус оплаты (PAX): ${ui.statusLine.ifBlank { "—" }}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Отправка суммы идёт по реальному пути 2can. Если железа нет — используйте mock-сценарии выше.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::sendTestAmount,
            enabled = !ui.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Реальный тест: отправить 1 руб. на терминал")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::cancelTransaction,
            enabled = !ui.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Отмена операции 2can (локально, без VendCancel)")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Диагностика · подстановка статуса Pax", style = MaterialTheme.typography.titleSmall)
        Text(
            "Имитирует ответ контроллера по шине (для проверки UI статуса без терминала).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.simulateDiagnosticPaxStatus(2) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Код 2")
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = { viewModel.simulateDiagnosticPaxStatus(4) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Код 4")
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = { viewModel.simulateDiagnosticPaxStatus(5) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Код 5")
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = { viewModel.simulateDiagnosticPaxStatus(6) },
                enabled = !ui.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Код 6")
            }
        }

        if (ui.isBusy) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Выполняется…", style = MaterialTheme.typography.bodySmall)
            }
        }

        ui.banner?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (ui.bannerIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "Журнал платёжника",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Карточки по типу дебага контроллера: нажмите строку, чтобы раскрыть детали и текст ошибки.",
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
        CardPaymentMockMode.TwoCan -> "mock включён для 2can"
        CardPaymentMockMode.Aqsi -> "mock включён для нового считывателя (не для 2can)"
    }
