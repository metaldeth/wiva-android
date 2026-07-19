package com.wiva.android.domain.telemetry

import javax.inject.Inject
import javax.inject.Singleton

/** Описание одной физической продуктовой ячейки автомата. */
data class PhysicalCellDefinition(
    val cellNumber: Int,
    val maxVolume: Int,
)

/** Источник N ячеек и maxVolume для cells.schema.report (mock / конфиг машины). */
interface PhysicalCellSchemaProvider {
    fun physicalCells(): List<PhysicalCellDefinition>
}

@Singleton
class DefaultPhysicalCellSchemaProvider
@Inject
constructor() : PhysicalCellSchemaProvider {
    override fun physicalCells(): List<PhysicalCellDefinition> =
        (1..DEFAULT_CELL_COUNT).map { cellNumber ->
            PhysicalCellDefinition(
                cellNumber = cellNumber,
                maxVolume = DEFAULT_MAX_VOLUME_ML,
            )
        }

    companion object {
        const val DEFAULT_CELL_COUNT = 6
        const val DEFAULT_MAX_VOLUME_ML = 5000
    }
}
