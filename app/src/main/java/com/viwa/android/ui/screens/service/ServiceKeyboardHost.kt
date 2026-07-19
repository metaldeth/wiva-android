package com.viwa.android.ui.screens.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.viwa.android.ui.components.keyboard.ViwaAlphanumericKeyboard
import com.viwa.android.ui.components.keyboard.ViwaNumericKeyboard

/** Режим встроенной клавиатуры в сервисном меню. */
sealed class ServiceKeyboardKind {
    data class Numeric(val showDecimalPoint: Boolean) : ServiceKeyboardKind()

    data object Alphanumeric : ServiceKeyboardKind()
}

/**
 * Одна активная сессия: какое поле редактируем и как читать/писать строку.
 * [key] — идентичность поля (смена при тапе на другое поле открывает новую сессию).
 */
data class ServiceKeyboardSession(
    val key: Any,
    val kind: ServiceKeyboardKind,
    val getValue: () -> String,
    val onValueChange: (String) -> Unit,
    val maxLength: Int = 512,
)

/**
 * Хост нижней панели клавиатуры: [open] / [dismiss].
 * Предоставляется через [LocalServiceKeyboardHost] внутри [ViwaServiceMenuContent].
 */
class ServiceKeyboardHostController {
    var session: ServiceKeyboardSession? by mutableStateOf(null)
        private set

    fun open(s: ServiceKeyboardSession) {
        session = s
    }

    fun dismiss() {
        session = null
    }
}

internal val LocalServiceKeyboardHost =
    compositionLocalOf<ServiceKeyboardHostController> {
        error("LocalServiceKeyboardHost: только внутри сервисного меню (ViwaServiceMenuContent)")
    }

@Composable
internal fun ServiceKeyboardBottomPanel(
    controller: ServiceKeyboardHostController,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = controller.session != null,
        modifier = modifier.fillMaxWidth(),
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        val s = controller.session ?: return@AnimatedVisibility
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        controller.dismiss()
                    },
                ) {
                    Text("Свернуть")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            when (val kind = s.kind) {
                is ServiceKeyboardKind.Numeric -> {
                    ViwaNumericKeyboard(
                        modifier = Modifier.fillMaxWidth(),
                        showDecimalPoint = kind.showDecimalPoint,
                        onKey = { key ->
                            if (kind.showDecimalPoint || key != ".") {
                                val cur = s.getValue()
                                if (cur.length >= s.maxLength) return@ViwaNumericKeyboard
                                val next = cur + key
                                s.onValueChange(next)
                            }
                        },
                        onBackspace = {
                            val cur = s.getValue()
                            if (cur.isNotEmpty()) s.onValueChange(cur.dropLast(1))
                        },
                        onEnter = { controller.dismiss() },
                    )
                }
                ServiceKeyboardKind.Alphanumeric -> {
                    ViwaAlphanumericKeyboard(
                        modifier = Modifier.fillMaxWidth(),
                        onInput = { ch ->
                            val cur = s.getValue()
                            if (cur.length < s.maxLength) s.onValueChange(cur + ch)
                        },
                        onBackspace = {
                            val cur = s.getValue()
                            if (cur.isNotEmpty()) s.onValueChange(cur.dropLast(1))
                        },
                        onEnter = { controller.dismiss() },
                    )
                }
            }
        }
    }
}
