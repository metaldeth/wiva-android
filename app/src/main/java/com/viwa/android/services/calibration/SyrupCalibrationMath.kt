package com.viwa.android.services.calibration

import kotlin.math.round

/**
 * Расчёты калибровки сиропа и тело [com.viwa.android.hardware.controller.RequestCommand.ServiceCommand]
 * (режим 0x09).
 */
object SyrupCalibrationMath {
    const val CELL_COUNT = 6

    fun physicalPort(containerNumber: Int): Int = (containerNumber + 8).coerceIn(1, 255)

 /**
 * Десятые доли секунды работы дозатора, 0–255.
 * [targetProductMl] / [conversionFactor] = секунды налива → *10 и round.
 */
    fun dispenserTimeTenths(
        targetProductMl: Double,
        conversionFactor: Double,
    ): Int {
        if (conversionFactor <= 0 || !targetProductMl.isFinite()) return 0
        val sec = targetProductMl / conversionFactor
        return round(sec * 10.0).toInt().coerceIn(0, 255)
    }

    fun buildCalibratePourBody(
        containerNumber: Int,
        targetProductMl: Double,
        conversionFactor: Double,
    ): ByteArray {
        val port = physicalPort(containerNumber)
        val tenths = dispenserTimeTenths(targetProductMl, conversionFactor)
        return byteArrayOf(0x09, 0, port.toByte(), tenths.toByte(), 0)
    }

 /** newCF = currentCF * (actualVolumeMl / targetProductMl) */
    fun computeNewConversionFactor(
        currentConversionFactor: Double,
        actualVolumeMl: Double,
        targetProductMl: Double,
    ): Double {
        require(targetProductMl > 0 && actualVolumeMl > 0) {
            "Объёмы должны быть > 0"
        }
        return currentConversionFactor * (actualVolumeMl / targetProductMl)
    }
}
