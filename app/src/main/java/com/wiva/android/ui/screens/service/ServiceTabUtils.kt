package com.wiva.android.ui.screens.service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val host = LocalServiceKeyboardHost.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.background,
        contentColor = scheme.onBackground,
    ) {
 // Фоновый слой: тап по «пустому» месту не должен совпадать по z-order с полями,
 // иначе detectTapGestures на том же Column, что и OutlinedTextField, даёт open+dismiss в один жест.
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                host.dismiss()
                                ViewCompat.requestApplyInsets(view)
                            },
                        )
                    },
            )
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
 /** Идентичность поля для сессии клавиатуры (та же вкладка + разные поля). */
    fieldKey: Any = label,
    maxLength: Int = 512,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    testTag: String? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val host = LocalServiceKeyboardHost.current
    val session = host.session
    val isKeyboardActive = session?.key == fieldKey
    val latestValue by rememberUpdatedState(value)
    val scheme = MaterialTheme.colorScheme

    val kind: ServiceKeyboardKind =
        when (keyboardType) {
            KeyboardType.Number, KeyboardType.Phone -> ServiceKeyboardKind.Numeric(showDecimalPoint = false)
            KeyboardType.Decimal -> ServiceKeyboardKind.Numeric(showDecimalPoint = true)
            else -> ServiceKeyboardKind.Alphanumeric
        }

    fun openKeyboard() {
        keyboardController?.hide()
        host.open(
            ServiceKeyboardSession(
                key = fieldKey,
                kind = kind,
                getValue = { latestValue },
                onValueChange = onValueChange,
                maxLength = maxLength,
            ),
        )
    }

    val overlayInteraction = remember { MutableInteractionSource() }

 // OutlinedTextField перехватывает жесты внутри decorationBox — modifier.clickable на поле часто не срабатывает.
 // Прозрачный слой поверх гарантирует открытие нижней панели по тапу.
    Box(modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = true,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            visualTransformation = visualTransformation,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            openKeyboard()
                        }
                    },
            singleLine = singleLine,
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Unspecified),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = if (isKeyboardActive) scheme.primary else scheme.outline,
                    focusedLabelColor = scheme.primary,
                    unfocusedLabelColor =
                        if (isKeyboardActive) {
                            scheme.primary
                        } else {
                            scheme.onSurfaceVariant
                        },
                    focusedLeadingIconColor = scheme.onSurfaceVariant,
                    unfocusedLeadingIconColor = scheme.onSurfaceVariant,
                    focusedTrailingIconColor = scheme.onSurfaceVariant,
                    unfocusedTrailingIconColor = scheme.onSurfaceVariant,
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = overlayInteraction,
                    indication = null,
                    onClick = { openKeyboard() },
                ),
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
    }
}
