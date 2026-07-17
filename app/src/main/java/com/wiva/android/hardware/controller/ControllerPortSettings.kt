package com.wiva.android.hardware.controller

/**
 * Параметры serial для контроллера (см. `docs/SERIAL_PORTS_IMPLEMENTATION.md`,
 * `serial/config: 9600, 8 data bits, no parity).
 *
 * На Android путь к устройству задаётся платформенно (USB/serial); пока используется заглушка
 * транспорта — поле носит документирующий характер до появления реального драйвера.
 */
data class ControllerPortSettings(
 /** Логический или физический путь (в эталоне Electron: `/dev/ttyS0` и т.п.). */
    val devicePath: String,
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val parityNone: Boolean = true,
) {
    companion object {
 /** Значения по умолчанию из SERIAL_PORTS_IMPLEMENTATION.md (контроллер). */
        val DEFAULT =
            ControllerPortSettings(
                devicePath = "/dev/ttyS0",
                baudRate = 9600,
                dataBits = 8,
                parityNone = true,
            )
    }
}
