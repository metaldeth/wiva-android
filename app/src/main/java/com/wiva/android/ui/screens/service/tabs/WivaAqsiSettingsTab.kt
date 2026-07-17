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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.ui.screens.service.SettingsColumn
import com.wiva.android.ui.screens.service.SettingsTextField

@Composable
fun WivaAqsiSettingsTab(
    viewModel: WivaAqsiSettingsViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsColumn {
        Text("Новый считыватель aQsi", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Адрес подключения к новому считывателю. Тест соединения проверяет только доступность TCP и не запускает оплату.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        SettingsTextField(
            label = "Host / IP ридера",
            value = ui.host,
            onValueChange = viewModel::setHost,
            keyboardType = KeyboardType.Uri,
            fieldKey = "aqsi_host",
        )
        SettingsTextField(
            label = "Порт TCP",
            value = ui.portText,
            onValueChange = viewModel::setPortText,
            placeholder = "16107",
            keyboardType = KeyboardType.Number,
            fieldKey = "aqsi_port",
        )
        SettingsTextField(
            label = "Таймаут, сек",
            value = ui.timeoutSecText,
            onValueChange = viewModel::setTimeoutSecText,
            placeholder = "15",
            keyboardType = KeyboardType.Number,
            fieldKey = "aqsi_timeout",
        )

        Button(
            onClick = { viewModel.save() },
            enabled = !ui.isBusy && !ui.tcpTestBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { viewModel.testTcpConnection() },
                enabled = !ui.isBusy && !ui.tcpTestBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Тест соединения (TCP)")
            }
            if (ui.tcpTestBusy) {
                Spacer(Modifier.width(10.dp))
                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        if (ui.isBusy) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Сохранение…", style = MaterialTheme.typography.bodySmall)
            }
        }

        ui.banner?.let { msg ->
            Spacer(Modifier.height(10.dp))
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

        ui.tcpTestBanner?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                "TCP: $msg",
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (ui.tcpTestIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
            )
        }
    }
}
