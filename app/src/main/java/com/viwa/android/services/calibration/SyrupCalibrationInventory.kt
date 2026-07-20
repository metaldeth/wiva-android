package com.viwa.android.services.calibration

import com.viwa.android.domain.model.ContainerCalibrationInfo
import com.viwa.android.domain.model.TelemetryCell
import com.viwa.android.domain.repository.TelemetryCellsRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Калибровка сиропов на базе MVP cells snapshot (conversionFactor в ячейке). */
@Singleton
class SyrupCalibrationInventory
@Inject
constructor(
    private val cellsRepository: TelemetryCellsRepository,
    private val conversionFactorMigration: SyrupConversionFactorMigration,
) {
    suspend fun listContainersForCalibration(): List<ContainerCalibrationInfo> {
        val snapshot = cellsRepository.getSnapshot() ?: return emptyList()
        return snapshot.cells
            .filter { !it.productUuid.isNullOrBlank() }
            .sortedBy { it.cellNumber }
            .map { cell ->
                ContainerCalibrationInfo(
                    containerNumber = cell.cellNumber,
                    catalogTitle = cell.productName.orEmpty(),
                    conversionFactor = cell.conversionFactor,
                    defaultProductMl = DEFAULT_PRODUCT_ML,
                )
            }
    }

    suspend fun updateContainerConversionFactor(
        containerNumber: Int,
        newConversionFactor: Double,
    ): Result<TelemetryCell> =
        runCatching {
            val snapshot =
                cellsRepository.getSnapshot()
                    ?: error("Cells snapshot отсутствует")
            val cell =
                snapshot.cells.find { it.cellNumber == containerNumber }
                    ?: error("Контейнер $containerNumber не найден")
            val updated = cell.copy(conversionFactor = newConversionFactor)
            val mergedCells =
                snapshot.cells.map { existing ->
                    if (existing.cellNumber == containerNumber) updated else existing
                }
            cellsRepository.replaceSnapshot(snapshot.copy(cells = mergedCells))
            updated
        }

    suspend fun findCellUuid(containerNumber: Int): String? =
        cellsRepository.getSnapshot()?.cells?.find { it.cellNumber == containerNumber }?.uuid

    suspend fun currentVolumeMl(containerNumber: Int): Int? =
        cellsRepository.getSnapshot()?.cells?.find { it.cellNumber == containerNumber }?.volume

    /** One-time merge of legacy JsonStore factors into persisted snapshot. */
    suspend fun migrateLegacyConversionFactorsIfNeeded() {
        val snapshot = cellsRepository.getSnapshot() ?: return
        val legacy = conversionFactorMigration.loadLegacyConversionFactors()
        val merged = conversionFactorMigration.mergeLegacyIntoSnapshot(snapshot, legacy)
        if (merged != snapshot) {
            cellsRepository.replaceSnapshot(merged)
        }
    }

    companion object {
        const val DEFAULT_CONVERSION_FACTOR = TelemetryCell.DEFAULT_CONVERSION_FACTOR
        const val DEFAULT_PRODUCT_ML = 30.0
    }
}
