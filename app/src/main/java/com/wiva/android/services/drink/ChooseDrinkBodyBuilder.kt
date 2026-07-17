package com.wiva.android.services.drink

/**
 * тело [RequestCommand.ChooseDrink] (9 байт).
 */
object ChooseDrinkBodyBuilder {
    private const val DISPENSER_TIME_MAX = 255
    private const val WATER_BYTE_MAX = 100
    private const val PORT_MIN = 1
    private const val PORT_MAX = 255
    private const val TOF_MIN = 0
    private const val TOF_MAX = 2

    fun build(
        physicalPort: Int,
        dispenserWorkTimeSec: Double,
        waterMl: Double,
        tof: Int,
    ): ByteArray {
        val port = physicalPort.coerceIn(PORT_MIN, PORT_MAX)
        val timeRaw =
            if (dispenserWorkTimeSec.isFinite()) {
                kotlin.math.round(dispenserWorkTimeSec * 10.0).toInt()
            } else {
                0
            }
        val timeByte = timeRaw.coerceIn(0, DISPENSER_TIME_MAX)
        val waterRaw =
            if (waterMl.isFinite()) {
                kotlin.math.round(waterMl / 10.0).toInt()
            } else {
                0
            }
        val waterByte = waterRaw.coerceIn(0, WATER_BYTE_MAX)
        val tofClamped = tof.coerceIn(TOF_MIN, TOF_MAX)
        return byteArrayOf(0x01, port.toByte(), timeByte.toByte(), waterByte.toByte(), 0, 0, 0, 0, tofClamped.toByte())
    }
}
