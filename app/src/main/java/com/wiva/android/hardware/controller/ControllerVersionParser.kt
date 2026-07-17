package com.wiva.android.hardware.controller

/** Парсинг версии прошивки контроллера. */
object ControllerVersionParser {
    fun parseVersion(payload: ByteArray): String =
        payload.joinToString(".") { b -> (b.toInt() and 0xff).toString().padStart(2, '0') }
}
