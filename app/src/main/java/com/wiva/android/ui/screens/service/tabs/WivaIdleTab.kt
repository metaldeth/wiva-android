package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.ui.screens.customer.IDLE_VIDEOS
import com.wiva.android.ui.screens.idle.IdleVideoViewModel

/**
 * Вкладка «Ожидание» в сервисном меню.
 * Аналог `IdleTab.tsx`.
 * Позволяет включать/выключать каждое из 14 видео скринсейвера
 * и полностью отключить режим ожидания.
 */
@Composable
fun WivaIdleTab(
 /** Тот же экземпляр, что в MainActivity — иначе настройки пишутся в другой VM и idle не реагирует. */
    idleViewModel: IdleVideoViewModel =
        hiltViewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity),
) {
    val enabledVideoIds by idleViewModel.enabledVideoIds.collectAsStateWithLifecycle()
    val allDisabled = enabledVideoIds.isEmpty()
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.background,
        contentColor = scheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
        Text(
            text = "Видео режима ожидания",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

 // Кнопки «Включить все» / «Выключить все»
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { idleViewModel.enableAllVideos() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Включить все")
            }
            OutlinedButton(
                onClick = { idleViewModel.disableAllVideos() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Выключить все")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

 // 14 чекбоксов — по одному на каждое видео
        IDLE_VIDEOS.forEach { item ->
            val checked = item.id in enabledVideoIds
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        idleViewModel.toggleVideo(item.id, isChecked)
                    },
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

 // Кнопка «Отключить видео ожидания полностью» — визуально отличается
        Button(
            onClick = { idleViewModel.disableAllVideos() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allDisabled) MaterialTheme.colorScheme.surfaceVariant
                else Color(0xFFD32F2F),
                contentColor = if (allDisabled) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.White,
            ),
        ) {
            Text(if (allDisabled) "Видео ожидания отключено" else "Отключить видео ожидания полностью")
        }
        }
    }
}
