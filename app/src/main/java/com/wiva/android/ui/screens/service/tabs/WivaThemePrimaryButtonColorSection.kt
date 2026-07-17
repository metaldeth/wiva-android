package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Настройка цвета основной кнопки («Налить воду» / оплата) по каналам RGB.
 *
 * Практики Material / UX: дискретные шаги 0–255, подписи значений, предпросмотр, сохранение при отпускании
 * ползунка (меньше записей в БД), явный сброс к умолчанию.
 */
@Composable
fun WivaThemePrimaryButtonColorSection(
    argb: Int,
    onRgbPreview: (r: Int, g: Int, b: Int) -> Unit,
    onSliderFinished: () -> Unit,
    onReset: () -> Unit,
) {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val preview = Color(argb)
    val hexRgb = "#%02X%02X%02X".format(r, g, b)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Цвет кнопки (налив / оплата)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Ползунки RGB, сохранение при отпускании. На экране напитков цвет обновляется сразу.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(preview)
                    .semantics { contentDescription = "Предпросмотр цвета основной кнопки" },
        )
        Text(
            text = hexRgb,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RgbChannelSlider(
            label = "Красный",
            value = r,
            accent = Color(0xFFE53935),
            contentDescription = "Красный канал RGB",
            onPreview = { newR -> onRgbPreview(newR, g, b) },
            onFinished = onSliderFinished,
        )
        RgbChannelSlider(
            label = "Зелёный",
            value = g,
            accent = Color(0xFF43A047),
            contentDescription = "Зелёный канал RGB",
            onPreview = { newG -> onRgbPreview(r, newG, b) },
            onFinished = onSliderFinished,
        )
        RgbChannelSlider(
            label = "Синий",
            value = b,
            accent = Color(0xFF1E88E5),
            contentDescription = "Синий канал RGB",
            onPreview = { newB -> onRgbPreview(r, g, newB) },
            onFinished = onSliderFinished,
        )
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Сбросить к цвету по умолчанию")
        }
    }
}

/**
 * Выбор RGB для ленты Flow-станции (SetFlowRgb 0xD2): предпросмотр при движении ползунков,
 * сохранение в конфиг и отправка на контроллер при отпускании — как у цвета кнопки в теме.
 */
@Composable
fun WivaFlowStripRgbSection(
    argb: Int,
    onRgbPreview: (r: Int, g: Int, b: Int) -> Unit,
    onSliderFinished: () -> Unit,
    onReset: () -> Unit,
) {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val preview = Color(argb)
    val hexRgb = "#%02X%02X%02X".format(r, g, b)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Цвет RGB-ленты Flow",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            "Сохранение при отпускании ползунка; на контроллер — SetFlowRgb (0xD2), как у тестовой кнопки в «Дебаг контроллера».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(preview)
                    .semantics { contentDescription = "Предпросмотр цвета RGB-ленты Flow" },
        )
        Text(
            text = hexRgb,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RgbChannelSlider(
            label = "Красный",
            value = r,
            accent = Color(0xFFE53935),
            contentDescription = "Красный канал RGB ленты",
            onPreview = { newR -> onRgbPreview(newR, g, b) },
            onFinished = onSliderFinished,
        )
        RgbChannelSlider(
            label = "Зелёный",
            value = g,
            accent = Color(0xFF43A047),
            contentDescription = "Зелёный канал RGB ленты",
            onPreview = { newG -> onRgbPreview(r, newG, b) },
            onFinished = onSliderFinished,
        )
        RgbChannelSlider(
            label = "Синий",
            value = b,
            accent = Color(0xFF1E88E5),
            contentDescription = "Синий канал RGB ленты",
            onPreview = { newB -> onRgbPreview(r, g, newB) },
            onFinished = onSliderFinished,
        )
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Сбросить к 128,0,255")
        }
    }
}

@Composable
private fun RgbChannelSlider(
    label: String,
    value: Int,
    accent: Color,
    contentDescription: String,
    onPreview: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(min = 88.dp),
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onPreview(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            steps = 254,
            onValueChangeFinished = onFinished,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
            colors =
                SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                ),
        )
    }
}
