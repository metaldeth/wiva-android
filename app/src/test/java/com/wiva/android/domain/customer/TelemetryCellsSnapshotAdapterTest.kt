package com.wiva.android.domain.customer

import com.wiva.android.domain.model.TelemetryCell
import com.wiva.android.domain.model.TelemetryCellsSnapshot
import com.wiva.android.domain.model.TelemetryProduct
import com.wiva.android.ui.screens.customer.WivaElectronAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryCellsSnapshotAdapterTest {

    @Test
    fun tasteMediaKey_mapsToDrinkContainerDisplayFields() {
        // given
        val snapshot =
            TelemetryCellsSnapshot(
                products =
                    listOf(
                        TelemetryProduct(uuid = "prod-1", name = "Чёрная вишня", tasteMediaKey = "cherry"),
                    ),
                cells =
                    listOf(
                        TelemetryCell(
                            uuid = "cell-1",
                            cellNumber = 1,
                            productUuid = "prod-1",
                            productName = "Чёрная вишня",
                            tasteMediaKey = "cherry",
                            volume = 1200,
                            maxVolume = 5000,
                            blockVolume = 100,
                            sosVolume = 300,
                            dosage1Price = 9900,
                            dosage2Price = 14900,
                        ),
                    ),
            )

        // when
        val containers = TelemetryCellsSnapshotAdapter.toDrinkContainers(snapshot)

        // then
        assertEquals(1, containers.size)
        val container = containers.first()
        assertEquals(1, container.containerNumber)
        assertEquals("Чёрная вишня", container.product.name)
        assertEquals("cherry", container.product.taste.mediaKey)
        assertEquals(1200, container.volumeMl)
        assertEquals(100, container.minVolumeMl)
        assertEquals(listOf(300 to 99, 700 to 149), container.product.dPrices.map { it.volume to it.priceRub })
        assertNotNull(WivaElectronAssets.horizontalCardImageUri(container.product.taste.mediaKey))
        assertTrue(WivaElectronAssets.hasPreparingVideoAsset(container.product.taste.mediaKey))
    }

    @Test
    fun nullProductUuid_returnsNullDrinkContainer() {
        // given
        val cell =
            TelemetryCell(
                uuid = "cell-empty",
                cellNumber = 2,
                productUuid = null,
                volume = 0,
                maxVolume = 5000,
            )

        // when
        val container = TelemetryCellsSnapshotAdapter.toDrinkContainer(cell)

        // then
        assertNull(container)
        assertTrue(TelemetryCellsSnapshotAdapter.toDrinkContainers(TelemetryCellsSnapshot(cells = listOf(cell))).isEmpty())
    }
}
