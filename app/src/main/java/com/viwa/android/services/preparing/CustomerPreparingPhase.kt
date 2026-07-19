package com.viwa.android.services.preparing

/** Состояние экрана готовки после перехода с меню (ожидание SUCCESS с контроллера). */
sealed class CustomerPreparingPhase {
    data object Idle : CustomerPreparingPhase()

    data class AwaitingDrinkReady(
        val totalSec: Int,
    ) : CustomerPreparingPhase()

    data object DrinkReady : CustomerPreparingPhase()
}
