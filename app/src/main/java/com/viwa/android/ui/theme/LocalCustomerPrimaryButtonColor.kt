package com.viwa.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.viwa.android.ui.screens.customer.ViwaCustomerUiTokens

/** Цвет основной кнопки на экране напитков; задаётся в [com.viwa.android.ui.MainActivity] из [ThemeRepository]. */
val LocalCustomerPrimaryButtonColor =
    staticCompositionLocalOf { ViwaCustomerUiTokens.BrandPrimary }
