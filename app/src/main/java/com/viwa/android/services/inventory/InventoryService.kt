package com.viwa.android.services.inventory

import com.viwa.android.data.remote.telemetry.mvp.TelemetryCellsSyncCoordinator
import com.viwa.android.data.remote.telemetry.mvp.cells.CellVolumeUpdateWire
import com.viwa.android.domain.customer.TelemetryCellsSnapshotAdapter
import com.viwa.android.domain.repository.TelemetryCellsRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class InventoryService
@Inject
constructor(
    private val cellsRepository: TelemetryCellsRepository,
    private val cellsSyncCoordinator: TelemetryCellsSyncCoordinator,
) {
    suspend fun applyWriteOff(
        containerNumber: Int,
        volumeMl: Int,
        concentrationRatio: Double,
    ) {
        val snapshot = cellsRepository.getSnapshot() ?: return
        val container =
            TelemetryCellsSnapshotAdapter.toDrinkContainers(snapshot)
                .find { it.containerNumber == containerNumber }
                ?: return
        val dosage = container.product.dosage
        if (dosage.drinkVolume <= 0) return

        val (productWriteOff, _) =
            InventoryWriteOffMath.computeProductAndWaterMl(
                drinkVolume = dosage.drinkVolume,
                dosageProduct = dosage.product,
                dosageWater = dosage.water,
                volumeMl = volumeMl,
                concentrationRatio = concentrationRatio,
            )

        val cell = snapshot.cells.find { it.cellNumber == containerNumber } ?: return
        val nextVolume = (cell.volume - productWriteOff).coerceAtLeast(0.0).toInt()
        cellsSyncCoordinator.onLocalVolumeChange(
            listOf(CellVolumeUpdateWire(uuid = cell.uuid, volume = nextVolume)),
        )
        Timber.tag(TAG).d("write-off cell=%d volume=%d -> %d", containerNumber, cell.volume, nextVolume)
    }

    companion object {
        private const val TAG = "InventoryService"
    }
}
