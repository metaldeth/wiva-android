package com.wiva.android.domain.telemetry

import com.wiva.android.domain.model.TelemetryCellsSnapshot
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Стабильная генерация uuid ячеек при первой инициализации схемы (OQ-5). */
@Singleton
class CellUuidAllocator
@Inject
constructor() {
    fun uuidForCellNumber(
        cellNumber: Int,
        existingSnapshot: TelemetryCellsSnapshot?,
    ): String {
        val existing =
            existingSnapshot
                ?.cells
                ?.firstOrNull { it.cellNumber == cellNumber }
                ?.uuid
        return existing ?: UUID.randomUUID().toString()
    }

    fun allocateForPhysicalCells(
        physicalCells: List<PhysicalCellDefinition>,
        existingSnapshot: TelemetryCellsSnapshot?,
    ): Map<Int, String> =
        physicalCells.associate { definition ->
            definition.cellNumber to uuidForCellNumber(definition.cellNumber, existingSnapshot)
        }
}
