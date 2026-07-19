package com.wiva.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpInventorySnapshotMapperTest {

    @Test
    fun productPickerSource_isSnapshotProducts_notExternalHttp() {
        // given — только локальный snapshot (без REST/HTTP слоя)
        val localProducts =
            listOf(
                TelemetryProduct(uuid = "p-a", name = "Лимон", tasteMediaKey = "lemon"),
                TelemetryProduct(uuid = "p-b", name = "Лайм", tasteMediaKey = "lime"),
            )
        val snapshot =
            TelemetryCellsSnapshot(
                products = localProducts,
                cells =
                    listOf(
                        TelemetryCell(
                            uuid = "c-1",
                            cellNumber = 1,
                            productUuid = "p-a",
                            productName = "Лимон",
                            tasteMediaKey = "lemon",
                            volume = 100,
                            maxVolume = 1000,
                        ),
                    ),
            )

        // when
        val (rows, products) = mapMvpInventoryFromSnapshot(snapshot)

        // then
        assertEquals(localProducts, products)
        assertEquals(1, rows.size)
        assertEquals("p-a", rows.first().productUuid)
        assertTrue(products.none { it.uuid.isBlank() })
    }
}
