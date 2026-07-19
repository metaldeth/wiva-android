package com.wiva.android.ui.screens.service

import com.wiva.android.domain.model.MvpInventoryTableRow

/** Одна ячейка (1…6) для экрана «Дашборд». */
data class ServiceDashboardCellUi(
    val cellNumber: Int,
    val hasData: Boolean,
    val tasteMediaKey: String?,
    val tasteHexColor: String?,
    val catalogTitle: String,
    val volumeMl: Int,
    val maxVolumeMl: Int,
    val fillFraction: Float,
    val ingredientLiters: Double,
    val approxDrinks: Int?,
    val approxBeverageLiters: Double?,
)

fun buildServiceDashboardCells(rows: List<MvpInventoryTableRow>): List<ServiceDashboardCellUi> {
    val byCell = rows.associateBy { it.cellNumber }
    return (1..6).map { cell ->
        val row = byCell[cell]
        if (row == null || row.productUuid.isNullOrBlank()) {
            ServiceDashboardCellUi(
                cellNumber = cell,
                hasData = false,
                tasteMediaKey = null,
                tasteHexColor = null,
                catalogTitle = "",
                volumeMl = 0,
                maxVolumeMl = 0,
                fillFraction = 0f,
                ingredientLiters = 0.0,
                approxDrinks = null,
                approxBeverageLiters = null,
            )
        } else {
            val vol = row.volumeMl
            val maxV = row.maxVolume
            val frac =
                if (maxV > 0) {
                    (vol.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            ServiceDashboardCellUi(
                cellNumber = cell,
                hasData = true,
                tasteMediaKey = row.tasteMediaKey,
                tasteHexColor = null,
                catalogTitle = row.productName.orEmpty(),
                volumeMl = vol,
                maxVolumeMl = maxV,
                fillFraction = frac,
                ingredientLiters = vol / 1000.0,
                approxDrinks = null,
                approxBeverageLiters = null,
            )
        }
    }
}
