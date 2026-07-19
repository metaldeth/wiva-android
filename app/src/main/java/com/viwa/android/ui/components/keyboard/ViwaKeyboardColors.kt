package com.viwa.android.ui.components.keyboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Цвета клавиш из текущей темы (светлая / тёмная): фон чуть выше [androidx.compose.material3.ColorScheme.surface],
 * лёгкая обводка как у системной клавиатуры.
 */
internal object ViwaKeyboardColors {
    @Composable
    fun keyBackground(): Color {
        val cs = MaterialTheme.colorScheme
        return cs.surfaceVariant
    }

    @Composable
    fun keyBorder(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    @Composable
    fun keyLabel(): Color = MaterialTheme.colorScheme.onSurface

 /** Подсветка модификаторов (Shift, ?123, ABC) — лёгкий оттенок primary, читается в светлой и тёмной теме. */
    @Composable
    fun keyModifierBackground(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
}
