package com.wiva.android.domain.model

/** Ручное обновление объёма ячейки (сервисное меню «Остатки»). */
data class CellVolumeUpdate(
    val containerNumber: Int,
    val volumeMl: Int,
)
