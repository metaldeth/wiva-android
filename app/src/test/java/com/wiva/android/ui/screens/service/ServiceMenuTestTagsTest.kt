package com.wiva.android.ui.screens.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceMenuTestTagsTest {

    @Test
    fun allDeclaredTags_areUnique() {
        val tags = ServiceMenuTestTags.allDeclaredTags.toList()
        assertEquals(tags.size, tags.distinct().size)
    }

    @Test
    fun allDeclaredTags_matchNamingConvention() {
        val pattern = Regex("^[a-z][a-z0-9_]*$")
        for (tag in ServiceMenuTestTags.allDeclaredTags) {
            assertTrue("Invalid tag: $tag", pattern.matches(tag))
        }
    }

    @Test
    fun telemetrySmokeTags_areDocumentedConstants() {
        assertEquals("telemetry_connection_root", ServiceMenuTestTags.TELEMETRY_CONNECTION_ROOT)
        assertEquals("telemetry_serial_input", ServiceMenuTestTags.TELEMETRY_SERIAL_INPUT)
        assertEquals("telemetry_reserve_serial", ServiceMenuTestTags.TELEMETRY_RESERVE_SERIAL)
        assertEquals("telemetry_register", ServiceMenuTestTags.TELEMETRY_REGISTER)
        assertEquals("telemetry_connect_ws", ServiceMenuTestTags.TELEMETRY_CONNECT_WS)
        assertEquals("telemetry_api_url_input", ServiceMenuTestTags.TELEMETRY_API_URL_INPUT)
        assertEquals("telemetry_ws_url_input", ServiceMenuTestTags.TELEMETRY_WS_URL_INPUT)
    }

    @Test
    fun serviceNavigationTags_resolveForTelemetryConnectionFlow() {
        assertEquals("service_group_telemetry", ServiceMenuTestTags.serviceGroupTag(WivaServiceGroupId.Telemetry))
        assertEquals(
            "service_subtab_telemetry_connection",
            ServiceMenuTestTags.serviceSubTabTag(WivaServiceSubTabId.TelemetryConnection),
        )
        assertEquals(
            "service_subtab_telemetry_addresses",
            ServiceMenuTestTags.serviceSubTabTag(WivaServiceSubTabId.TelemetryAddresses),
        )
    }
}
