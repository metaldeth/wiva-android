package com.viwa.android.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val TAPS_REQUIRED = 5
private const val TIME_WINDOW_MS = 2000L
private const val TRIGGER_SIZE_DP = 80

/**
 * Невидимая зона в правом верхнем углу.
 * После [TAPS_REQUIRED] тапов за [TIME_WINDOW_MS] мс вызывает [onActivate].
 */
@Composable
fun ServiceMenuTrigger(
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tapTimestamps = remember { mutableListOf<Long>() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier =
                Modifier
                    .size(TRIGGER_SIZE_DP.dp)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            val now = System.currentTimeMillis()
                            tapTimestamps.removeAll { now - it > TIME_WINDOW_MS }
                            tapTimestamps.add(now)

                            if (tapTimestamps.size >= TAPS_REQUIRED) {
                                tapTimestamps.clear()
                                onActivate()
                            }
                        }
                    },
        )
    }
}
