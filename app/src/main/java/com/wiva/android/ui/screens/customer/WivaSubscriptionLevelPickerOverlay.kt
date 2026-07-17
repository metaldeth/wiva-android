package com.wiva.android.ui.screens.customer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiva.android.R
import com.wiva.android.services.telemetry.SubscriptionLevelItem
import com.wiva.android.ui.theme.MontserratFamily

/** Аналог `RECOMMENDED_LEVEL_INDEX` в `SubscribeOfferScreen.tsx`. */
private const val RecommendedLevelIndex = 1

private val BadgeBg = Color(0xFFFBBF24)
private val BadgeText = Color(0xFF7C2D12)
private val BackButtonGray = Color(0xFF9CA3AF)

/**
 * Фото-фоны по позиции тарифа (циклически для 5+ тарифов).
 * Изображения с flowstation.ru: фокус → старт, восстановление → дейли, вкус → макс, минералы → 4-й тариф.
 */
private data class SubscriptionCardVisual(
    @DrawableRes val photoRes: Int,
    val audienceLine: String,
    val featureLine: String,
)

private val CardVisuals = listOf(
    SubscriptionCardVisual(
        photoRes = R.drawable.flow_card_nosugar,
        audienceLine = "Для лёгкого ритма тренировок",
        featureLine = "Без сахара и лишних калорий",
    ),
    SubscriptionCardVisual(
        photoRes = R.drawable.flow_hero_bg,
        audienceLine = "Оптимум для регулярных тренировок",
        featureLine = "Ежедневный запас энергии и свежести",
    ),
    SubscriptionCardVisual(
        photoRes = R.drawable.flow_ingredients_hero,
        audienceLine = "Для тех, кто берёт максимум",
        featureLine = "Чистый вкус фруктов и быстрое восстановление",
    ),
    SubscriptionCardVisual(
        photoRes = R.drawable.flow_about_hero,
        audienceLine = "Для максимального комфорта каждый день",
        featureLine = "Свежий напиток всегда под рукой",
    ),
)

/**
 * Полноэкранный выбор тарифа подписки в стиле FLOW.
 * Карточки — с фото-фоном и двойным градиентным оверлеем; средний тариф акцентируется размером и бейджем.
 * Поддерживает светлую и тёмную темы (экранный фон и заголовок адаптируются, карточки — нет, они всегда тёмные).
 */
@Composable
fun WivaSubscriptionLevelPickerOverlay(
    s: Float,
    levels: List<SubscriptionLevelItem>?,
    levelsLoading: Boolean,
    tariffsError: String? = null,
    onDismiss: () -> Unit,
    onSelectLevel: (SubscriptionLevelItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val screenBg = if (isLight) Color(0xFFF0F0F0) else WivaCustomerUiTokens.ScreenBg
    val headerColor = if (isLight) WivaCustomerUiTokens.TypoMainPrimaryLight else Color.White
    val subtitleColor =
        if (isLight) WivaCustomerUiTokens.TypoMainPrimaryLight.copy(alpha = 0.55f)
        else Color.White.copy(alpha = 0.50f)

    Box(
        modifier = modifier.fillMaxSize().background(screenBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (24f * s).dp, vertical = (20f * s).dp),
        ) {
 // ── Заголовок ──────────────────────────────────────────────────
            Text(
                text = "Выберите подписку FLOW",
                fontSize = (26f * s).sp,
                lineHeight = (32f * s).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
                color = headerColor,
            )
            Spacer(Modifier.height((4f * s).dp))
            Text(
                text = "Подберите объём под свой ритм тренировок",
                fontSize = (15f * s).sp,
                lineHeight = (20f * s).sp,
                fontFamily = MontserratFamily,
                color = subtitleColor,
                modifier = Modifier.padding(bottom = (6f * s).dp),
            )
            Text(
                text = "Лимит обновляется каждый день в 00:00",
                fontSize = (12f * s).sp,
                lineHeight = (16f * s).sp,
                fontFamily = MontserratFamily,
                color = subtitleColor.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = (16f * s).dp),
            )

 // ── Основной контент ───────────────────────────────────────────
            when {
                !tariffsError.isNullOrBlank() -> {
                    ErrorBlock(
                        s = s,
                        message = tariffsError,
                        modifier = Modifier.weight(1f),
                    )
                }
                levels == null || levelsLoading -> {
                    LoadingBlock(s = s, modifier = Modifier.weight(1f))
                }
                levels.isEmpty() -> {
                    EmptyBlock(s = s, modifier = Modifier.weight(1f))
                }
                else -> {
                    val showRecommendedBadge = levels.size >= 2
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                (18f * s).dp,
                                Alignment.CenterHorizontally,
                            ),
                            verticalAlignment = Alignment.Top,
                            contentPadding = PaddingValues(
                                horizontal = (4f * s).dp,
                                vertical = (4f * s).dp,
                            ),
                        ) {
                            itemsIndexed(levels, key = { _, l -> l.uuid }) { index, level ->
                                val recommended = showRecommendedBadge && index == RecommendedLevelIndex
                                SubscriptionPhotoCard(
                                    s = s,
                                    level = level,
                                    recommended = recommended,
                                    visual = CardVisuals[index % CardVisuals.size],
                                    onClick = { onSelectLevel(level) },
                                )
                            }
                        }
                    }
                }
            }

 // ── Кнопка назад ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (12f * s).dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size((48f * s).dp)
                        .clip(CircleShape)
                        .background(BackButtonGray),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size((24f * s).dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPhotoCard(
    s: Float,
    level: SubscriptionLevelItem,
    recommended: Boolean,
    visual: SubscriptionCardVisual,
    onClick: () -> Unit,
) {
    val cardWidth = if (recommended) (272f * s).dp else (226f * s).dp
    val cardHeight = if (recommended) (390f * s).dp else (318f * s).dp
    val cornerRadius = (20f * s).dp
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .run {
                if (recommended) border(
                    width = (2.5f * s).dp,
                    color = WivaCustomerUiTokens.BrandPrimary,
                    shape = RoundedCornerShape(cornerRadius),
                ) else this
            }
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
 // ── Фото-фон ───────────────────────────────────────────────────────
        Image(
            painter = painterResource(visual.photoRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

 // ── Двойной градиент: затемняет верх (для текста) и низ (для цены) ─
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xA6000000),
                            0.28f to Color(0x36000000),
                            0.60f to Color(0x66000000),
                            1.00f to Color(0xF1000000),
                        ),
                    ),
                ),
        )

 // ── Контент ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (18f * s).dp, vertical = (18f * s).dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
 // Верх: бейдж + название
            Column {
                if (recommended) {
                    Text(
                        text = "Лучший выбор",
                        fontSize = (12f * s).sp,
                        lineHeight = (16f * s).sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MontserratFamily,
                        color = BadgeText,
                        modifier = Modifier
                            .clip(RoundedCornerShape((8f * s).dp))
                            .background(BadgeBg)
                            .padding(horizontal = (10f * s).dp, vertical = (4f * s).dp),
                    )
                    Spacer(Modifier.height((10f * s).dp))
                }
                Text(
                    text = level.name ?: "Подписка",
                    fontSize = ((if (recommended) 22f else 18f) * s).sp,
                    lineHeight = ((if (recommended) 28f else 24f) * s).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height((4f * s).dp))
                Text(
                    text = visual.audienceLine,
                    fontSize = (12f * s).sp,
                    lineHeight = (16f * s).sp,
                    fontFamily = MontserratFamily,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

 // Низ: стек условий + кнопка
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((16f * s).dp))
                    .background(Color(0x332A2A2A))
                    .border(
                        width = (1f * s).dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape((16f * s).dp),
                    )
                    .padding((14f * s).dp),
            ) {
                Column {
                    Text(
                        text = "Ежедневный лимит",
                        fontSize = (11f * s).sp,
                        lineHeight = (14f * s).sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MontserratFamily,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                    Spacer(Modifier.height((3f * s).dp))
                    Text(
                        text = level.volume?.let { "$it л / день" } ?: "Ежедневный лимит",
                        fontSize = ((if (recommended) 28f else 22f) * s).sp,
                        lineHeight = ((if (recommended) 32f else 26f) * s).sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MontserratFamily,
                        color = Color.White,
                    )
                    Spacer(Modifier.height((3f * s).dp))
                    Text(
                        text = formatPriceMonthly(level.price),
                        fontSize = ((if (recommended) 15f else 13f) * s).sp,
                        lineHeight = ((if (recommended) 20f else 18f) * s).sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MontserratFamily,
                        color = Color.White.copy(alpha = 0.84f),
                    )
                    Spacer(Modifier.height((8f * s).dp))
                    Text(
                        text = visual.featureLine,
                        fontSize = (12f * s).sp,
                        lineHeight = (16f * s).sp,
                        fontFamily = MontserratFamily,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height((12f * s).dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((46f * s).dp)
                            .clip(RoundedCornerShape((12f * s).dp))
                            .background(
                                if (recommended) WivaCustomerUiTokens.BrandPrimary
                                else Color.White.copy(alpha = 0.18f),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Выбрать",
                            fontSize = (15f * s).sp,
                            lineHeight = (20f * s).sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MontserratFamily,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock(s: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size((40f * s).dp),
            color = WivaCustomerUiTokens.BrandPrimary,
            strokeWidth = (4f * s).dp,
        )
        Spacer(Modifier.height((16f * s).dp))
        Text(
            text = "Загрузка тарифов...",
            fontSize = (16f * s).sp,
            fontFamily = MontserratFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBlock(s: Float, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            fontSize = (16f * s).sp,
            fontFamily = MontserratFamily,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = (8f * s).dp),
        )
    }
}

@Composable
private fun EmptyBlock(s: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Нет доступных тарифов",
            fontSize = (18f * s).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFamily,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height((8f * s).dp))
        Text(
            text = "Попробуйте позже или обратитесь к администратору",
            fontSize = (14f * s).sp,
            fontFamily = MontserratFamily,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPriceRub(price: Double): String {
    val whole = kotlin.math.abs(price % 1.0) < 1e-6
    return if (whole) "${price.toInt()} ₽" else "$price ₽"
}

private fun formatPriceMonthly(price: Double): String = "${formatPriceRub(price)} / мес"
