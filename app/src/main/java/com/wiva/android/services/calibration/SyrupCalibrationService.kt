package com.wiva.android.services.calibration

import com.wiva.android.data.remote.telemetry.mvp.TelemetryCellsSyncCoordinator
import com.wiva.android.data.remote.telemetry.mvp.cells.CellVolumeUpdateWire
import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.RequestCommand
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SyrupCalibrationService
@Inject
constructor(
    private val controllerGateway: ControllerGateway,
    private val calibrationInventory: SyrupCalibrationInventory,
    private val cellsSyncCoordinator: TelemetryCellsSyncCoordinator,
) {
    suspend fun pourTestSample(
        containerNumber: Int,
        targetProductMl: Double,
    ): Result<Unit> {
        val containers = calibrationInventory.listContainersForCalibration()
        val info = containers.find { it.containerNumber == containerNumber }
        if (info == null) {
            return Result.failure(IllegalArgumentException("Контейнер $containerNumber не найден"))
        }
        if (info.conversionFactor <= 0) {
            return Result.failure(IllegalStateException("conversionFactor должен быть > 0"))
        }
        val target =
            targetProductMl
                .takeIf { it.isFinite() && it > 0 }
                ?.let { t -> t.coerceIn(1.0, 0xff * 10.0) }
                ?: return Result.failure(IllegalArgumentException("Целевой объём должен быть > 0"))
        val body =
            SyrupCalibrationMath.buildCalibratePourBody(
                containerNumber = containerNumber,
                targetProductMl = target,
                conversionFactor = info.conversionFactor,
            )
        controllerGateway.sendCommand(RequestCommand.ServiceCommand, body)
        deductVolume(containerNumber, target)
        Timber.i(
            "SyrupCalibration: тестовый налив container=%d target=%.1f мл body=%s",
            containerNumber,
            target,
            body.joinToString(",") { (it.toInt() and 0xff).toString() },
        )
        return Result.success(Unit)
    }

    suspend fun submitCalibrationResult(
        containerNumber: Int,
        actualVolumeMl: Double,
        targetProductMl: Double?,
    ): Result<Double> {
        if (!actualVolumeMl.isFinite() || actualVolumeMl <= 0) {
            return Result.failure(IllegalArgumentException("Фактический объём должен быть > 0"))
        }
        val containers = calibrationInventory.listContainersForCalibration()
        val info = containers.find { it.containerNumber == containerNumber }
        if (info == null) {
            return Result.failure(IllegalArgumentException("Контейнер $containerNumber не найден"))
        }
        val target =
            when {
                targetProductMl != null && targetProductMl.isFinite() && targetProductMl > 0 -> targetProductMl
                else -> info.defaultProductMl
            }
        if (target <= 0) {
            return Result.failure(IllegalStateException("Целевая порция должна быть > 0"))
        }
        val newCf =
            SyrupCalibrationMath.computeNewConversionFactor(
                currentConversionFactor = info.conversionFactor,
                actualVolumeMl = actualVolumeMl,
                targetProductMl = target,
            )
        calibrationInventory
            .updateContainerConversionFactor(containerNumber, newCf)
            .onFailure { return Result.failure(it) }
        Timber.i("SyrupCalibration: сохранён conversionFactor=%.4f container=%d", newCf, containerNumber)
        return Result.success(newCf)
    }

    private suspend fun deductVolume(containerNumber: Int, amount: Double) {
        val uuid = calibrationInventory.findCellUuid(containerNumber) ?: return
        val current = calibrationInventory.currentVolumeMl(containerNumber) ?: return
        val next = (current - amount).coerceAtLeast(0.0).toInt()
        cellsSyncCoordinator.onLocalVolumeChange(
            listOf(CellVolumeUpdateWire(uuid = uuid, volume = next)),
        )
    }
}
