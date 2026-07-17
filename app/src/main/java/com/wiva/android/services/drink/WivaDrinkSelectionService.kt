package com.wiva.android.services.drink

import com.wiva.android.domain.model.customer.DrinkContainer
import com.wiva.android.domain.model.customer.DrinkWaterOption
import com.wiva.android.domain.model.customer.toTofByte
import com.wiva.android.hardware.controller.ControllerGateway
import com.wiva.android.hardware.controller.RequestCommand
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * расчёт рецепта и [RequestCommand.ChooseDrink].
 *
 * **Без калибровки воды готовка не запускается** — [flowRateMlPerSec] обязателен и > 0 (проверка в [com.wiva.android.services.preparing.PreparingManager]).
 */
@Singleton
class WivaDrinkSelectionService
@Inject
constructor(
    private val controller: ControllerGateway,
) {
 /**
 * @param drinkVolumeMl выбранный объём (300 или 700)
 * @param flowRateMlPerSec скорость налива из калибровки воды, мл/с
 * @return примерное время готовки, сек (round(waterMl / flowRateMlPerSec))
 */
    suspend fun chooseDrink(
        container: DrinkContainer,
        drinkVolumeMl: Int,
        waterOption: DrinkWaterOption?,
        concentrationRatio: Double,
        flowRateMlPerSec: Double,
    ): Int {
        val dosage = container.product.dosage
        val physicalPort = (container.containerNumber + 8).coerceIn(1, 255)
        val ratio = drinkVolumeMl.toDouble() / dosage.drinkVolume.toDouble()
        val k = concentrationRatio
        val productAmount = dosage.product * ratio * k
        val dispenserWorkTimeSec = productAmount / dosage.conversionFactor
        val waterMl =
            DrinkPreparationCalculations.waterMlForDrink(
                dosageWaterMl = dosage.water,
                drinkVolumeMl = drinkVolumeMl,
                recipeDrinkVolumeMl = dosage.drinkVolume,
            )
        val preparingTime =
            DrinkPreparationCalculations.preparingTimeSec(waterMl, flowRateMlPerSec)

        val tofRaw =
            when {
                waterOption != null -> waterOption.toTofByte()
                container.sodaStatus == true -> 2
                else -> 0
            }
        val tof = tofRaw.coerceIn(0, 2)
        val body =
            ChooseDrinkBodyBuilder.build(
                physicalPort = physicalPort,
                dispenserWorkTimeSec = dispenserWorkTimeSec,
                waterMl = waterMl,
                tof = tof,
            )
        Timber.tag(TAG).i(
            "ChooseDrink logicalPort=%d physicalPort=%d volumeMl=%d tof=%d body=%s",
            container.containerNumber,
            physicalPort,
            drinkVolumeMl,
            tof,
            body.joinToString(" ") { "%02x".format(it.toInt() and 0xff) },
        )
        controller.sendCommand(RequestCommand.ChooseDrink, body)
        return preparingTime
    }

    companion object {
        private const val TAG = "WivaDrinkSelection"
    }
}
