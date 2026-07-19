package com.viwa.android.domain.telemetry

import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.data.repository.TelemetryCellsRepositoryImpl
import com.viwa.android.domain.model.TelemetryCell
import com.viwa.android.domain.model.TelemetryCellsSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CellUuidAllocatorTest {

    private val allocator = CellUuidAllocator()
    private val provider = DefaultPhysicalCellSchemaProvider()

    @Test
    fun uuidForCellNumber_shouldReturnSameUuidAfterPersistRoundTrip() =
        runBlocking {
            val fakeStore = FakeConfigRepository()
            val repository = TelemetryCellsRepositoryImpl(fakeStore)
            val physicalCells = provider.physicalCells()

            val firstPass =
                allocator.allocateForPhysicalCells(
                    physicalCells = physicalCells,
                    existingSnapshot = null,
                )
            val snapshot =
                TelemetryCellsSnapshot(
                    cells =
                        physicalCells.map { definition ->
                            TelemetryCell(
                                uuid = firstPass.getValue(definition.cellNumber),
                                cellNumber = definition.cellNumber,
                                maxVolume = definition.maxVolume,
                            )
                        },
                )
            repository.replaceSnapshot(snapshot)

            val loaded = repository.getSnapshot()
            val secondPass =
                allocator.allocateForPhysicalCells(
                    physicalCells = physicalCells,
                    existingSnapshot = loaded,
                )

            assertEquals(firstPass, secondPass)
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
