package com.wiva.android.hardware.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerConnectionMockTest {

    @Test
    fun mockPath_sendReadFirmwareVersion_emitsControllerAck() =
        runBlocking {
            val protocol = ControllerProtocol()
            val transport = MockControllerSerialTransport()
            val events = mutableListOf<ResponseCommand>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val conn =
                ControllerConnection(
                    devicePath = ControllerConstants.MOCK_CONTROLLER_PATH,
                    baudRate = 9600,
                    protocol = protocol,
                    transport = transport,
                    connectionScope = scope,
                    onCommandLog = {},
                    onNotConnected = {},
                    onPortNotFound = {},
                    emitResponse = { cmd, _ -> events.add(cmd) },
                )
            conn.init()
            conn.sendCommand(RequestCommand.ReadFirmwareVersion, ControllerConstants.DEFAULT_BODY)
            delay(150)
            assertTrue(events.any { it == ResponseCommand.ControllerACK })
        }

    @Test
    fun mockPort_sendServiceWater_emitsBeginAndSuccess() =
        runBlocking {
            val protocol = ControllerProtocol()
            val transport = MockControllerSerialTransport()
            val events = mutableListOf<ResponseCommand>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val conn =
                ControllerConnection(
                    devicePath = "MOCK_x",
                    baudRate = 9600,
                    protocol = protocol,
                    transport = transport,
                    connectionScope = scope,
                    onCommandLog = {},
                    onNotConnected = {},
                    onPortNotFound = {},
                    emitResponse = { cmd, _ -> events.add(cmd) },
                )
            conn.init()
            val body = byteArrayOf(0x0a, 0, 0, 0, 0x02)
            conn.sendCommand(RequestCommand.ServiceCommand, body)
 // MOCK_BEGIN_DELAY_MS = 1000; SUCCESS ещё через volumeMl/20 с (20 мл → 1 с)
            delay(1100)
            assertTrue(events.any { it == ResponseCommand.DrinkPreparingBegin })
            delay(1200)
            assertTrue(events.any { it == ResponseCommand.DrinkPreparingSuccess })
        }
}
