package com.wiva.android.ui.components.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class WivaAlphanumericMode {
    Letters,
    Numbers,
    Symbols,
}

private val ROW_Q = "qwertyuiop".map { it.toString() }
private val ROW_A = "asdfghjkl".map { it.toString() }
private val ROW_Z = "zxcvbnm".map { it.toString() }

/** Доп. ряд в режиме «123». */
private val ROW_NUM_TOP = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val ROW_NUM_MID =
    listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")

/** Режим «#+=». */
private val ROW_SYM_TOP = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
private val ROW_SYM_MID = listOf("_", "-", "\\", "|", "~", "<", ">", "€", "£", "¥")

/**
 * QWERTY + режимы «123» и «#+=» с цифрами и символами в духе системной клавиатуры.
 * Цвета из [androidx.compose.material3.MaterialTheme] (светлая / тёмная тема).
 *
 * [onEnter] — клавиша Enter (иконка «ввод»): отправка формы, переход строки и т.д. Один Backspace на ряд букв.
 */
@Composable
fun WivaAlphanumericKeyboard(
    modifier: Modifier = Modifier,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(WivaAlphanumericMode.Letters) }
    var capsLock by remember { mutableStateOf(false) }

    val gapH = WivaKeyboardSpec.KeyGapHorizontal
    val gapV = WivaKeyboardSpec.KeyGapVertical

    fun emitLetter(s: String) {
        val c = s.firstOrNull() ?: return
        val out =
            if (capsLock) {
                c.uppercaseChar().toString()
            } else {
                c.lowercaseChar().toString()
            }
        onInput(out)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gapV),
    ) {
        when (mode) {
            WivaAlphanumericMode.Letters -> {
                KeyboardRow(gapH) {
                    for (ch in ROW_Q) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { emitLetter(ch) },
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = WivaKeyboardSpec.LetterRowStagger),
                    horizontalArrangement = Arrangement.spacedBy(gapH),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (ch in ROW_A) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { emitLetter(ch) },
                        )
                    }
                }
                KeyboardRow(gapH) {
                    WivaKeyboardIconKey(
                        imageVector = Icons.Filled.KeyboardCapslock,
                        contentDescription = if (capsLock) "Выключить Caps Lock" else "Включить Caps Lock",
                        modifier = Modifier.weight(1.15f),
                        emphasizedBackground = capsLock,
                        onClick = { capsLock = !capsLock },
                    )
                    for (ch in ROW_Z) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { emitLetter(ch) },
                        )
                    }
                    WivaKeyboardIconKey(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Стереть",
                        modifier = Modifier.weight(1.15f),
                        onClick = onBackspace,
                    )
                }
                KeyboardRow(gapH) {
                    WivaKeyboardKey(
                        label = "?123",
                        modifier = Modifier.weight(1.15f),
                        emphasizedBackground = true,
                        onClick = { mode = WivaAlphanumericMode.Numbers },
                    )
                    WivaKeyboardKey(
                        label = ",",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput(",") },
                    )
                    WivaKeyboardKey(
                        label = "Пробел",
                        modifier = Modifier.weight(4f),
                        onClick = { onInput(" ") },
                    )
                    WivaKeyboardKey(
                        label = ".",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput(".") },
                    )
                    WivaKeyboardEnterKey(
                        modifier = Modifier.weight(1.15f),
                        contentDescription = "Ввод",
                        onClick = onEnter,
                    )
                }
            }

            WivaAlphanumericMode.Numbers -> {
                KeyboardRow(gapH) {
                    for (ch in ROW_NUM_TOP) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { onInput(ch) },
                        )
                    }
                }
                KeyboardRow(gapH) {
                    for (ch in ROW_NUM_MID) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { onInput(ch) },
                        )
                    }
                }
                KeyboardRow(gapH) {
                    WivaKeyboardKey(
                        label = "#+=",
                        modifier = Modifier.weight(1.2f),
                        emphasizedBackground = true,
                        onClick = { mode = WivaAlphanumericMode.Symbols },
                    )
                    WivaKeyboardKey(
                        label = ".",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput(".") },
                    )
                    WivaKeyboardKey(
                        label = ",",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput(",") },
                    )
                    WivaKeyboardKey(
                        label = "?",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput("?") },
                    )
                    WivaKeyboardKey(
                        label = "!",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput("!") },
                    )
                    WivaKeyboardEnterKey(
                        modifier = Modifier.weight(1.2f),
                        contentDescription = "Ввод",
                        onClick = onEnter,
                    )
                    WivaKeyboardIconKey(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Стереть",
                        modifier = Modifier.weight(1.2f),
                        onClick = onBackspace,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
                    WivaKeyboardKey(
                        label = "ABC",
                        modifier = Modifier.weight(1f),
                        emphasizedBackground = true,
                        onClick = { mode = WivaAlphanumericMode.Letters },
                    )
                }
            }

            WivaAlphanumericMode.Symbols -> {
                KeyboardRow(gapH) {
                    for (ch in ROW_SYM_TOP) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { onInput(ch) },
                        )
                    }
                }
                KeyboardRow(gapH) {
                    for (ch in ROW_SYM_MID) {
                        WivaKeyboardKey(
                            label = ch,
                            modifier = Modifier.weight(1f),
                            onClick = { onInput(ch) },
                        )
                    }
                }
                KeyboardRow(gapH) {
                    WivaKeyboardKey(
                        label = "123",
                        modifier = Modifier.weight(1.2f),
                        emphasizedBackground = true,
                        onClick = { mode = WivaAlphanumericMode.Numbers },
                    )
                    WivaKeyboardKey(
                        label = "`",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput("`") },
                    )
                    WivaKeyboardKey(
                        label = "'",
                        modifier = Modifier.weight(1f),
                        onClick = { onInput("'") },
                    )
                    WivaKeyboardEnterKey(
                        modifier = Modifier.weight(1.2f),
                        contentDescription = "Ввод",
                        onClick = onEnter,
                    )
                    Spacer(Modifier.weight(1.2f))
                    WivaKeyboardIconKey(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Стереть",
                        modifier = Modifier.weight(1.2f),
                        onClick = onBackspace,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gapH)) {
                    WivaKeyboardKey(
                        label = "ABC",
                        modifier = Modifier.weight(1f),
                        emphasizedBackground = true,
                        onClick = { mode = WivaAlphanumericMode.Letters },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    gapH: androidx.compose.ui.unit.Dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gapH),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
