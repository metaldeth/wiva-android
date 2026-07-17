package com.wiva.android.ui.screens.service.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wiva.android.ui.screens.service.SettingsColumn
import com.wiva.android.ui.screens.service.SettingsTextField

/**
 * Два тестовых поля: цифры + десятичная точка и произвольный текст — общий хост клавиатуры сервисного меню, без IME.
 */
@Composable
fun WivaKeyboardTestTab() {
    var numericValue by remember { mutableStateOf("") }
    var textValue by remember { mutableStateOf("") }

    SettingsColumn {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Клавиатура (тест)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Нажмите поле — снизу появится встроенная клавиатура. Системная клавиатура не используется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsTextField(
                label = "Числовое (PIN / сумма)",
                value = numericValue,
                onValueChange = { numericValue = it },
                placeholder = "Только цифры и точка",
                keyboardType = KeyboardType.Decimal,
                fieldKey = "keyboard_test_numeric",
                maxLength = 64,
            )
            SettingsTextField(
                label = "Текст (QWERTY)",
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = "Буквы, цифры, символы",
                keyboardType = KeyboardType.Text,
                fieldKey = "keyboard_test_text",
                maxLength = 512,
            )
            Text(
                "Enter на клавиатуре — свернуть панель.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
