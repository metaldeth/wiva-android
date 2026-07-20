package com.viwa.android.domain.model

import kotlinx.serialization.Serializable

/** Единый локальный store ячеек и каталога продуктов (JsonStore). */
@Serializable
data class TelemetryCellsSnapshot(
    val schemaHash: String? = null,
    val contentRevision: Int? = null,
    val products: List<TelemetryProduct> = emptyList(),
    val cells: List<TelemetryCell> = emptyList(),
    val machineCalibration: MachineCalibration? = null,
    val savedAtEpochMs: Long = 0L,
)
