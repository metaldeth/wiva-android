package com.viwa.android.services.calibration

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.TelemetryCell
import com.viwa.android.domain.model.TelemetryCellsSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Merges legacy JsonStore syrup conversion factors into cells snapshot. */
@Singleton
class SyrupConversionFactorMigration
@Inject
constructor(
    private val configRepository: ConfigRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadLegacyConversionFactors(): Map<Int, Double> {
        val raw = configRepository.getJson(JsonStoreKeys.SYRUP_CONVERSION_FACTORS) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(MapSerializer(Int.serializer(), Double.serializer()), raw)
        }.getOrDefault(emptyMap())
    }

    fun mergeLegacyIntoSnapshot(
        snapshot: TelemetryCellsSnapshot,
        legacyFactors: Map<Int, Double>,
    ): TelemetryCellsSnapshot {
        if (legacyFactors.isEmpty()) return snapshot
        val mergedCells =
            snapshot.cells.map { cell ->
                mergeLegacyIntoCell(cell, legacyFactors)
            }
        return snapshot.copy(cells = mergedCells)
    }

    fun mergeLegacyIntoCell(
        cell: TelemetryCell,
        legacyFactors: Map<Int, Double>,
    ): TelemetryCell {
        if (legacyFactors.isEmpty()) return cell
        if (cell.conversionFactor != TelemetryCell.DEFAULT_CONVERSION_FACTOR) return cell
        val legacy = legacyFactors[cell.cellNumber] ?: return cell
        return cell.copy(conversionFactor = legacy)
    }
}
