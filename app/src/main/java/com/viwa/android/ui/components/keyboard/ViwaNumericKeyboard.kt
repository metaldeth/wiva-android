package com.viwa.android.ui.components.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Цифровая клавиатура (как на телефоне / PIN): 0–9, опционально «.», Backspace.
 * [onEnter], если задан — показывается клавиша Enter (как на [ViwaAlphanumericKeyboard]).
 * Стиль из [MaterialTheme] — одинаково в светлой и тёмной теме.
 */
@Composable
fun ViwaNumericKeyboard(
    modifier: Modifier = Modifier,
    showDecimalPoint: Boolean = false,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: (() -> Unit)? = null,
) {
    val gapH = ViwaKeyboardSpec.KeyGapHorizontal
    val gapV = ViwaKeyboardSpec.KeyGapVertical

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gapV),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("1", "2", "3")) {
                ViwaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("4", "5", "6")) {
                ViwaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("7", "8", "9")) {
                ViwaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            if (showDecimalPoint) {
                ViwaKeyboardKey(
                    label = ".",
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(".") },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            ViwaKeyboardKey(
                label = "0",
                modifier = Modifier.weight(1f),
                onClick = { onKey("0") },
            )
            if (onEnter != null) {
                ViwaKeyboardEnterKey(
                    modifier = Modifier.weight(1f),
                    contentDescription = "Ввод",
                    onClick = onEnter,
                )
            }
            ViwaKeyboardIconKey(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Стереть",
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
            )
        }
    }
}
