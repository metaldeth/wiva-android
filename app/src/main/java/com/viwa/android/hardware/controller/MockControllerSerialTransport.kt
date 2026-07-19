package com.viwa.android.hardware.controller

import timber.log.Timber

/**
 * Демо-порт: не открывает USB; как `SerialPortConnection` при пути `MOCK_*`.
 * После [open] считается открытым; [write] только в logcat.
 */
class MockControllerSerialTransport : ControllerSerialTransport {
    override var isOpen: Boolean = false
        private set

    private var rxListener: ((ByteArray) -> Unit)? = null

    override suspend fun open(settings: ControllerPortSettings): Boolean {
        close()
        isOpen = true
        Timber.tag(TAG).i(
            "Mock serial: open path=%s baud=%d",
            settings.devicePath,
            settings.baudRate,
        )
        return true
    }

    override fun close() {
        if (isOpen) {
            Timber.tag(TAG).i("Mock serial: close")
        }
        isOpen = false
    }

    override suspend fun write(bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { b -> "%02x".format(b.toInt() and 0xff) }
        Timber.tag(TAG).d("Mock serial TX (%d bytes): %s", bytes.size, hex)
    }

    override fun setOnBytesReceived(listener: ((ByteArray) -> Unit)?) {
        rxListener = listener
    }

    companion object {
        private const val TAG = "ViwaController"
    }
}
