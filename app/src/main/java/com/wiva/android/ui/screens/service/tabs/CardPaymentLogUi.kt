package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wiva.android.services.payment.CardPaymentLogEntry
import com.wiva.android.services.payment.CardPaymentLogLane
import com.wiva.android.ui.screens.service.serviceMenuSp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOG_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

@Composable
fun CardPaymentLogPanel(entries: List<CardPaymentLogEntry>) {
    val reversed = remember(entries) { entries.asReversed() }
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (reversed.isEmpty()) {
            Text(
                "Нет записей. Запустите тест или включите mock — строки появятся здесь.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        reversed.forEach { entry ->
            val ex = expanded[entry.id] == true
            CardPaymentLogRow(
                entry = entry,
                expanded = ex,
                onToggle = { expanded[entry.id] = !ex },
            )
        }
    }
}

@Composable
private fun CardPaymentLogRow(
    entry: CardPaymentLogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val lanePalette = laneColorsForLane(entry.lane)
    val badgeBg = lanePalette.foreground.copy(alpha = 0.14f)
    val surfaceTint =
        lanePalette.surface.copy(
            alpha = if (expanded) 0.92f else 0.74f,
        )
    val textPrimary =
        if (entry.isError) {
            scheme.error
        } else {
            lanePalette.foreground
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceTint)
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = laneShortLabel(entry.lane),
                    color = lanePalette.foreground,
                    fontSize = serviceMenuSp(10),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier =
                        Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = LOG_TIME_FORMAT.format(Date(entry.timestampMillis)),
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.provider,
                    fontSize = serviceMenuSp(11),
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(0.38f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entry.message,
                    fontSize = serviceMenuSp(12),
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier.weight(0.62f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                )
            }
            if (!expanded && entry.detail.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Нажмите карточку для подробностей",
                    fontSize = serviceMenuSp(10),
                    color = scheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = lanePalette.foreground.copy(alpha = 0.22f))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = laneLongDescriptionRu(entry.lane),
                    fontSize = serviceMenuSp(11),
                    color = lanePalette.foreground.copy(alpha = 0.85f),
                )
                if (entry.detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.detail,
                        fontSize = serviceMenuSp(11),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = serviceMenuSp(16),
                        color = if (entry.isError) scheme.error else scheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Исход: ${if (entry.isError) "ошибка / неуспех" else "информация / успех"}",
                    fontSize = serviceMenuSp(10),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun laneShortLabel(lane: CardPaymentLogLane): String =
    when (lane) {
        CardPaymentLogLane.ToTerminal -> "OUT"
        CardPaymentLogLane.FromTerminal -> "IN"
        CardPaymentLogLane.Mock -> "MOCK"
        CardPaymentLogLane.System -> "SYS"
    }

internal fun laneLongDescriptionRu(lane: CardPaymentLogLane): String =
    when (lane) {
        CardPaymentLogLane.ToTerminal -> "Направление: запрос к терминалу / контроллеру"
        CardPaymentLogLane.FromTerminal -> "Направление: ответ или статус от терминала"
        CardPaymentLogLane.Mock -> "Сценарий без физического платёжника"
        CardPaymentLogLane.System -> "Сервисное действие или диагностика"
    }

private data class LaneColors(
    val foreground: Color,
    val surface: Color,
)

@Composable
private fun laneColorsForLane(lane: CardPaymentLogLane): LaneColors {
    val scheme = MaterialTheme.colorScheme
    return when (lane) {
        CardPaymentLogLane.ToTerminal ->
            LaneColors(foreground = scheme.primary, surface = scheme.primaryContainer)
        CardPaymentLogLane.FromTerminal ->
            LaneColors(foreground = scheme.tertiary, surface = scheme.tertiaryContainer)
        CardPaymentLogLane.Mock ->
            LaneColors(foreground = scheme.secondary, surface = scheme.secondaryContainer)
        CardPaymentLogLane.System ->
            LaneColors(foreground = scheme.outline, surface = scheme.surfaceVariant)
    }
}
