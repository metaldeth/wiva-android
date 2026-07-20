package com.viwa.android.ui.screens.service.tabs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.domain.model.CardPaymentMockMode
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun ViwaCardPaymentMethodTab(
    viewModel: ViwaCardPaymentMethodViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("Оплата картой", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Продакшен-путь — USB aQsi Pill (Arcus2). Legacy 2can/PAX удалён.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Активный метод: aQsi USB; mock: ${mockLabel(ui.mockMode)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Настройки host/port в JsonStore больше не используются для оплаты. Диагностика USB, тест 1 ₽ и журнал обмена — на вкладках «aQsi» и «Тесты и журнал».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun mockLabel(mode: CardPaymentMockMode): String =
    when (mode) {
        CardPaymentMockMode.Disabled -> "выключен"
        CardPaymentMockMode.Aqsi -> "aQsi"
    }
