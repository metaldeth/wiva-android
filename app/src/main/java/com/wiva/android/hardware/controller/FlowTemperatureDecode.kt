package com.wiva.android.hardware.controller

/**
 * Декодирует байт температуры потока с контроллера (ответ на [RequestCommand.ReadFlowTemperature]).
 *
 * На железе: **0°C** передаётся байтом **0**; отрицательные значения кодируются как **temp + 255**
 * (например −5 → 250, −1 → 254). Положительные **1…127** передаются как есть.
 *
 * @param rawUnsigned байт 0…255 (`toInt and 0xff`)
 */
fun decodeFlowTemperatureByte(rawUnsigned: Int): Int {
    val u = rawUnsigned and 0xff
    return when {
        u == 0 -> 0
        u < 128 -> u
        else -> u - 255
    }
}
