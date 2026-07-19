package com.wiva.android.data.repository

import com.wiva.android.data.local.db.JsonStoreKeys
import com.wiva.android.domain.model.TelemetryCell
import com.wiva.android.domain.model.TelemetryCellsSnapshot
import com.wiva.android.domain.model.TelemetryProduct
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryCellsRepositoryImplTest {

    private val fakeStore = FakeConfigRepository()

    private fun repo() = TelemetryCellsRepositoryImpl(fakeStore)

    @Test
    fun replaceSnapshot_shouldReplaceEntireSnapshotIncludingProducts() =
        runBlocking {
            val r = repo()
            val snapshotA =
                TelemetryCellsSnapshot(
                    schemaHash = "hash-a",
                    contentRevision = 1,
                    products = listOf(TelemetryProduct("p1", "A", "cherry")),
                    cells =
                        listOf(
                            TelemetryCell(
                                uuid = "u1",
                                cellNumber = 1,
                                maxVolume = 5000,
                                productUuid = "p1",
                                productName = "A",
                                tasteMediaKey = "cherry",
                            ),
                        ),
                )
            val snapshotB =
                TelemetryCellsSnapshot(
                    schemaHash = "hash-b",
                    contentRevision = 2,
                    products = listOf(TelemetryProduct("p2", "B", "lemon")),
                    cells =
                        listOf(
                            TelemetryCell(
                                uuid = "u2",
                                cellNumber = 2,
                                maxVolume = 5000,
                                productUuid = "p2",
                                productName = "B",
                                tasteMediaKey = "lemon",
                            ),
                        ),
                )

            r.replaceSnapshot(snapshotA)
            r.replaceSnapshot(snapshotB)

            val loaded = r.getSnapshot()
            assertEquals(snapshotB.copy(savedAtEpochMs = loaded!!.savedAtEpochMs), loaded)
            assertEquals(listOf("p2"), loaded.products.map { it.uuid })
            assertEquals(listOf(2), loaded.cells.map { it.cellNumber })
        }

    @Test
    fun roundTrip_jsonStore_equalsOriginalExceptSavedAtTimestamp() =
        runBlocking {
            val r = repo()
            val original =
                TelemetryCellsSnapshot(
                    schemaHash = "abc",
                    contentRevision = 7,
                    products =
                        listOf(
                            TelemetryProduct("prod-cherry", "Вишня", "cherry"),
                            TelemetryProduct("prod-lemon", "Лимон", "lemon"),
                        ),
                    cells =
                        listOf(
                            TelemetryCell(
                                uuid = "550e8400-e29b-41d4-a716-446655440001",
                                cellNumber = 1,
                                productUuid = "prod-cherry",
                                productName = "Вишня",
                                tasteMediaKey = "cherry",
                                volume = 1200,
                                maxVolume = 5000,
                                sosVolume = 100,
                                dosage1Price = 9900,
                                dosage2Price = 14900,
                            ),
                        ),
                )

            r.replaceSnapshot(original)
            val loaded = r.getSnapshot()

            assertEquals(original.schemaHash, loaded?.schemaHash)
            assertEquals(original.contentRevision, loaded?.contentRevision)
            assertEquals(original.products, loaded?.products)
            assertEquals(original.cells, loaded?.cells)
            assertEquals(true, (loaded?.savedAtEpochMs ?: 0L) > 0L)
            assertEquals(true, fakeStore.strings.containsKey(JsonStoreKeys.TELEMETRY_CELLS_SNAPSHOT))
        }

    @Test
    fun clearSnapshot_removesStoredValue() =
        runBlocking {
            val r = repo()
            r.replaceSnapshot(
                TelemetryCellsSnapshot(
                    schemaHash = "x",
                    cells = listOf(TelemetryCell(uuid = "u", cellNumber = 1, maxVolume = 100)),
                ),
            )
            r.clearSnapshot()
            assertNull(r.getSnapshot())
            assertNull(fakeStore.strings[JsonStoreKeys.TELEMETRY_CELLS_SNAPSHOT])
        }

    private class FakeConfigRepository : ConfigRepository {
        val strings = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = strings[key]

        override suspend fun set(key: String, value: String) {
            strings[key] = value
        }

        override suspend fun delete(key: String) {
            strings.remove(key)
        }

        override suspend fun getJson(key: String): String? = strings[key]

        override suspend fun setJson(key: String, json: String) {
            strings[key] = json
        }
    }
}
