package com.viwa.android.hardware.controller

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/***/
class ControllerProtocolTest {
    private val protocol = ControllerProtocol()

    @Test
    fun encodesTxFeLenCmdBody() {
        val result =
            protocol.formatRequest(
                RequestCommand.ChooseDrink,
                byteArrayOf(1.toByte(), 2.toByte(), 3.toByte()),
            )
        assertArrayEquals(
            byteArrayOf(0xfe.toByte(), 4.toByte(), 0x50.toByte(), 1.toByte(), 2.toByte(), 3.toByte()),
            result,
        )
    }

    @Test
    fun parsesRxD5LenCmdBody() {
        val buf = byteArrayOf(0xd5.toByte(), 3.toByte(), 0x22.toByte(), 0xaa.toByte(), 0xbb.toByte())
        val msg = protocol.parseResponse(buf)
        assertNotNull(msg)
        assertEquals(ResponseCommand.ControllerACK, msg!!.command)
        assertArrayEquals(byteArrayOf(0xaa.toByte(), 0xbb.toByte()), msg.payload)
    }

    @Test
    fun lenIsBodyLengthPlusOne() {
        val result =
            protocol.formatRequest(
                RequestCommand.ReadFirmwareVersion,
                byteArrayOf(),
            )
        assertEquals(1, result[1].toInt() and 0xff)
    }

    @Test
    fun emptyBodyLenIs1() {
        val result =
            protocol.formatRequest(
                RequestCommand.ReadFirmwareVersion,
                byteArrayOf(),
            )
        assertArrayEquals(
            byteArrayOf(0xfe.toByte(), 1.toByte(), 0x97.toByte()),
            result,
        )
    }

    @Test
    fun incompleteRxReturnsNull() {
        val buf = byteArrayOf(0xd5.toByte(), 10.toByte(), 0x22.toByte())
        assertNull(protocol.parseResponse(buf))
    }
}
