package com.wiva.android.ui.screens.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiva.android.R
import com.wiva.android.ui.components.QRCodeView
import com.wiva.android.ui.theme.MontserratFamily

private val FlowBg       = Color(0xFF121212)
private val FlowBgCard   = Color(0xFF1D1D1D)
private val FlowAccent   = Color(0xFF7F5AF0)
private val FlowAccentBg = Color(0xFF2D2257)
private val FlowWhite    = Color(0xFFF5F5F7)
private val FlowGray     = Color(0xFFB0B0B0)

private data class Benefit(val icon: ImageVector, val label: String)

private val benefits = listOf(
    Benefit(Icons.Rounded.WaterDrop, "1 литр каждый день"),
    Benefit(Icons.Rounded.Bolt,      "Витамины B — фокус и энергия"),
    Benefit(Icons.Rounded.Favorite,  "Без сахара и калорий"),
    Benefit(Icons.Rounded.Spa,       "Натуральный вкус"),
)

/**
 * Экран предложения бесплатного напитка — стиль FLOW Dark Premium.
 * Тёмный фон #121212, фиолетовый акцент #7F5AF0, лайм/вода как hero-визуал.
 */
@Composable
fun FreeDrinkOfferScreen(
    onClose: () -> Unit,
    viewModel: FreeDrinkOfferViewModel = hiltViewModel(),
) {
    val qrUrl by viewModel.qrUrl.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowBg)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().shader = android.graphics.RadialGradient(
                            size.width * 0.72f, size.height * 0.45f,
                            size.width * 0.48f,
                            intArrayOf(0x44_7F5AF0.toInt(), 0x00_000000),
                            floatArrayOf(0f, 1f),
                            android.graphics.Shader.TileMode.CLAMP,
                        )
                    }
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LeftPanel(
                qrUrl = qrUrl,
                modifier = Modifier.fillMaxHeight().weight(0.43f),
            )
            RightPanel(
                modifier = Modifier.fillMaxHeight().weight(0.57f),
            )
        }

 // Кнопка закрытия — правый верхний угол
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .semantics { contentDescription = "Закрыть"; role = Role.Button },
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = FlowWhite,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LeftPanel(qrUrl: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FlowBgCard)
            .padding(horizontal = 30.dp, vertical = 28.dp),
    ) {
 // Заголовок
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = FlowWhite)) { append("Попробуй ") }
                withStyle(SpanStyle(color = FlowAccent)) { append("бесплатно") }
            },
            style = TextStyle(
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Чистый вкус фруктов и быстрое восстановление",
            modifier = Modifier.fillMaxWidth(0.80f),
            style = TextStyle(
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = FlowGray,
            ),
        )

        Spacer(Modifier.height(22.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            benefits.forEach { BenefitRow(it.icon, it.label) }
        }

        Spacer(Modifier.weight(1f))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val qrBoxSize = minOf(maxWidth * 0.62f, 220.dp)
            Box(
                modifier = Modifier
                    .size(qrBoxSize)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(9.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qrUrl != null) {
                    QRCodeView(data = qrUrl, modifier = Modifier.size(qrBoxSize - 18.dp))
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = FlowAccent,
                        strokeWidth = 3.dp,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Наведи камеру на QR-код",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = FlowGray.copy(alpha = 0.7f),
            ),
        )
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FlowAccentBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlowAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = TextStyle(
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                color = FlowWhite,
            ),
        )
    }
}

@Composable
private fun RightPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.flow_ingredients_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

 // Верхнее холодное свечение
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(FlowAccent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(860f, 140f),
                        radius = 1100f,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FlowAccent.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xFF120F1E).copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )

        FloatingBubblesOverlay()

 // Нижний градиент
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.44f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xAA121212), Color(0xF2121212)),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color(0xFF232328).copy(alpha = 0.90f),
                                Color(0xFF17171A).copy(alpha = 0.94f),
                            ),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Column {
                    Text(
                        text = "Твой ежедневный литр",
                        style = TextStyle(
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 27.sp,
                            lineHeight = 31.sp,
                            color = FlowWhite,
                        ),
                    )
                    Text(
                        text = "чистой пользы",
                        style = TextStyle(
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 27.sp,
                            lineHeight = 31.sp,
                            color = FlowAccent,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "100% натуральный продукт",
                        style = TextStyle(
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = FlowWhite.copy(alpha = 0.72f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBubblesOverlay() {
    val transition = rememberInfiniteTransition(label = "flow-bubbles")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bubble-drift",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val bubbles = listOf(
            BubbleSpec(0.16f, 0.18f, 0.020f, 0.012f, 0.010f, 0.10f),
            BubbleSpec(0.30f, 0.12f, 0.015f, -0.010f, 0.008f, 0.08f),
            BubbleSpec(0.56f, 0.23f, 0.024f, 0.014f, 0.012f, 0.12f),
            BubbleSpec(0.75f, 0.14f, 0.018f, -0.012f, 0.009f, 0.09f),
            BubbleSpec(0.84f, 0.28f, 0.013f, 0.010f, 0.008f, 0.08f),
        )

        bubbles.forEach { bubble ->
            drawCircle(
                color = Color.White.copy(alpha = bubble.alpha),
                radius = size.minDimension * bubble.radiusFraction,
                center = Offset(
                    x = size.width * (bubble.x + bubble.dx * drift),
                    y = size.height * (bubble.y + bubble.dy * drift),
                ),
            )
        }
    }
}

private data class BubbleSpec(
    val x: Float,
    val y: Float,
    val radiusFraction: Float,
    val dx: Float,
    val dy: Float,
    val alpha: Float,
)
