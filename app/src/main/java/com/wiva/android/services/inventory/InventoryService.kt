package com.wiva.android.services.inventory

import com.wiva.android.domain.repository.MachineInventoryRepository
import com.wiva.android.services.telemetry.WivaTelemetryService
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class InventoryService
@Inject
constructor(
    private val inventoryRepository: MachineInventoryRepository,
    private val telemetryService: WivaTelemetryService,
) {
    suspend fun applyWriteOff(
        containerNumber: Int,
        volumeMl: Int,
        concentrationRatio: Double,
    ) {
        val container = inventoryRepository.findContainerByNumber(containerNumber) ?: return
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

        inventoryRepository.deductContainerVolume(containerNumber, productWriteOff)

        if (!telemetryService.sendCellVolumeImportFromConfig()) {
            Timber.tag(TAG).w("cellVolumeImportTopic не отправлен (offline или нет данных)")
        }
    }

    companion object {
        private const val TAG = "InventoryService"
    }
}
