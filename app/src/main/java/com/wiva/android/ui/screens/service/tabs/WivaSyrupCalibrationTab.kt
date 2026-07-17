package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.ui.screens.service.ServiceViewModel
import com.wiva.android.ui.screens.service.SettingsColumn
import com.wiva.android.ui.screens.service.SettingsTextField

@Composable
fun WivaSyrupCalibrationTab(viewModel: ServiceViewModel) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshSyrupCalibrationUi()
    }

    val selected = s.syrupSelectedContainerNumber
    val currentCf =
        selected?.let { n -> s.syrupContainers.find { it.containerNumber == n }?.conversionFactor }

    SettingsColumn {
        Text("Калибровка сиропов", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
 "Тестовый налив (ServiceCommand 0x52 / 0x09), затем фактический объём и пересчёт conversionFactor,.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (s.syrupContainers.isEmpty()) {
            Text(
                "Нет контейнеров в merge-конфиге. Дождитесь телеметрии (матрица наполнения) или проверьте «Наполнение».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Контейнер", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Column {
                for (c in s.syrupContainers) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == c.containerNumber,
                            onClick = { viewModel.setSyrupSelectedContainerNumber(c.containerNumber) },
                        )
                        Text(
                            "#${c.containerNumber} — ${c.catalogTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Текущий conversionFactor: ${currentCf?.let { "%.4f".format(it) } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            s.syrupNewConversionFactor?.let { ncf ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "После сохранения: ${"%.4f".format(ncf)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(16.dp))
            SettingsTextField(
                label = "Целевой объём продукта (мл)",
                value = s.syrupTargetMlText,
                onValueChange = viewModel::setSyrupTargetMlText,
            )
            Button(
                onClick = { viewModel.runSyrupTestPour() },
                enabled = !s.syrupPourBusy && selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Налить")
            }
            if (s.syrupPourBusy) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(strokeWidth = 2.dp)
            }

            Spacer(Modifier.height(20.dp))
            SettingsTextField(
                label = "Фактический объём (мл)",
                value = s.syrupActualMlText,
                onValueChange = viewModel::setSyrupActualMlText,
            )
            Button(
                onClick = { viewModel.saveSyrupCalibration() },
                enabled = !s.syrupSaveBusy && selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
            if (s.syrupSaveBusy) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
        s.syrupBanner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(
                banner,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (s.syrupBannerIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
    }
}
