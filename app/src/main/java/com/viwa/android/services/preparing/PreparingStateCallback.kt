package com.viwa.android.services.preparing

import com.viwa.android.domain.model.preparing.PreparingState

/** Колбэк для подписчиков на машину состояний готовки (G4 может логировать/метрики). */
fun interface PreparingStateCallback {
    operator fun invoke(state: PreparingState)
}
