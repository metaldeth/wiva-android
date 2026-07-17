package com.wiva.android.hardware.controller

/**
 * Транспорт байтов контроллера (USB serial или мок).
 * Реализации создаёт [ControllerHardwareManager], не Hilt-синглтон.
 */
interface ControllerSerialTransport {
    val isOpen: Boolean

    suspend fun open(settings: ControllerPortSettings): Boolean

    fun close()

    suspend fun write(bytes: ByteArray)

    fun setOnBytesReceived(listener: ((ByteArray) -> Unit)?)
}
