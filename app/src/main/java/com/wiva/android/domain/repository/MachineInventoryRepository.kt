package com.wiva.android.domain.repository

import com.wiva.android.domain.model.CellVolumeUpdate
import com.wiva.android.domain.model.ContainerCalibrationInfo
import com.wiva.android.domain.model.MachineInventoryTableRow
import com.wiva.android.domain.model.customer.DrinkContainer
import kotlinx.coroutines.flow.StateFlow

/** База ингредиентов + матрица наполнения + merge. */
interface MachineInventoryRepository {
    val inventoryRevision: StateFlow<Int>

    suspend fun ingestBaseIngredientExport(rawJson: String)

    suspend fun ingestCellStoreMessage(rawJson: String)

 /** Остатки по ячейкам продукта из WS `cellVolumeExport` (body.cells),. */
    suspend fun ingestCellVolumeExport(rawJson: String)

    suspend fun getTableRows(): List<MachineInventoryTableRow>

 /** Контейнеры из merge-конфига для калибровки сиропа. */
    suspend fun listContainersForCalibration(): List<ContainerCalibrationInfo>

 /** Обновляет conversionFactor у контейнера и сохраняет [com.wiva.android.data.local.db.JsonStoreKeys.TELEMETRY_MERGED_INVENTORY]. */
    suspend fun updateContainerConversionFactor(
        containerNumber: Int,
        newConversionFactor: Double,
    ): Result<Unit>

 /** Контейнер из merge-конфига по id вкуса. */
    suspend fun findDrinkContainerByTasteId(tasteId: Int): DrinkContainer?

 /** Контейнер по номеру ячейки (1…6). */
    suspend fun findContainerByNumber(containerNumber: Int): DrinkContainer?

 /** Контейнер по id продукта / ингредиента (`product.id`). */
    suspend fun findDrinkContainerByProductId(productId: Int): DrinkContainer?

 /** Все контейнеры напитков из merge-конфига, отсортированные по номеру. */
    suspend fun listDrinkContainers(): List<DrinkContainer>

 /** Списание объёма продукта из ячейки, объём не ниже 0. */
    suspend fun deductContainerVolume(
        containerNumber: Int,
        amount: Double,
    )

 /** Ручная установка объёмов и отправка [com.wiva.android.services.telemetry.WivaTelemetryService.sendCellVolumeImportFromConfig]. */
    suspend fun applyCellVolumes(updates: List<CellVolumeUpdate>)

 /** true, если база ингредиентов уже загружена (хотя бы один элемент). */
    suspend fun isBaseIngredientLoaded(): Boolean
}
