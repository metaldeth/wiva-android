package com.viwa.android.services.calibration

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.domain.model.ContainerCalibrationInfo
import com.viwa.android.domain.repository.TelemetryCellsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Калибровка сиропов на базе MVP cells snapshot (без legacy merge-inventory). */
@Singleton
class SyrupCalibrationInventory
@Inject
constructor(
    private val cellsRepository: TelemetryCellsRepository,
    private val configRepository: ConfigRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listContainersForCalibration(): List<ContainerCalibrationInfo> {
        val snapshot = cellsRepository.getSnapshot() ?: return emptyList()
        val factors = loadConversionFactors()
        return snapshot.cells
            .filter { !it.productUuid.isNullOrBlank() }
            .sortedBy { it.cellNumber }
            .map { cell ->
                ContainerCalibrationInfo(
                    containerNumber = cell.cellNumber,
                    catalogTitle = cell.productName.orEmpty(),
                    conversionFactor = factors[cell.cellNumber] ?: DEFAULT_CONVERSION_FACTOR,
                    defaultProductMl = DEFAULT_PRODUCT_ML,
                )
            }
    }

    suspend fun updateContainerConversionFactor(
        containerNumber: Int,
        newConversionFactor: Double,
    ): Result<Unit> =
        runCatching {
            val factors = loadConversionFactors().toMutableMap()
            factors[containerNumber] = newConversionFactor
            configRepository.setJson(
                JsonStoreKeys.SYRUP_CONVERSION_FACTORS,
                json.encodeToString(MapSerializer(Int.serializer(), Double.serializer()), factors),
            )
        }

    suspend fun findCellUuid(containerNumber: Int): String? =
        cellsRepository.getSnapshot()?.cells?.find { it.cellNumber == containerNumber }?.uuid

    suspend fun currentVolumeMl(containerNumber: Int): Int? =
        cellsRepository.getSnapshot()?.cells?.find { it.cellNumber == containerNumber }?.volume

    companion object {
        const val DEFAULT_CONVERSION_FACTOR = 4.0
        const val DEFAULT_PRODUCT_ML = 30.0
    }

    private suspend fun loadConversionFactors(): Map<Int, Double> {
        val raw = configRepository.getJson(JsonStoreKeys.SYRUP_CONVERSION_FACTORS) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(MapSerializer(Int.serializer(), Double.serializer()), raw)
        }.getOrDefault(emptyMap())
    }
}
