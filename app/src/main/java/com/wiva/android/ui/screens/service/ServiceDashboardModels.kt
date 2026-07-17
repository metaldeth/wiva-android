package com.wiva.android.ui.screens.service

import com.wiva.android.domain.model.MachineInventoryTableRow
import com.wiva.android.domain.repository.MachineInventoryRepository
import kotlin.math.floor
import kotlin.math.max

/**
 * Одна ячейка (1…6) для экрана «Дашборд»: остаток, шкала, оценка порций по рецепту.
 */
data class ServiceDashboardCellUi(
    val cellNumber: Int,
 /** Пустая ячейка в merge-конфиге. */
    val hasData: Boolean,
 /** Ключ PNG в assets wiva_electron/img/horizontalCard. */
    val tasteMediaKey: String?,
 /** HEX цвета вкуса из каталога (плейсхолдер, если картинка не загрузилась). */
    val tasteHexColor: String?,
    val catalogTitle: String,
    val volumeMl: Int,
    val maxVolumeMl: Int,
 /** 0.1 для [androidx.compose.material3.LinearProgressIndicator]. */
    val fillFraction: Float,
 /** Объём ингредиента в литрах. */
    val ingredientLiters: Double,
 /** Оценка числа стандартных порций напитка по [dosage.product]. */
    val approxDrinks: Int?,
 /** Оценка суммарного объёма готового напитка, л. */
    val approxBeverageLiters: Double?,
)

suspend fun buildServiceDashboardCells(
    rows: List<MachineInventoryTableRow>,
    inventoryRepository: MachineInventoryRepository,
): List<ServiceDashboardCellUi> {
    val byCell = rows.associateBy { it.cellNumber }
    return (1..6).map { cell ->
        val row = byCell[cell]
        if (row == null) {
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
            val vol = row.volumeMl ?: 0
            val maxV = row.maxVolumeMl ?: 0
            val frac =
                if (maxV > 0) {
                    (vol.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            val container = inventoryRepository.findContainerByNumber(cell)
            val taste = container?.product?.taste
            val dosage = container?.product?.dosage
            val dosageProduct = dosage?.product ?: 0.0
            val drinkVol = dosage?.drinkVolume?.takeIf { it > 0 } ?: row.drinkVolumeMl.takeIf { it > 0 }
            val approxDrinks =
                if (dosageProduct > 0.0) {
                    max(0, floor(vol / dosageProduct).toInt())
                } else {
                    null
                }
            val approxBevLiters =
                if (approxDrinks != null && drinkVol != null && drinkVol > 0) {
                    approxDrinks * drinkVol / 1000.0
                } else {
                    null
                }
            ServiceDashboardCellUi(
                cellNumber = cell,
                hasData = true,
                tasteMediaKey = taste?.mediaKey,
                tasteHexColor = taste?.hexColor,
                catalogTitle = row.catalogTitle,
                volumeMl = vol,
                maxVolumeMl = maxV,
                fillFraction = frac,
                ingredientLiters = vol / 1000.0,
                approxDrinks = approxDrinks,
                approxBeverageLiters = approxBevLiters,
            )
        }
    }
}
