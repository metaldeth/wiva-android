package com.wiva.android.domain.telemetry

import com.wiva.android.data.telemetry.inventory.StoredDosageWire
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON categoryConfigMachine для cellStoreImportTopic — как
 * [ buildCategoryConfigMachine](mergeBaseAndMatrix.ts).
 */
object CategoryConfigMachineBuilder {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Serializable
    private data class Entry(
        val description: String,
        val key: String,
        val name: String,
        val type: String,
        val value: String = "",
    )

    private val rows: List<Entry> =
        listOf(
            Entry(
                description = "Объём готового напитка",
                key = "DrinkVolume",
                name = "Объём напитка",
                type = "NUMBER",
            ),
            Entry(
                description = "Вода",
                key = "Water",
                name = "Вода",
                type = "NUMBER",
            ),
            Entry(
                description = "Продукт",
                key = "Product",
                name = "Продукт",
                type = "NUMBER",
            ),
            Entry(
                description = "Фактор общения",
                key = "ConversionFactor",
                name = "Фактор общения",
                type = "NUMBER",
            ),
        )

    fun build(dosage: StoredDosageWire): String {
        val values =
            mapOf(
                "DrinkVolume" to dosage.drinkVolume.toDouble(),
                "Water" to dosage.water,
                "Product" to dosage.product,
                "ConversionFactor" to dosage.conversionFactor,
            )
        val out =
            rows.map { e ->
                val v = values[e.key] ?: 0.0
                e.copy(value = formatNumber(v))
            }
        return json.encodeToString(ListSerializer(Entry.serializer()), out)
    }

    private fun formatNumber(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "0"
        val asInt = v.toInt()
        return if (kotlin.math.abs(v - asInt) < 1e-9) asInt.toString() else v.toString()
    }
}
