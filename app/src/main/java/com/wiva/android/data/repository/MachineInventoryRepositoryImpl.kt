package com.wiva.android.data.repository

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.data.telemetry.inventory.BaseIngredientBrandWire
import com.wiva.android.data.telemetry.inventory.BaseIngredientFlatWire
import com.wiva.android.data.telemetry.inventory.BaseIngredientsFileWire
import com.wiva.android.data.telemetry.inventory.CellStoreMatrixBodyWire
import com.wiva.android.data.telemetry.inventory.MatrixBrandWire
import com.wiva.android.data.telemetry.inventory.MatrixNamedIdWire
import com.wiva.android.data.telemetry.inventory.StoredDosageWire
import com.wiva.android.data.telemetry.inventory.StoredMachineConfigWire
import com.wiva.android.domain.model.CellVolumeUpdate
import com.wiva.android.domain.model.ContainerCalibrationInfo
import com.wiva.android.domain.model.MachineInventoryTableRow
import com.wiva.android.domain.model.customer.DrinkContainer
import com.wiva.android.domain.model.customer.DrinkDosage
import com.wiva.android.domain.model.customer.DrinkPrice
import com.wiva.android.domain.model.customer.DrinkProduct
import com.wiva.android.domain.model.customer.DrinkTaste
import com.wiva.android.domain.repository.MachineInventoryRepository
import com.wiva.android.data.telemetry.inventory.StoredContainerWire
import com.wiva.android.domain.telemetry.MergeBaseAndMatrix
import com.wiva.android.services.telemetry.WivaTelemetryService
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

@Singleton
class MachineInventoryRepositoryImpl
@Inject
constructor(
    private val configRepository: ConfigRepository,
    private val telemetryService: Lazy<WivaTelemetryService>,
) : MachineInventoryRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private val mutex = Mutex()
    private val _revision = MutableStateFlow(0)
    override val inventoryRevision: StateFlow<Int> = _revision.asStateFlow()

    private fun bump() {
        _revision.value = _revision.value + 1
    }

    private suspend fun loadBase(): Map<Int, BaseIngredientFlatWire> {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_BASE_INGREDIENTS) ?: return emptyMap()
        return runCatching {
            val file = json.decodeFromString(BaseIngredientsFileWire.serializer(), raw)
            file.entries.mapKeys { it.key.toInt() }
        }.getOrElse {
            Timber.e(it, "MachineInventory: loadBase")
            emptyMap()
        }
    }

    private suspend fun saveBase(map: Map<Int, BaseIngredientFlatWire>) {
        val file = BaseIngredientsFileWire(entries = map.mapKeys { it.key.toString() })
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_BASE_INGREDIENTS,
            json.encodeToString(BaseIngredientsFileWire.serializer(), file),
        )
    }

    private suspend fun loadMatrix(): CellStoreMatrixBodyWire {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_CELL_STORE_MATRIX) ?: return CellStoreMatrixBodyWire()
        return runCatching {
            json.decodeFromString(CellStoreMatrixBodyWire.serializer(), raw)
        }.getOrElse {
            Timber.e(it, "MachineInventory: loadMatrix")
            CellStoreMatrixBodyWire()
        }
    }

    private suspend fun saveMatrix(matrix: CellStoreMatrixBodyWire) {
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_CELL_STORE_MATRIX,
            json.encodeToString(CellStoreMatrixBodyWire.serializer(), matrix),
        )
    }

    private suspend fun loadMerged(): StoredMachineConfigWire? {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_MERGED_INVENTORY) ?: return null
        return runCatching { json.decodeFromString(StoredMachineConfigWire.serializer(), raw) }
            .getOrElse {
                Timber.e(it, "MachineInventory: loadMerged")
                null
            }
    }

    private suspend fun saveMerged(config: StoredMachineConfigWire) {
        configRepository.setJson(
            JsonStoreKeys.TELEMETRY_MERGED_INVENTORY,
            json.encodeToString(StoredMachineConfigWire.serializer(), config),
        )
    }

    private fun flattenBrands(brands: List<BaseIngredientBrandWire>): Map<Int, BaseIngredientFlatWire> {
        val map = linkedMapOf<Int, BaseIngredientFlatWire>()
        for (brand in brands) {
            val brandInfo =
                MatrixBrandWire(
                    id = brand.id,
                    name = brand.name,
                    mediaKey = brand.mediaKey.ifBlank { null },
                )
            for (line in brand.ingredientLines) {
                val lineInfo =
                    MatrixNamedIdWire(
                        id = line.id,
                        name = line.name,
                    )
                for (ing in line.ingredients) {
                    map[ing.id] =
                        BaseIngredientFlatWire(
                            name = ing.name,
                            mediaKey = ing.mediaKey,
                            taste = ing.taste,
                            componentOnAmount = ing.componentOnAmount,
                            components = ing.components,
                            brand = brandInfo,
                            ingredientLine = lineInfo,
                        )
                }
            }
        }
        return map
    }

    private suspend fun mergeAndPersist() {
        val base = loadBase()
        val matrix = loadMatrix()
        val existing = loadMerged()
        val merged = MergeBaseAndMatrix.merge(base, matrix, existing)
        saveMerged(merged)
        Timber.i("MachineInventory: merge OK, containers=${merged.containers.size}")
    }

    override suspend fun ingestBaseIngredientExport(rawJson: String) {
        mutex.withLock {
            runCatching {
                val root = json.parseToJsonElement(rawJson).jsonObject
                val success = root["success"]?.jsonPrimitive?.booleanOrNull
                if (success == false) {
                    Timber.w("MachineInventory: baseIngredient success=false ${root["message"]}")
                    return@withLock
                }
                val bodyEl = root["body"] ?: return@withLock
                if (bodyEl is JsonNull) return@withLock
                val arr = bodyEl.jsonArray
                val brands =
                    arr.map { element ->
                        json.decodeFromJsonElement(BaseIngredientBrandWire.serializer(), element)
                    }
                val map = flattenBrands(brands)
                saveBase(map)
                mergeAndPersist()
                bump()
            }.onFailure { Timber.e(it, "MachineInventory: ingestBaseIngredientExport") }
        }
    }

    override suspend fun ingestCellStoreMessage(rawJson: String) {
        mutex.withLock {
            runCatching {
                val root = json.parseToJsonElement(rawJson).jsonObject
                val success = root["success"]?.jsonPrimitive?.booleanOrNull
                if (success == false) {
                    Timber.w("MachineInventory: cellStore success=false ${root["message"]}")
                    return@withLock
                }
                val bodyEl = root["body"]
                if (bodyEl == null || bodyEl is JsonNull) {
                    Timber.d("MachineInventory: cellStore body=null, пропускаем (ACK)")
                    return@withLock
                }
                val matrix = json.decodeFromJsonElement(CellStoreMatrixBodyWire.serializer(), bodyEl)
                saveMatrix(matrix)
                mergeAndPersist()
                bump()
            }.onFailure { Timber.e(it, "MachineInventory: ingestCellStoreMessage") }
        }
    }

    override suspend fun ingestCellVolumeExport(rawJson: String) {
        mutex.withLock {
            runCatching {
                val root = json.parseToJsonElement(rawJson).jsonObject
                val body = root["body"]?.jsonObject ?: return@withLock
                val cellsArr = body["cells"]?.jsonArray ?: return@withLock
                val cellsByNumber = LinkedHashMap<Int, Int>()
                for (el in cellsArr) {
                    val obj = el.jsonObject
                    val num = obj["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
                    cellsByNumber[num] = parseCellVolumeExportVolume(obj["volume"])
                }
                if (cellsByNumber.isEmpty()) return@withLock
                val merged = loadMerged() ?: return@withLock
                val newContainers =
                    merged.containers.map { c ->
                        val v = cellsByNumber[c.containerNumber]
                        if (v != null) c.copy(volume = v) else c
                    }
                if (newContainers == merged.containers) return@withLock
                saveMerged(StoredMachineConfigWire(containers = newContainers))
                bump()
                Timber.d("MachineInventory: cellVolumeExport применён, ячеек=${cellsByNumber.size}")
            }.onFailure { Timber.e(it, "MachineInventory: ingestCellVolumeExport") }
        }
    }

    private fun parseCellVolumeExportVolume(volumeEl: JsonElement?): Int {
        if (volumeEl == null || volumeEl is JsonNull) return 0
        val p = volumeEl.jsonPrimitive
        return p.intOrNull
            ?: p.contentOrNull?.toDoubleOrNull()?.roundToInt()
            ?: 0
    }

    override suspend fun isBaseIngredientLoaded(): Boolean {
        val raw = configRepository.getJson(JsonStoreKeys.TELEMETRY_BASE_INGREDIENTS) ?: return false
        return runCatching {
            val file = json.decodeFromString(BaseIngredientsFileWire.serializer(), raw)
            file.entries.isNotEmpty()
        }.getOrElse { false }
    }

    override suspend fun listContainersForCalibration(): List<ContainerCalibrationInfo> {
        val merged = loadMerged() ?: return emptyList()
        return merged.containers
            .sortedBy { it.containerNumber }
            .map { c ->
                val d = c.product.dosage
                ContainerCalibrationInfo(
                    containerNumber = c.containerNumber,
                    catalogTitle = c.catalogTitle,
                    conversionFactor = d.conversionFactor,
                    defaultProductMl = d.product,
                )
            }
    }

    override suspend fun updateContainerConversionFactor(
        containerNumber: Int,
        newConversionFactor: Double,
    ): Result<Unit> =
        mutex.withLock {
            runCatching {
                val merged = loadMerged() ?: error("Нет merge-конфигурации наполнения")
                val idx = merged.containers.indexOfFirst { it.containerNumber == containerNumber }
                if (idx < 0) error("Контейнер $containerNumber не найден")
                val c = merged.containers[idx]
                val d = c.product.dosage
                val updatedDosage =
                    StoredDosageWire(
                        conversionFactor = newConversionFactor,
                        drinkVolume = d.drinkVolume,
                        product = d.product,
                        water = d.water,
                    )
                val updatedProduct = c.product.copy(dosage = updatedDosage)
                val updatedContainer = c.copy(product = updatedProduct)
                val newContainers = merged.containers.toMutableList()
                newContainers[idx] = updatedContainer
                saveMerged(StoredMachineConfigWire(containers = newContainers))
                bump()
            }
        }

    override suspend fun findDrinkContainerByTasteId(tasteId: Int): DrinkContainer? {
        val merged = loadMerged() ?: return null
        val c = merged.containers.firstOrNull { it.product.taste.id == tasteId } ?: return null
        return mapStoredContainerToDrink(c)
    }

    override suspend fun findContainerByNumber(containerNumber: Int): DrinkContainer? {
        val merged = loadMerged() ?: return null
        val c = merged.containers.firstOrNull { it.containerNumber == containerNumber } ?: return null
        return mapStoredContainerToDrink(c)
    }

    override suspend fun findDrinkContainerByProductId(productId: Int): DrinkContainer? {
        val merged = loadMerged() ?: return null
        val c = merged.containers.firstOrNull { it.product.id == productId } ?: return null
        return mapStoredContainerToDrink(c)
    }

    override suspend fun listDrinkContainers(): List<DrinkContainer> {
        val merged = loadMerged() ?: return emptyList()
        return merged.containers
            .sortedBy { it.containerNumber }
            .map { mapStoredContainerToDrink(it) }
    }

    override suspend fun deductContainerVolume(
        containerNumber: Int,
        amount: Double,
    ) = mutex.withLock {
        val merged = loadMerged() ?: return@withLock
        val idx = merged.containers.indexOfFirst { it.containerNumber == containerNumber }
        if (idx < 0) return@withLock
        val c = merged.containers[idx]
        val current = (c.volume ?: 0).toDouble()
        val next = max(0.0, current - amount)
        val updated = c.copy(volume = next.roundToInt())
        val newContainers = merged.containers.toMutableList()
        newContainers[idx] = updated
        saveMerged(StoredMachineConfigWire(containers = newContainers))
        bump()
    }

    override suspend fun applyCellVolumes(updates: List<CellVolumeUpdate>) {
        if (updates.isEmpty()) return
        mutex.withLock {
            val merged = loadMerged() ?: return@withLock
            val byNumber = updates.associateBy { it.containerNumber }
            val newContainers =
                merged.containers.map { c ->
                    val u = byNumber[c.containerNumber] ?: return@map c
                    c.copy(volume = u.volumeMl.coerceAtLeast(0))
                }
            saveMerged(StoredMachineConfigWire(containers = newContainers))
            bump()
        }
        telemetryService.get().sendCellVolumeImportFromConfig()
    }

    private fun mapStoredContainerToDrink(c: StoredContainerWire): DrinkContainer {
        val p = c.product
        return DrinkContainer(
            containerNumber = c.containerNumber,
            sodaStatus = null,
            product =
                DrinkProduct(
                    id = p.id,
                    name = p.name,
                    taste =
                        DrinkTaste(
                            id = p.taste.id,
                            name = p.taste.name,
                            mediaKey = p.taste.mediaKey,
                            hexColor = p.taste.hexColor,
                        ),
                    dosage =
                        DrinkDosage(
                            conversionFactor = p.dosage.conversionFactor,
                            drinkVolume = p.dosage.drinkVolume,
                            product = p.dosage.product,
                            water = p.dosage.water,
                        ),
                    dPrices = p.dPrices.map { pr -> DrinkPrice(volume = pr.volume, priceRub = pr.price) },
                ),
            volumeMl = c.volume ?: 0,
            minVolumeMl = c.minVolume,
            isActive = c.isActive,
        )
    }

    override suspend fun getTableRows(): List<MachineInventoryTableRow> {
        val merged = loadMerged() ?: return emptyList()
        return merged.containers.map { c ->
            val p = c.product
            val p300 = p.dPrices.firstOrNull { it.volume == 300 }?.price
            val p700 = p.dPrices.firstOrNull { it.volume == 700 }?.price
            MachineInventoryTableRow(
                cellNumber = c.containerNumber,
                ingredientId = p.id,
                catalogTitle = c.catalogTitle,
                tasteName = p.taste.name,
                brandName = p.producingCompany.name,
                price300Rub = p300,
                price700Rub = p700,
                active = c.isActive,
                volumeMl = c.volume ?: 0,
                minVolumeMl = c.minVolume,
                maxVolumeMl = c.maxVolume,
                drinkVolumeMl = p.dosage.drinkVolume,
            )
        }
    }
}
