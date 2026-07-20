package com.viwa.android.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MachineRegistrationEnrollTest {
    @Test
    fun `migrateLegacy heals legacy credential without enrolled flags`() {
        val raw =
            MachineRegistration(
                serialNumber = "E-01",
                machineCredential = "mch_0f6d1cf879d0e04f896e04f8c09904b02cec5eff0f0f073dbe18ae1597822b4b",
                installationId = "d1be53f1-08a5-4847-bdee-88bc1cdf9d52",
                authScheme = MachineRegistration.AUTH_SCHEME_LEGACY_CREDENTIAL,
                isRegistered = false,
                enrolled = false,
            )
        val migrated = MachineRegistration.migrateLegacy(raw)
        assertTrue(migrated.isRegistered)
        assertTrue(migrated.enrolled)
        assertTrue(MachineRegistration.isEnrolled(migrated))
        assertTrue(MachineRegistration.isEnrolled(raw))
    }

    @Test
    fun `isEnrolled false without credential or flags`() {
        val empty = MachineRegistration(serialNumber = "E-01")
        assertFalse(MachineRegistration.isEnrolled(empty))
        assertFalse(MachineRegistration.isEnrolled(MachineRegistration.migrateLegacy(empty)))
    }

    @Test
    fun `isEnrolled true for stable_secret when registered`() {
        val reg =
            MachineRegistration(
                serialNumber = "VIWA-000004",
                authScheme = MachineRegistration.AUTH_SCHEME_STABLE_SECRET,
                isRegistered = true,
                enrolled = false,
            )
        assertTrue(MachineRegistration.isEnrolled(reg))
    }
}
