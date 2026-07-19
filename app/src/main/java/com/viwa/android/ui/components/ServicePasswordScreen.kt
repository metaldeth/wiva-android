package com.viwa.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.viwa.android.ui.components.keyboard.ViwaAlphanumericKeyboard
import kotlinx.coroutines.delay

private const val SERVICE_PASSWORD = "studio"

/** Если за это время пользователь ни разу не нажимал клавиши пароля — экран закрывается. */
private const val AUTO_DISMISS_IF_NO_INPUT_MS = 10_000L

/**
 * Полноэкранный ввод пароля сервисного меню **без системной клавиатуры** — только своя клавиатура,
 * чтобы не тянуть IME и не ловить баги с нижним отступом ([imePadding]) на старых API.
 */
@Composable
fun ServicePasswordScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
 /** Был ввод с клавиатуры (даже если потом стёрли или ошибка «неверный пароль»). */
    var hadKeyboardInput by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val latestHadKeyboardInput = rememberUpdatedState(hadKeyboardInput)

    LaunchedEffect(Unit) {
        delay(AUTO_DISMISS_IF_NO_INPUT_MS)
        if (!latestHadKeyboardInput.value) {
            onDismiss()
        }
    }

    fun validate() {
        if (password.equals(SERVICE_PASSWORD, ignoreCase = true)) {
            password = ""
            errorMessage = null
            onSuccess()
        } else {
            errorMessage = "Неверный пароль"
            password = ""
        }
    }

    fun appendChar(c: String) {
        hadKeyboardInput = true
        if (password.length < 128) {
            password += c
            errorMessage = null
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.background,
        contentColor = scheme.onBackground,
    ) {
 // Как у прежнего AlertDialog: dismissOnBackPress = false
        BackHandler {}

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "Сервисное меню",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (password.isEmpty()) {
                    Text(
                        text = "Введите пароль",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground.copy(alpha = 0.38f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                } else {
                    val display = if (passwordVisible) password else "•".repeat(password.length)
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector =
                            if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                        contentDescription =
                            if (passwordVisible) {
                                "Скрыть пароль"
                            } else {
                                "Показать пароль"
                            },
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                ViwaAlphanumericKeyboard(
                    modifier = Modifier.fillMaxWidth(),
                    onInput = { appendChar(it) },
                    onBackspace = {
                        hadKeyboardInput = true
                        if (password.isNotEmpty()) {
                            password = password.dropLast(1)
                            errorMessage = null
                        }
                    },
                    onEnter = {
                        hadKeyboardInput = true
                        validate()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            password = ""
                            errorMessage = null
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = { validate() },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text("Войти")
                    }
                }
            }
        }
    }
}
