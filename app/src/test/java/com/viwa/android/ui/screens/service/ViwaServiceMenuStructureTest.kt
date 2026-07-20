package com.viwa.android.ui.screens.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ViwaServiceMenuStructureTest {
    @Test
    fun serviceMenuGroups_keepCanonicalOrder() {
        val groupIds = ViwaServiceMenuGroups.map { it.id }
        assertEquals(
            listOf(
                ViwaServiceGroupId.Dashboard,
                ViwaServiceGroupId.Telemetry,
                ViwaServiceGroupId.Debug,
                ViwaServiceGroupId.Integrations,
                ViwaServiceGroupId.CardPayment,
                ViwaServiceGroupId.Equipment,
                ViwaServiceGroupId.Maintenance,
                ViwaServiceGroupId.Settings,
                ViwaServiceGroupId.Updater,
                ViwaServiceGroupId.Performance,
                ViwaServiceGroupId.Metrics,
            ),
            groupIds,
        )
    }

    @Test
    fun telemetrySubTabs_keepConnectionFirst() {
        val telemetry = findViwaServiceGroup(ViwaServiceGroupId.Telemetry)
        assertEquals(
            listOf(
                ViwaServiceSubTabId.TelemetryConnection,
                ViwaServiceSubTabId.TelemetryAddresses,
                ViwaServiceSubTabId.TelemetryInventory,
                ViwaServiceSubTabId.TelemetryTests,
            ),
            telemetry.subTabs.map { it.id },
        )
    }

    @Test
    fun equipmentSubTabs_matchHybridDevicesLayout() {
        val equipment = findViwaServiceGroup(ViwaServiceGroupId.Equipment)
        assertEquals("Устройства", equipment.label)
        assertEquals(
            listOf(
                ViwaServiceSubTabId.Devices,
                ViwaServiceSubTabId.Ports,
                ViwaServiceSubTabId.Controller,
                ViwaServiceSubTabId.Payment,
                ViwaServiceSubTabId.Scanner,
                ViwaServiceSubTabId.Rfid,
            ),
            equipment.subTabs.map { it.id },
        )
    }
}
