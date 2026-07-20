package com.viwa.android.services.calibration

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import com.viwa.android.data.repository.TelemetryCellsRepositoryImpl
import com.viwa.android.domain.model.TelemetryCell
import com.viwa.android.domain.model.TelemetryCellsSnapshot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyrupCalibrationInventoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var configRepository: FakeConfigRepository
    private lateinit var repository: TelemetryCellsRepositoryImpl
    private lateinit var inventory: SyrupCalibrationInventory

    @Before
    fun setUp() {
        configRepository = FakeConfigRepository()
        repository = TelemetryCellsRepositoryImpl(configRepository)
        inventory =
            SyrupCalibrationInventory(
                cellsRepository = repository,
                conversionFactorMigration = SyrupConversionFactorMigration(configRepository),
            )
    }

    @Test
    fun listContainers_usesConversionFactorFromCell() = runTest {
        // given
        repository.replaceSnapshot(
            TelemetryCellsSnapshot(
                cells =
                    listOf(
                        TelemetryCell(
                            uuid = "u1",
                            cellNumber = 1,
                            productUuid = "p1",
                            productName = "Cherry",
                            maxVolume = 5000,
                            conversionFactor = 5.5,
                        ),
                    ),
            ),
        )

        // when
        val containers = inventory.listContainersForCalibration()

        // then
        assertEquals(5.5, containers.single().conversionFactor, 0.0001)
    }

    @Test
    fun migrateLegacyConversionFactors_mergesIntoSnapshotWhenCellAtDefault() = runTest {
        // given
        repository.replaceSnapshot(
            TelemetryCellsSnapshot(
                cells =
                    listOf(
                        TelemetryCell(
                            uuid = "u1",
                            cellNumber = 1,
                            productUuid = "p1",
                            maxVolume = 5000,
                        ),
                        TelemetryCell(
                            uuid = "u2",
                            cellNumber = 2,
                            productUuid = "p2",
                            maxVolume = 5000,
                            conversionFactor = 3.0,
                        ),
                    ),
            ),
        )
        configRepository.setJson(
            JsonStoreKeys.SYRUP_CONVERSION_FACTORS,
            json.encodeToString(MapSerializer(Int.serializer(), Double.serializer()), mapOf(1 to 6.0)),
        )

        // when
        inventory.migrateLegacyConversionFactorsIfNeeded()

        // then
        val cells = repository.getSnapshot()!!.cells
        assertEquals(6.0, cells.first { it.cellNumber == 1 }.conversionFactor, 0.0001)
        assertEquals(3.0, cells.first { it.cellNumber == 2 }.conversionFactor, 0.0001)
    }

    @Test
    fun updateContainerConversionFactor_persistsInSnapshot() = runTest {
        // given
        repository.replaceSnapshot(
            TelemetryCellsSnapshot(
                cells =
                    listOf(
                        TelemetryCell(
                            uuid = "u1",
                            cellNumber = 1,
                            productUuid = "p1",
                            maxVolume = 5000,
                        ),
                    ),
            ),
        )

        // when
        val updated = inventory.updateContainerConversionFactor(1, 9.9).getOrThrow()

        // then
        assertEquals(9.9, updated.conversionFactor, 0.0001)
        assertEquals(9.9, repository.getSnapshot()!!.cells.single().conversionFactor, 0.0001)
    }

    private class FakeConfigRepository : ConfigRepository {
        private val store = mutableMapOf<String, String>()

        override suspend fun get(key: String): String? = store[key]

        override suspend fun set(key: String, value: String) {
            store[key] = value
        }

        override suspend fun delete(key: String) {
            store.remove(key)
        }

        override suspend fun getJson(key: String): String? = store[key]

        override suspend fun setJson(key: String, json: String) {
            store[key] = json
        }
    }
}
