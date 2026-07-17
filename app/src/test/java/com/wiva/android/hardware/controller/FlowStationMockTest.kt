package com.wiva.android.hardware.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowStationMockTest {

    private fun makeConnection(
        events: MutableList<Pair<ResponseCommand, ByteArray>>,
    ): ControllerConnection {
        val protocol = ControllerProtocol()
        val transport = MockControllerSerialTransport()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return ControllerConnection(
            devicePath = ControllerConstants.MOCK_CONTROLLER_PATH,
            baudRate = 9600,
            protocol = protocol,
            transport = transport,
            connectionScope = scope,
            onCommandLog = {},
            onNotConnected = {},
            onPortNotFound = {},
            emitResponse = { cmd, payload -> events.add(cmd to payload) },
        )
    }

    @Test
    fun mockPath_readFlowTemperature_emitsTemperaturePayload() =
        runBlocking {
            val events = mutableListOf<Pair<ResponseCommand, ByteArray>>()
            val conn = makeConnection(events)
            conn.init()
            conn.sendCommand(RequestCommand.ReadFlowTemperature, ControllerConstants.DEFAULT_BODY)
            delay(150)

            val event = events.firstOrNull { it.first == ResponseCommand.ControllerTimeoutResetActivate }
            assertTrue("ControllerTimeoutResetActivate not received", event != null)
            assertEquals("TH0 должен быть 22°C", 22, event!!.second[0].toInt() and 0xff)
            assertEquals("TH1 должен быть 45°C", 45, event.second[1].toInt() and 0xff)
        }

    @Test
    fun mockPath_setFlowRgb_emitsControllerAck() =
        runBlocking {
            val events = mutableListOf<Pair<ResponseCommand, ByteArray>>()
            val conn = makeConnection(events)
            conn.init()
            conn.sendCommand(
                RequestCommand.SetFlowRgb,
                byteArrayOf(128.toByte(), 0, 255.toByte(), 0, 0),
            )
            delay(150)

            assertTrue(
                "ControllerACK не получен на SetFlowRgb",
                events.any { it.first == ResponseCommand.ControllerACK },
            )
        }

    @Test
    fun mockPath_readFlowBucketStatus_emitsBucketNotFull() =
        runBlocking {
            val events = mutableListOf<Pair<ResponseCommand, ByteArray>>()
            val conn = makeConnection(events)
            conn.init()
            conn.sendCommand(RequestCommand.ReadFlowBucketStatus, ControllerConstants.DEFAULT_BODY)
            delay(150)

            val event = events.firstOrNull { it.first == ResponseCommand.CupSensorStatusAnswer }
            assertTrue("CupSensorStatusAnswer не получен", event != null)
            assertEquals("TrS=0 (ведро не заполнено)", 0, event!!.second[0].toInt() and 0xff)
        }
}
