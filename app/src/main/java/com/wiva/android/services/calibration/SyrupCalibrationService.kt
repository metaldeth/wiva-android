package com.wiva.android.services.calibration

import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.RequestCommand
import com.wiva.android.domain.repository.MachineInventoryRepository
import com.wiva.android.services.telemetry.WivaTelemetryService
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SyrupCalibrationService
@Inject
constructor(
    private val controllerGateway: ControllerGateway,
    private val machineInventoryRepository: MachineInventoryRepository,
    private val telemetryService: WivaTelemetryService,
) {
 /**
 * Тестовый налив концентрата: ServiceCommand 0x52, тело [0x09,0,port,tenths,0].
 * В мок-режиме контроллер только логирует TX и отвечает ACK (успех).
 * После отправки команды списываем целевой объём из остатка этой ячейки.
 */
    suspend fun pourTestSample(
        containerNumber: Int,
        targetProductMl: Double,
    ): Result<Unit> {
        val containers = machineInventoryRepository.listContainersForCalibration()
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
        machineInventoryRepository.deductContainerVolume(containerNumber = containerNumber, amount = target)
        if (!telemetryService.sendCellVolumeImportFromConfig()) {
            Timber.w(
                "SyrupCalibration: списание %.1f мл сохранено локально, но cellVolumeImportTopic не отправлен (offline), container=%d",
                target,
                containerNumber,
            )
        }
        Timber.i(
            "SyrupCalibration: тестовый налив container=%d target=%.1f мл body=%s",
            containerNumber,
            target,
            body.joinToString(",") { (it.toInt() and 0xff).toString() },
        )
        return Result.success(Unit)
    }

 /**
 * Сохранение результата калибровки: newCF, локальный конфиг, cellVolumeImportTopic + cellStoreImportTopic.
 */
    suspend fun submitCalibrationResult(
        containerNumber: Int,
        actualVolumeMl: Double,
        targetProductMl: Double?,
    ): Result<Double> {
        if (!actualVolumeMl.isFinite() || actualVolumeMl <= 0) {
            return Result.failure(IllegalArgumentException("Фактический объём должен быть > 0"))
        }
        val containers = machineInventoryRepository.listContainersForCalibration()
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
        machineInventoryRepository
            .updateContainerConversionFactor(containerNumber, newCf)
            .onFailure { return Result.failure(it) }
        val volumeSent = telemetryService.sendCellVolumeImportFromConfig()
        val storeSent = telemetryService.sendCellStoreImportFromConfig()
        if (!volumeSent || !storeSent) {
            Timber.w(
                "Калибровка сохранена, но телеметрия не отправлена (WebSocket отключён), container=%d",
                containerNumber,
            )
        } else {
            Timber.i(
                "Калибровка: cellVolumeImportTopic и cellStoreImportTopic отправлены, container=%d",
                containerNumber,
            )
        }
        return Result.success(newCf)
    }
}
