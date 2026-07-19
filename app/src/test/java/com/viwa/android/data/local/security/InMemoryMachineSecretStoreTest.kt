package com.viwa.android.data.local.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryMachineSecretStoreTest {
    private lateinit var store: InMemoryMachineSecretStore

    @Before
    fun setUp() {
        store = InMemoryMachineSecretStore()
    }

    @Test
    fun `save and get roundtrip`() = runTest {
        store.saveSecret("VIWA-000004", "sec_test_value")
        assertEquals("sec_test_value", store.getSecret("VIWA-000004"))
        assertTrue(store.hasSecret("viwa-000004"))
    }

    @Test
    fun `clear removes secret`() = runTest {
        store.saveSecret("VIWA-000004", "sec_test_value")
        store.clearSecret("VIWA-000004")
        assertNull(store.getSecret("VIWA-000004"))
        assertFalse(store.hasSecret("VIWA-000004"))
    }
}
