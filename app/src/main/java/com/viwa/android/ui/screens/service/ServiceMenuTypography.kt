package com.viwa.android.ui.screens.service

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Единый множитель размера текста в сервисном меню: вложенная [Typography] и явные [serviceMenuSp].
 * Поднять/опустить масштаб — только здесь.
 */
const val SERVICE_MENU_TEXT_SCALE = 1.24f

/** Масштабирует все стили Material 3 (размер строки — пропорционально). */
fun Typography.scaled(scale: Float = SERVICE_MENU_TEXT_SCALE): Typography =
    Typography(
        displayLarge = displayLarge.scaled(scale),
        displayMedium = displayMedium.scaled(scale),
        displaySmall = displaySmall.scaled(scale),
        headlineLarge = headlineLarge.scaled(scale),
        headlineMedium = headlineMedium.scaled(scale),
        headlineSmall = headlineSmall.scaled(scale),
        titleLarge = titleLarge.scaled(scale),
        titleMedium = titleMedium.scaled(scale),
        titleSmall = titleSmall.scaled(scale),
        bodyLarge = bodyLarge.scaled(scale),
        bodyMedium = bodyMedium.scaled(scale),
        bodySmall = bodySmall.scaled(scale),
        labelLarge = labelLarge.scaled(scale),
        labelMedium = labelMedium.scaled(scale),
        labelSmall = labelSmall.scaled(scale),
    )

private fun TextStyle.scaled(scale: Float): TextStyle {
    val newFontSize = fontSize * scale
    val newLineHeight =
        if (lineHeight == TextUnit.Unspecified) {
            TextUnit.Unspecified
        } else {
            lineHeight * scale
        }
    return copy(fontSize = newFontSize, lineHeight = newLineHeight)
}

/** Явный размер в sp с тем же множителем, что и тема (моноширинные логи и т.п.). */
fun serviceMenuSp(base: Int): TextUnit = (base * SERVICE_MENU_TEXT_SCALE).sp
