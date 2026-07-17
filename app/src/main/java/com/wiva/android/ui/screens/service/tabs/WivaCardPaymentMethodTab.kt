package com.wiva.android.ui.screens.service.tabs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.domain.model.CardPaymentMethod
import com.wiva.android.domain.model.CardPaymentMockMode
import com.wiva.android.ui.screens.service.SettingsColumn

@Composable
fun WivaCardPaymentMethodTab(
    viewModel: WivaCardPaymentMethodViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("Метод оплаты картой", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Глобальный выбор для экрана заказа (не СБП): какой считыватель карты используется для реальной продажи или mock-теста.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Сейчас активно: ${methodLabel(ui.selected)}; mock: ${mockLabel(ui.mockMode)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        MethodRow(
            title = "2can",
            subtitle = "Штатный считыватель через контроллер автомата. Настройки, mock и тест оплаты — во вкладке «2can».",
            selected = ui.selected === CardPaymentMethod.Pax,
            enabled = !ui.isBusy,
            onSelect = viewModel::selectPax,
        )
        Spacer(Modifier.height(8.dp))
        MethodRow(
            title = "Новый считыватель aQsi",
            subtitle = "Новый карточный считыватель. Настройки подключения, mock и тест оплаты — на соседних вкладках.",
            selected = ui.selected === CardPaymentMethod.Aqsi,
            enabled = !ui.isBusy,
            onSelect = viewModel::selectAqsi,
        )

        if (ui.isBusy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                strokeWidth = 2.dp,
            )
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

        Spacer(Modifier.height(24.dp))
        Text(
            "Важно: одновременно активен только один карточный провайдер для экрана заказа. Mock-кнопки в настройках нужны только для проверки интерфейса без железа.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun methodLabel(method: CardPaymentMethod): String =
    when (method) {
        CardPaymentMethod.Pax -> "2can"
        CardPaymentMethod.Aqsi -> "новый считыватель aQsi"
    }

private fun mockLabel(mode: CardPaymentMockMode): String =
    when (mode) {
        CardPaymentMockMode.Disabled -> "выключен"
        CardPaymentMockMode.TwoCan -> "2can"
        CardPaymentMockMode.Aqsi -> "новый считыватель aQsi"
    }

@Composable
private fun MethodRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
