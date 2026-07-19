package com.viwa.android.ui.screens.customer

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color

/**
 * Визуальные константы клиентского слоя.
 * См. `PrimaryActionButton.module.scss`, `DrinkListPage.module.scss`, `PreparingPage.module.scss`.
 */
object ViwaCustomerUiTokens {
 /** Основная кнопка экрана напитков: «Налить воду» и «Оплатить … ₽». */
    val BrandPrimary = Color(0xFFC500FF)

 /** ARGB бренда по умолчанию — светлая тема. */
    val DefaultBrandPrimaryArgb: Int =
        AndroidColor.argb(0xFF, 0xC5, 0x00, 0xFF)

 /**
 * Бренд по умолчанию — тёмная тема (чуть светлее для контраста на тёмном фоне).
 * Хранилище: [com.viwa.android.data.local.db.JsonStoreKeys.CUSTOMER_PRIMARY_BUTTON_ARGB_DARK].
 */
    val DefaultBrandPrimaryArgbDark: Int =
        AndroidColor.argb(0xFF, 0xDD, 0x66, 0xFF)

 /** Резерв под акцент выбранных опций (. */
    val BrandActiveBlue = Color(0xFFC500FF)

    val PrimaryButtonText = Color(0xFFFFFFFF)

 /** Тёмный фон тела (`--bg-main-body` / тёмная тема wiva). */
    val ScreenBg = Color(0xFF1C1C1E)

 /** Радиальный виньет (`--bg-vignette-*`). */
    val VignetteCenter = Color(0x66000000)

    val PanelOverlayBg = Color(0xE628282A)

    val DrinkHintPillBg = Color(0xD11C1C1E)

    val DrinkHintText = Color(0xE6FFFFFF)

    val OptionGroupBorder = Color(0xFF3A3A3C)

    val OptionGroupBg = Color(0xFF2C2C2E)

 /** `--typo-main-ghost` (Stats / ScannerHint). */
    val GhostTypo = Color(0x99E0E0E0)

 /**
 * Figma variable `typo/main/primary` на карточке напитка (node `772:1798`, get_variable_defs).
 * Совпадает с `--typo-main-primary` в `Theme_color_gpnDefault.css`.
 */
    val TypoMainPrimaryLight = Color(0xFF231F20)

 /** `--typo-main-primary` в тёмной теме (`Theme_color_gpnDark.css`). */
    val TypoMainPrimaryDark = Color(0xFFE0E0E0)

 /** Текст на активной карточке в тёмной теме (фон — акцент 40 %). В светлой теме — [TypoMainPrimaryLight]. */
    val DrinkCardOnAccentContent = Color(0xFFFFFFFF)

 /** Figma — неактивная опция в шапке (подпись и иконка). */
    val FigmaOptionInactive = Color(0xFF8A8A8C)

 /**
 * Figma — вертикальный разделитель в светлой теме (`#f0f0f1`).
 * В Compose для шапки опций задано `MaterialTheme.colorScheme.outlineVariant` (см. `LightScheme` / `DarkScheme` в `MainActivity`).
 */
    val FigmaOptionDivider = Color(0xFFF0F0F1)

 // --- Модалка выбора оплаты (Figma node 1549:292 / Card, get_design_context) ---

 /** `--bg/main/body` на панели модалки, fallback в макете `#f3f3f4`. */
    val PaymentModalSurface = Color(0xFFF3F3F4)

 /** Затемнение под модалкой: `--bg/split/tone` ≈ black 85 %. */
    val PaymentModalScrim = Color(0xD9000000)

 /** Подложка под QR СБП (светлая тема на тёмном фоне и тёмная тема модалки). */
    val SbpQrBackground = Color(0xFFFFFFFF)

 /** Плитка «Оплата СБП». */
    val PaymentTileSbpBg = Color(0xFF3B2699)

 /** Плитка «Оплата картой». */
    val PaymentTileCardBg = Color(0xFF585858)

 /** Подписи на плитках оплаты (`#e0e0e0`). */
    val PaymentTileLabel = Color(0xFFE0E0E0)

 /** Контур и типографика кнопки «Отмена» (`#4c4c4c`). */
    val PaymentCancelBorderAndText = Color(0xFF4C4C4C)
}
