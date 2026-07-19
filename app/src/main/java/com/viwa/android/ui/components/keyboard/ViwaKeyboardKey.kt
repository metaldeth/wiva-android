package com.viwa.android.ui.components.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ViwaKeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    emphasizedBackground: Boolean = false,
    onClick: () -> Unit,
) {
    val bg =
        if (emphasizedBackground) {
            ViwaKeyboardColors.keyModifierBackground()
        } else {
            ViwaKeyboardColors.keyBackground()
        }
    Surface(
        onClick = onClick,
        modifier = modifier.height(ViwaKeyboardSpec.KeyHeight),
        shape = RoundedCornerShape(ViwaKeyboardSpec.KeyCornerRadius),
        color = bg,
        border = BorderStroke(1.dp, ViwaKeyboardColors.keyBorder()),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style =
                    if (label.length > 2) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                color = ViwaKeyboardColors.keyLabel(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Клавиша «Ввод» / Enter — как на системной клавиатуре (акцент primary). */
@Composable
internal fun ViwaKeyboardEnterKey(
    modifier: Modifier = Modifier,
    contentDescription: String = "Ввод",
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(ViwaKeyboardSpec.KeyHeight),
        shape = RoundedCornerShape(ViwaKeyboardSpec.KeyCornerRadius),
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
internal fun ViwaKeyboardIconKey(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    emphasizedBackground: Boolean = false,
    onClick: () -> Unit,
) {
    val bg =
        if (emphasizedBackground) {
            ViwaKeyboardColors.keyModifierBackground()
        } else {
            ViwaKeyboardColors.keyBackground()
        }
    Surface(
        onClick = onClick,
        modifier = modifier.height(ViwaKeyboardSpec.KeyHeight),
        shape = RoundedCornerShape(ViwaKeyboardSpec.KeyCornerRadius),
        color = bg,
        border = BorderStroke(1.dp, ViwaKeyboardColors.keyBorder()),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = ViwaKeyboardColors.keyLabel(),
            )
        }
    }
}
