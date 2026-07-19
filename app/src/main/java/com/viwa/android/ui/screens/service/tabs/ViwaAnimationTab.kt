package com.viwa.android.ui.screens.service.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.viwa.android.domain.model.customer.PrimaryButtonPulseStyle
import com.viwa.android.ui.screens.customer.ViwaPrimaryPulseMiniPreview
import com.viwa.android.ui.screens.service.ServiceUiState
import com.viwa.android.ui.screens.service.ServiceViewModel
import com.viwa.android.ui.screens.service.SettingsColumn

@Composable
fun ViwaAnimationTab(
    state: ServiceUiState,
    viewModel: ServiceViewModel,
) {
    SettingsColumn {
        Text("Анимация основной кнопки", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Экран напитков: цвет кнопки — #C500FF. Эффекты включаются только во время удержания «Налить воду». В состоянии покоя анимации отключены полностью.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButtonPulseStyle.entries.forEach { style ->
            val selected = state.primaryButtonPulseStyle == style
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setPrimaryButtonPulseStyle(style) }
                        .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { viewModel.setPrimaryButtonPulseStyle(style) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(style.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        style.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ViwaPrimaryPulseMiniPreview(
                        style = style,
                        selected = selected,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
