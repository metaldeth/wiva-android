package com.viwa.android.ui.screens.service

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Блок OTA: хост, проверка, загрузка и установка APK. */
@Composable
fun ViwaUpdaterSection(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    val progress by viewModel.updateInstallProgress.collectAsStateWithLifecycle()
    var updateHost by remember(state.updateHost) { mutableStateOf(state.updateHost) }
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.background,
        contentColor = scheme.onBackground,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
        ) {
        Text(
            "Обновления",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        InfoRow("Текущая версия", state.currentVersion)
        Spacer(Modifier.height(16.dp))
        SettingsTextField(
            label = "URL сервера обновлений",
            value = updateHost,
            onValueChange = { updateHost = it },
            placeholder = "http://83.166.246.158:9083",
        )
        Button(onClick = { viewModel.setUpdateHost(updateHost) }) {
            Text("Сохранить")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.checkForUpdates() },
            enabled = !state.isCheckingUpdate && !state.isInstalling,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Проверить обновления")
        }
        if (state.isCheckingUpdate) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Проверка обновлений...",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        state.updateCheckError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.isUpToDate && !state.isCheckingUpdate) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Установлена актуальная версия",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        state.availableUpdate?.let { update ->
            Spacer(Modifier.height(16.dp))
            Text(
                "Доступна версия: ${update.version}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (update.changelog.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    update.changelog,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.installUpdate(update) },
                enabled = !state.isInstalling,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Установить")
            }
            if (state.isInstalling) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Загрузка...",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        progress?.let { p ->
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { p.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val downloaded = formatBytes(p.bytesDownloaded)
            val totalPart =
                if (p.totalBytes > 0) {
                    " / ${formatBytes(p.totalBytes)} (${(p.progress * 100).toInt()}%)"
                } else {
                    ""
                }
            Text(
                "$downloaded$totalPart",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        }
    }
}

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_048_576 -> "%.1f МБ".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f КБ".format(bytes / 1_024.0)
        else -> "$bytes Б"
    }
