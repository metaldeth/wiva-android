package com.viwa.android.ui.screens.service.devices

import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.SerialPortManager
import com.viwa.android.data.repository.ConfigRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DevicesBlockControllerTest {
    @Test
    fun `PortRole includes controller and payment for device assignment chips`() {
        assertEquals(
            setOf(PortRole.SCANNER, PortRole.PAYMENT, PortRole.CONTROLLER, PortRole.UNKNOWN, PortRole.UNASSIGNED),
            PortRole.entries.toSet(),
        )
    }

    @Test
    fun `SerialPortManager parses legacy PRIMARY_CONTROLLER role`() = runTest {
        val configRepository = mockk<ConfigRepository>()
        coEvery { configRepository.getJson(any()) } returns
            """{"/dev/ttyS4":"PRIMARY_CONTROLLER"}"""
        val manager =
            SerialPortManager(
                usbSerialManager = mockk(relaxed = true),
                configRepository = configRepository,
                context = mockk(relaxed = true),
            )

        val assignments = manager.getPortAssignments()

        assertEquals(PortRole.CONTROLLER, assignments["/dev/ttyS4"])
    }
}
