package com.viwa.android.ui.screens.customer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.viwa.android.ui.system.DialogWindowImmersiveSideEffect

@Composable
fun InvalidSubscriptionCardDialog(
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = true,
            ),
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurface,
        title = {
            DialogWindowImmersiveSideEffect()
            Text(
                text = "Карта отсканирована, но с ней что-то не так",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "Попробуйте отсканировать карту ещё раз или обратитесь к администратору",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        },
    )
}
