package com.viwa.android.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viwa.android.domain.model.customer.PrimaryButtonPulseStyle
import com.viwa.android.ui.theme.LocalCustomerPrimaryButtonColor
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
private fun brandFill(enabled: Boolean, pressDampen: Boolean): Color {
    val brand = LocalCustomerPrimaryButtonColor.current
    val base = if (enabled) brand else brand.copy(alpha = 0.55f)
    return if (pressDampen) lerp(base, Color.Black, 0.12f) else base
}

@Composable
private fun pulseFactor(
    style: PrimaryButtonPulseStyle,
    pulseFraction: Float,
    enabled: Boolean,
): Float {
    if (!enabled) return 1f
    return when (style) {
        PrimaryButtonPulseStyle.PulseScale -> 1f + 0.04f * sin(pulseFraction * 2 * Math.PI).toFloat()
        PrimaryButtonPulseStyle.BreathingGlow -> 1f
        PrimaryButtonPulseStyle.Shimmer -> 1f + 0.015f * sin(pulseFraction * 2 * Math.PI).toFloat()
        PrimaryButtonPulseStyle.Bounce -> {
            val t = (pulseFraction * 3f) % 1f
            1f + 0.055f * sin(t * Math.PI).toFloat()
        }
        PrimaryButtonPulseStyle.Wave ->
            1f + 0.028f * sin(pulseFraction * 2 * Math.PI).toFloat()
    }
}

@Composable
private fun waveOffsetYDp(
    style: PrimaryButtonPulseStyle,
    pulseFraction: Float,
    enabled: Boolean,
    scaleS: Float,
): Dp {
    if (!enabled || style != PrimaryButtonPulseStyle.Wave) return 0.dp
    val px = 3f * scaleS * sin(pulseFraction * 2 * Math.PI).toFloat()
    return px.dp
}

@Composable
private fun glowAlpha(style: PrimaryButtonPulseStyle, pulseFraction: Float, enabled: Boolean): Float {
    if (!enabled || style != PrimaryButtonPulseStyle.BreathingGlow) return 0f
    return 0.35f + 0.45f * (0.5f + 0.5f * sin(pulseFraction * 2 * Math.PI).toFloat())
}

/** Фазы 0.1 для пульса и шима (совместимо с любым BOM Compose). */
@Composable
private fun rememberPrimaryButtonAnimPhases(
    pulseStyle: PrimaryButtonPulseStyle,
    pulseOn: Boolean,
): Pair<Float, Float> {
    var tick by remember { mutableLongStateOf(0L) }
    val pulsePeriod =
        when (pulseStyle) {
            PrimaryButtonPulseStyle.Bounce -> 1100L
            PrimaryButtonPulseStyle.Shimmer -> 1800L
            else -> 1600L
        }
    LaunchedEffect(pulseOn) {
        if (!pulseOn) return@LaunchedEffect
        tick = System.currentTimeMillis()
        while (isActive) {
            delay(32)
            tick = System.currentTimeMillis()
        }
    }
    if (!pulseOn) return 0f to 0f
    val pulseFrac = ((tick % pulsePeriod).toFloat() / pulsePeriod.toFloat()).coerceIn(0f, 1f)
    val shimmerFrac = ((tick % 2200L).toFloat() / 2200f).coerceIn(0f, 1f)
    return pulseFrac to shimmerFrac
}

@Composable
fun ViwaPrimaryActionBar(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    pulseHint: Boolean,
    pulseStyle: PrimaryButtonPulseStyle,
    waterPourMode: Boolean,
    pressDampen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    s: Float,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

 // Анимации включаем только когда это явно разрешено (например, удержание «Налить воду»).
    val pulseOn = pulseHint && enabled && !loading
    val (pulseFraction, shimmerPhase) = rememberPrimaryButtonAnimPhases(pulseStyle, pulseOn)

    val pulseMul =
        if (pulseOn) {
            pulseFactor(pulseStyle, pulseFraction, true)
        } else {
            1f
        }
    val extraPress =
        if ((pressed || pressDampen) && enabled && !loading) {
            1.03f
        } else {
            1f
        }
    val scale = pulseMul * extraPress

    val glowA = if (pulseOn) glowAlpha(pulseStyle, pulseFraction, true) else 0f
    val topR = (24f * s).dp
    val barHeight = (120f * s).dp
    val shape = RoundedCornerShape(topStart = topR, topEnd = topR)

    val bg = brandFill(enabled, pressDampen || pressed)

    val waveY = waveOffsetYDp(pulseStyle, pulseFraction, pulseOn, s)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(barHeight)
                .scale(scale)
                .background(bg, shape)
                .clip(shape)
                .then(
                    if (waterPourMode) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled && !loading,
                            onClick = onClick,
                        )
                    },
                ),
    ) {
        if (glowA > 0.01f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val rPx = (24f * s).dp.toPx()
                            val strokeW = 3.dp.toPx()
                            onDrawBehind {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = glowA * 0.5f),
                                    style = Stroke(width = strokeW),
                                    cornerRadius = CornerRadius(rPx, rPx),
                                )
                            }
                        },
            )
        }
        if (pulseOn && pulseStyle == PrimaryButtonPulseStyle.Shimmer) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val wPx = size.width
                            val hPx = size.height
                            val bandPx = wPx * 0.35f
                            val xPx = -bandPx + (wPx + bandPx * 2f) * shimmerPhase
                            val endYPx = hPx * 0.4f
                            onDrawBehind {
                                drawRect(
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.22f),
                                                    Color.Transparent,
                                                ),
                                            start = Offset(xPx, 0f),
                                            end = Offset(xPx + bandPx, endYPx),
                                        ),
                                )
                            }
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset(y = waveY),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    Modifier.size((36f * s).dp),
                    strokeWidth = (3f * s).dp,
                    color = ViwaCustomerUiTokens.PrimaryButtonText,
                )
            } else {
                Text(
                    text = label,
                    fontSize = (48f * s).sp,
                    lineHeight = (54f * s).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ViwaCustomerUiTokens.PrimaryButtonText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = (12f * s).dp),
                )
            }
        }
    }
}

@Composable
fun ViwaPrimaryPulseMiniPreview(
    style: PrimaryButtonPulseStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val previewS = 0.36f
    val h = (120f * previewS).dp
    val w = (220f * previewS).dp
    val shape = RoundedCornerShape((24f * previewS).dp)
    Box(
        modifier =
            modifier
                .size(w, h)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Color.White.copy(alpha = 0.9f) else Color.Transparent,
                    shape = shape,
                ),
    ) {
        ViwaPrimaryActionBar(
            label = "Вода",
            enabled = true,
            loading = false,
            pulseHint = true,
            pulseStyle = style,
            waterPourMode = false,
            pressDampen = false,
            onClick = {},
            modifier = Modifier.fillMaxSize(),
            s = previewS,
        )
    }
}
