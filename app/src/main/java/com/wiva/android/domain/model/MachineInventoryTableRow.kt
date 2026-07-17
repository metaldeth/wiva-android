package com.wiva.android.domain.model

/**
 * Строка таблицы «Наполнение» в сервисном меню (итог merge базы + матрицы).
 */
data class MachineInventoryTableRow(
    val cellNumber: Int,
    val ingredientId: Int,
 /** Полное название: бренд · линия · продукт */
    val catalogTitle: String,
    val tasteName: String,
    val brandName: String,
    val price300Rub: Int?,
    val price700Rub: Int?,
    val active: Boolean,
    val volumeMl: Int?,
    val minVolumeMl: Int?,
    val maxVolumeMl: Int?,
    val drinkVolumeMl: Int,
)
