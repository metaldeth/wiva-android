package com.wiva.android.ui.components.keyboard

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
 * [onEnter], если задан — показывается клавиша Enter (как на [WivaAlphanumericKeyboard]).
 * Стиль из [MaterialTheme] — одинаково в светлой и тёмной теме.
 */
@Composable
fun WivaNumericKeyboard(
    modifier: Modifier = Modifier,
    showDecimalPoint: Boolean = false,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: (() -> Unit)? = null,
) {
    val gapH = WivaKeyboardSpec.KeyGapHorizontal
    val gapV = WivaKeyboardSpec.KeyGapVertical

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gapV),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("1", "2", "3")) {
                WivaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("4", "5", "6")) {
                WivaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            for (d in listOf("7", "8", "9")) {
                WivaKeyboardKey(
                    label = d,
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(d) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
            if (showDecimalPoint) {
                WivaKeyboardKey(
                    label = ".",
                    modifier = Modifier.weight(1f),
                    onClick = { onKey(".") },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            WivaKeyboardKey(
                label = "0",
                modifier = Modifier.weight(1f),
                onClick = { onKey("0") },
            )
            if (onEnter != null) {
                WivaKeyboardEnterKey(
                    modifier = Modifier.weight(1f),
                    contentDescription = "Ввод",
                    onClick = onEnter,
                )
            }
            WivaKeyboardIconKey(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Стереть",
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
            )
        }
    }
}
