package com.viwa.android.data.payment.aqsi.serial

/** Arcus2 USB serial session (production or test double). */
interface AqsiSerialLink {
    val isOpen: Boolean

    fun read(timeoutMs: Int = 100): ByteArray?

    fun write(data: ByteArray, timeoutMs: Int = 200)

    fun close()
}
