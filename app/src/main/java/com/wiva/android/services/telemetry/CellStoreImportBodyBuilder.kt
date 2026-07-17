package com.wiva.android.services.telemetry

import com.wiva.android.data.telemetry.inventory.CellStoreMatrixBodyWire
import com.wiva.android.data.telemetry.inventory.MatrixCellWire
import com.wiva.android.data.telemetry.inventory.MatrixPriceWire
import com.wiva.android.data.telemetry.inventory.MatrixProductWire
import com.wiva.android.data.telemetry.inventory.StoredMachineConfigWire
import com.wiva.android.domain.telemetry.CategoryConfigMachineBuilder

/**
 * Тело cellStoreImportTopic из merge-конфигурации — как
 * [ TelemetryManager.buildCellStoreImportBodyFromConfig].
 */
object CellStoreImportBodyBuilder {
    fun buildFromMerged(
        merged: StoredMachineConfigWire,
        savedMatrix: CellStoreMatrixBodyWire,
    ): CellStoreMatrixBodyWire {
        val sorted = merged.containers.sortedBy { it.containerNumber }
        val seen = mutableSetOf<Int>()
        val products = mutableListOf<MatrixProductWire>()
        for (c in sorted) {
            val ingredientId = c.product.id
            if (ingredientId in seen) continue
            seen.add(ingredientId)
            val cellNumbers =
                merged.containers
                    .filter { it.product.id == ingredientId }
                    .map { it.containerNumber }
                    .sorted()
            val categoryConfigMachine = CategoryConfigMachineBuilder.build(c.product.dosage)
            val prices =
                c.product.dPrices.map { p ->
                    MatrixPriceWire(id = null, volume = p.volume, price = p.price)
                }
            products.add(
                MatrixProductWire(
                    ingredientId = ingredientId,
                    cellNumbers = cellNumbers,
                    isActive = c.isActive,
                    categoryConfigMachine = categoryConfigMachine,
                    purposeConfigMachine = "",
                    prices = prices,
                ),
            )
        }
        val cells =
            sorted.map { c ->
                MatrixCellWire(
                    cellNumber = c.containerNumber,
                    maxVolume = c.maxVolume ?: 0,
                    minVolume = c.minVolume ?: 0,
                    isActive = c.isActive,
                    categoryConfigMachine = CategoryConfigMachineBuilder.build(c.product.dosage),
                    purposeConfigMachine = "",
                )
            }
        return CellStoreMatrixBodyWire(
            products = products,
            cells = cells,
            cellCups = savedMatrix.cellCups,
            cellWaters = savedMatrix.cellWaters,
            cellDisposables = savedMatrix.cellDisposables,
            mixOfTastes = savedMatrix.mixOfTastes,
        )
    }
}
