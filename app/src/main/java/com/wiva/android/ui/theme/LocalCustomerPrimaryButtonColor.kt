package com.wiva.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.wiva.android.ui.screens.customer.WivaCustomerUiTokens

/** Цвет основной кнопки на экране напитков; задаётся в [com.wiva.android.ui.MainActivity] из [ThemeRepository]. */
val LocalCustomerPrimaryButtonColor =
    staticCompositionLocalOf { WivaCustomerUiTokens.BrandPrimary }
