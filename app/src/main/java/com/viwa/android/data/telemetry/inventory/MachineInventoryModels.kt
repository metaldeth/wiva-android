package com.viwa.android.data.telemetry.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- WS: baseIngredientRequestExportTopic (иерархия) ---

@Serializable
data class BaseIngredientBrandWire(
    val id: Int,
    val name: String,
    val mediaKey: String = "",
    val ingredientLines: List<BaseIngredientLineWire> = emptyList(),
)

@Serializable
data class BaseIngredientLineWire(
    val id: Int,
    val name: String,
    val ingredients: List<BaseIngredientItemWire> = emptyList(),
)

@Serializable
data class BaseIngredientItemWire(
    val id: Int,
    val name: String,
    val mediaKey: String = "",
    val componentOnAmount: Double? = null,
    val components: List<MatrixComponentWire>? = null,
    val taste: MatrixTasteWire? = null,
    val cellCategory: MatrixNamedIdWire? = null,
    val sportPit: MatrixSportPitWire? = null,
)

@Serializable
data class MatrixSportPitWire(
    val id: Int,
    val name: String,
    val mediaKey: String? = null,
)

// --- WS: cellStore matrix ---

@Serializable
data class MatrixPriceWire(
    val id: Int? = null,
    val volume: Int,
    val price: Int,
)

@Serializable
data class MatrixTasteWire(
    val id: Int,
    val name: String,
    val mediaKey: String? = null,
    val hexColor: String? = null,
)

@Serializable
data class MatrixBrandWire(
    val id: Int,
    val name: String,
    val mediaKey: String? = null,
)

@Serializable
data class MatrixNamedIdWire(
    val id: Int,
    val name: String,
)

@Serializable
data class MatrixComponentWire(
    val id: Int,
    val name: String,
    val qty: Double,
    val unit: String,
)

@Serializable
data class MatrixProductWire(
    val ingredientId: Int,
    val cellNumbers: List<Int> = emptyList(),
    val prices: List<MatrixPriceWire> = emptyList(),
    val categoryConfigMachine: String = "",
    val isActive: Boolean = true,
    val purposeConfigMachine: String? = null,
    val name: String? = null,
    val mediaKey: String? = null,
    val taste: MatrixTasteWire? = null,
    val brand: MatrixBrandWire? = null,
    val ingredientLine: MatrixNamedIdWire? = null,
    val components: List<MatrixComponentWire>? = null,
    val componentOnAmount: Double? = null,
    val cellCategory: MatrixNamedIdWire? = null,
    val cellPurpose: MatrixNamedIdWire? = null,
    val sportPit: MatrixSportPitWire? = null,
)

@Serializable
data class MatrixCellWire(
    val id: Int? = null,
    val cellNumber: Int,
    val maxVolume: Int = 0,
    val minVolume: Int = 0,
    val expirationTimer: String? = null,
    val categoryConfigMachine: String? = null,
    val purposeConfigMachine: String? = null,
    val isActive: Boolean? = null,
    val cellCategory: MatrixNamedIdWire? = null,
    val cellPurpose: MatrixNamedIdWire? = null,
)

@Serializable
data class MatrixCellWaterWire(
    val cellNumber: Int,
    val type: String = "",
    val maxVolume: Int = 0,
    val minVolume: Int = 0,
    val filterValue: Int? = null,
    val expirationTimer: String? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class MatrixCellCupWire(
    val cellNumber: Int,
    val cupVolume: Int = 0,
    val isCount: Boolean? = null,
    val maxVolume: Int? = null,
    val minVolume: Int? = null,
)

@Serializable
data class MatrixCellDisposableWire(
    val id: Int? = null,
    val name: String? = null,
    val cellNumber: Int,
    val isCount: Boolean? = null,
    val minVolume: Int? = null,
)

@Serializable
data class MixOfTastesWire(
    val isActive: Boolean? = null,
    val dosages: List<MatrixPriceWire> = emptyList(),
)

@Serializable
data class CellStoreMatrixBodyWire(
    val products: List<MatrixProductWire> = emptyList(),
    val cells: List<MatrixCellWire> = emptyList(),
    val cellWaters: List<MatrixCellWaterWire> = emptyList(),
    val cellCups: List<MatrixCellCupWire> = emptyList(),
    val cellDisposables: List<MatrixCellDisposableWire> = emptyList(),
    val mixOfTastes: MixOfTastesWire? = null,
)

// --- Плоская база (память / JSON map string keys) ---

@Serializable
data class BaseIngredientFlatWire(
    val name: String,
    val mediaKey: String = "",
    val taste: MatrixTasteWire? = null,
    val componentOnAmount: Double? = null,
    val components: List<MatrixComponentWire>? = null,
    val brand: MatrixBrandWire? = null,
    val ingredientLine: MatrixNamedIdWire? = null,
)

@Serializable
data class BaseIngredientsFileWire(
    val entries: Map<String, BaseIngredientFlatWire> = emptyMap(),
)

// --- Итог merge (.containers) ---

@Serializable
data class StoredCupWire(
    val name: String = "По умолчанию",
    val mediaKey: String = "default",
    val id: Int = 0,
)

@Serializable
data class StoredTasteWire(
    val name: String,
    val id: Int,
    val hexColor: String? = null,
    val mediaKey: String? = null,
)

@Serializable
data class StoredProducingCompanyWire(
    val name: String,
    val mediaKey: String = "",
    val id: Int = 0,
)

@Serializable
data class StoredComponentDtoWire(
    val componentName: String,
    val componentAmount: Double,
    val measurementUnit: String,
)

@Serializable
data class StoredPriceWire(
    val volume: Int,
    val price: Int,
)

@Serializable
data class StoredDosageWire(
    val conversionFactor: Double,
    val drinkVolume: Int,
    val product: Double,
    val water: Double,
)

@Serializable
data class StoredProductWire(
    val name: String,
    val taste: StoredTasteWire,
    val producingCompany: StoredProducingCompanyWire,
    val componentOnAmount: Double = 0.0,
    val components: List<StoredComponentDtoWire> = emptyList(),
 /** Как Condition.Powder в electron */
    val condition: String = "Powder",
    val dPrices: List<StoredPriceWire> = emptyList(),
    val dosage: StoredDosageWire,
    val id: Int,
)

@Serializable
data class StoredContainerWire(
    val containerNumber: Int,
    val cup: StoredCupWire = StoredCupWire(),
    val product: StoredProductWire,
    val isActive: Boolean = true,
    val hasSmallDrink: Boolean = true,
    val volume: Int? = null,
    val minVolume: Int? = null,
    val maxVolume: Int? = null,
 /** Полное имя для UI: бренд · линия · продукт */
    val catalogTitle: String,
)

@Serializable
data class StoredMachineConfigWire(
    val containers: List<StoredContainerWire> = emptyList(),
)
