package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MachineRegistrationSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serialized registration does not contain machineSecret field`() {
        val reg =
            com.wiva.android.domain.model.MachineRegistration(
                serialNumber = "WIVA-000004",
                authScheme = com.wiva.android.domain.model.MachineRegistration.AUTH_SCHEME_STABLE_SECRET,
                enrolled = true,
                isRegistered = true,
            )
        val encoded = json.encodeToString(com.wiva.android.domain.model.MachineRegistration.serializer(), reg)
        assertFalse(encoded.contains("machineSecret"))
        assertFalse(encoded.contains("mch_"))
    }

    @Test
    fun `saveMachineRegistration shape clears credential keys`() {
        val reg =
            com.wiva.android.domain.model.MachineRegistration(
                serialNumber = "WIVA-000004",
                machineCredential = "mch_should_not_persist",
                machineKey = "mch_should_not_persist",
                authScheme = com.wiva.android.domain.model.MachineRegistration.AUTH_SCHEME_STABLE_SECRET,
            )
        val sanitized = reg.copy(machineCredential = "", machineKey = "")
        val encoded = json.encodeToString(com.wiva.android.domain.model.MachineRegistration.serializer(), sanitized)
        assertFalse(encoded.contains("mch_should_not_persist"))
        assertTrue(encoded.contains("stable_secret"))
    }
}
