package com.wiva.android.domain.telemetry

import com.wiva.android.data.telemetry.inventory.BaseIngredientFlatWire
import com.wiva.android.data.telemetry.inventory.CellStoreMatrixBodyWire
import com.wiva.android.data.telemetry.inventory.MatrixCellWire
import com.wiva.android.data.telemetry.inventory.MatrixComponentWire
import com.wiva.android.data.telemetry.inventory.MatrixPriceWire
import com.wiva.android.data.telemetry.inventory.StoredComponentDtoWire
import com.wiva.android.data.telemetry.inventory.StoredContainerWire
import com.wiva.android.data.telemetry.inventory.StoredCupWire
import com.wiva.android.data.telemetry.inventory.StoredDosageWire
import com.wiva.android.data.telemetry.inventory.StoredMachineConfigWire
import com.wiva.android.data.telemetry.inventory.StoredPriceWire
import com.wiva.android.data.telemetry.inventory.StoredProducingCompanyWire
import com.wiva.android.data.telemetry.inventory.StoredProductWire
import com.wiva.android.data.telemetry.inventory.StoredTasteWire
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber

/**
 * Порт [ mergeBaseAndMatrix.ts]: объединение базы ингредиентов и матрицы наполнения.
 */
object MergeBaseAndMatrix {
    private val DEFAULT_CUP = StoredCupWire()

    private const val VOL_SMALL = 300
    private const val VOL_LARGE = 700

    private val jsonLenient =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    data class ParsedDosage(
        val drinkVolume: Int,
        val water: Double,
        val product: Double,
        val conversionFactor: Double,
    )

    fun parseDosage(categoryConfigMachine: String): ParsedDosage? {
        val arr: JsonArray =
            try {
                val el = jsonLenient.parseToJsonElement(categoryConfigMachine.ifBlank { "[]" })
                el as? JsonArray ?: return null
            } catch (_: Exception) {
                return null
            }
        val map = mutableMapOf<String, Double>()
        for (item in arr) {
            val o = item as? JsonObject ?: continue
            val key = o["key"]?.let { (it as? JsonPrimitive)?.contentOrNull } ?: continue
            val valueStr = o["value"]?.let { (it as? JsonPrimitive)?.contentOrNull } ?: continue
            map[key] = valueStr.toDoubleOrNull() ?: continue
        }
        return ParsedDosage(
            drinkVolume = map["DrinkVolume"]?.toInt() ?: 300,
            water = map["Water"] ?: 250.0,
            product = map["Product"] ?: 30.0,
            conversionFactor = map["ConversionFactor"] ?: 4.0,
        )
    }

    fun matrixPricesToConfig(prices: List<MatrixPriceWire>): List<StoredPriceWire> {
        val raw = prices.map { StoredPriceWire(volume = it.volume, price = it.price) }
        if (raw.isEmpty()) return emptyList()
        if (raw.size == 1) {
            val p = raw[0].price
            return listOf(StoredPriceWire(VOL_SMALL, p), StoredPriceWire(VOL_LARGE, p))
        }
        val sorted = raw.sortedBy { it.price }
        return listOf(
            StoredPriceWire(VOL_SMALL, sorted[0].price),
            StoredPriceWire(VOL_LARGE, sorted[1].price),
        )
    }

    private fun baseComponentsToDTO(components: List<MatrixComponentWire>?): List<StoredComponentDtoWire> {
        if (components.isNullOrEmpty()) return emptyList()
        return components.map {
            StoredComponentDtoWire(
                componentName = it.name,
                componentAmount = it.qty,
                measurementUnit = it.unit,
            )
        }
    }

    private data class PreservedVolumes(
        val volume: Int? = null,
        val minVolume: Int? = null,
        val maxVolume: Int? = null,
    )

    private fun buildCatalogTitle(
        info: BaseIngredientFlatWire?,
        productName: String,
    ): String {
        val parts = mutableListOf<String>()
        info?.brand?.name?.trim()?.takeIf { it.isNotEmpty() }?.let(parts::add)
        info?.ingredientLine?.name?.trim()?.takeIf { it.isNotEmpty() }?.let(parts::add)
        productName.trim().takeIf { it.isNotEmpty() }?.let { p ->
            if (parts.none { it.equals(p, ignoreCase = true) }) parts.add(p)
        }
        return parts.joinToString(" · ").ifBlank { productName }
    }

    fun merge(
        base: Map<Int, BaseIngredientFlatWire>,
        matrix: CellStoreMatrixBodyWire,
        existingConfig: StoredMachineConfigWire?,
    ): StoredMachineConfigWire {
        val saved = mutableMapOf<Int, PreservedVolumes>()
        for (c in existingConfig?.containers.orEmpty()) {
            if (c.volume != null || c.minVolume != null || c.maxVolume != null) {
                saved[c.containerNumber] =
                    PreservedVolumes(volume = c.volume, minVolume = c.minVolume, maxVolume = c.maxVolume)
            }
        }

        val cellsByNumber = (matrix.cells).associateBy { it.cellNumber }
        val containers = mutableListOf<StoredContainerWire>()

        for (mp in matrix.products) {
            val info = base[mp.ingredientId]
            val dosageParsed = parseDosage(mp.categoryConfigMachine)
            if (dosageParsed == null) {
                Timber.e(
                    "MergeBaseAndMatrix: skip ingredientId=${mp.ingredientId} bad categoryConfigMachine",
                )
                continue
            }

            val name = info?.name ?: mp.name ?: "Ingredient ${mp.ingredientId}"
            val tasteWire = info?.taste ?: mp.taste
            val taste =
                StoredTasteWire(
                    name = tasteWire?.name ?: "default",
                    id = tasteWire?.id ?: 0,
                    hexColor = tasteWire?.hexColor,
                    mediaKey = tasteWire?.mediaKey,
                )

            val producingCompany =
                if (info?.brand != null) {
                    StoredProducingCompanyWire(
                        name = info.brand.name,
                        mediaKey = info.brand.mediaKey ?: "",
                        id = info.brand.id,
                    )
                } else {
                    StoredProducingCompanyWire(name = "—", mediaKey = "", id = 0)
                }

            val componentOnAmount = info?.componentOnAmount ?: mp.componentOnAmount ?: 1.0
            val components = baseComponentsToDTO(info?.components ?: mp.components)
            val dPrices = matrixPricesToConfig(mp.prices)
            val hasSmallDrink = dPrices.size > 1

            val dosage =
                StoredDosageWire(
                    conversionFactor = dosageParsed.conversionFactor,
                    drinkVolume = dosageParsed.drinkVolume,
                    product = dosageParsed.product,
                    water = dosageParsed.water,
                )

            val product =
                StoredProductWire(
                    name = name,
                    taste = taste,
                    producingCompany = producingCompany,
                    componentOnAmount = componentOnAmount,
                    components = components,
                    condition = "Powder",
                    dPrices = dPrices,
                    dosage = dosage,
                    id = mp.ingredientId,
                )

            val catalogTitle = buildCatalogTitle(info, name)

            for (cellNumber in mp.cellNumbers) {
                val preserved = saved[cellNumber]
                val matrixCell: MatrixCellWire? = cellsByNumber[cellNumber]

                val dosageForCell =
                    matrixCell?.categoryConfigMachine?.let { parseDosage(it) }
                val containerDosage =
                    dosageForCell?.let {
                        StoredDosageWire(
                            conversionFactor = it.conversionFactor,
                            drinkVolume = it.drinkVolume,
                            product = it.product,
                            water = it.water,
                        )
                    } ?: dosage

                val container =
                    StoredContainerWire(
                        containerNumber = cellNumber,
                        cup = DEFAULT_CUP,
                        product = product.copy(dosage = containerDosage),
                        isActive = mp.isActive,
                        hasSmallDrink = hasSmallDrink,
                        catalogTitle = catalogTitle,
                    )

                val withVolumes =
                    when {
                        preserved != null ->
                            container.copy(
                                volume = preserved.volume ?: 0,
                                minVolume = preserved.minVolume,
                                maxVolume = preserved.maxVolume,
                            )
                        matrixCell != null ->
                            container.copy(
                                volume = 0,
                                minVolume = matrixCell.minVolume,
                                maxVolume = matrixCell.maxVolume,
                            )
                        else -> container.copy(volume = 0)
                    }
                containers.add(withVolumes)
            }
        }

        containers.sortBy { it.containerNumber }
        return StoredMachineConfigWire(containers = containers)
    }
}
