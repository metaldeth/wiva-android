package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.ui.screens.service.ServiceViewModel
import com.wiva.android.ui.screens.service.SettingsColumn
import com.wiva.android.ui.screens.service.SettingsTextField

@Composable
fun WivaWaterCalibrationTab(viewModel: ServiceViewModel) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshWaterCalibrationInfo()
    }

    SettingsColumn {
        Text("Калибровка воды", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
 "Тестовый налив (0x52 / 0x0A), замер BEGIN→SUCCESS, ввод фактического объёма и запись коэффициента (0xBC → 0xBB),.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        val info = s.waterCalInfo
        Text("Последняя калибровка", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Расход: ${info.flowRateMlPerSec?.let { "%.2f".format(it) } ?: "—"} мл/с",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Опорный (ручная калибровка): ${info.calibratedFlowRateMlPerSec?.let { "%.2f".format(it) } ?: "—"} мл/с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Адаптивный (по фактическим готовкам): ${info.adaptiveFlowRateMlPerSec?.let { "%.2f".format(it) } ?: "—"} мл/с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Последнее наблюдение: ${info.lastObservedFlowRateMlPerSec?.let { "%.2f".format(it) } ?: "—"} мл/с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Цель / факт: ${info.lastTargetMl ?: "—"} / ${info.lastActualMl ?: "—"} мл",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Длительность налива: ${info.lastPourDurationSec?.let { "%.2f".format(it) } ?: "—"} с",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        SettingsTextField(
            label = "Объём налива (целевой), мл",
            value = s.waterCalTargetMlInput,
            onValueChange = viewModel::setWaterCalTargetMlInput,
        )
        Button(
            onClick = { viewModel.startWaterCalibrationPour() },
            enabled = !s.waterCalPourBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Начать налив")
        }
        if (s.waterCalPourBusy) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        s.waterCalPourResult?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (msg.contains("заверш")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }

        Spacer(Modifier.height(20.dp))
        SettingsTextField(
            label = "Фактический объём, мл",
            value = s.waterCalActualMlInput,
            onValueChange = viewModel::setWaterCalActualMlInput,
        )
        Button(
            onClick = { viewModel.saveWaterCalibrationCoefficient() },
            enabled = !s.waterCalSaveBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить калибровку")
        }
        if (s.waterCalSaveBusy) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(strokeWidth = 2.dp)
        }

        Spacer(Modifier.height(20.dp))
        Text("Адаптация скорости", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        SettingsTextField(
            label = "Сколько последних наливов учитывать (1..20)",
            value = s.waterCalAdaptiveWindowInput,
            onValueChange = viewModel::setWaterCalAdaptiveWindowInput,
        )
        Button(
            onClick = { viewModel.recomputeWaterFlowRateFromHistory() },
            enabled = !s.waterCalRecomputeBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Пересчитать и сохранить скорость по истории")
        }
        if (s.waterCalRecomputeBusy) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(strokeWidth = 2.dp)
        }

        s.waterCalBanner?.let { banner ->
            Spacer(Modifier.height(8.dp))
            Text(
                banner,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (s.waterCalBannerIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
    }
}
