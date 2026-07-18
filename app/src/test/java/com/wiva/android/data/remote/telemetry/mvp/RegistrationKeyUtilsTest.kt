package com.wiva.android.data.remote.telemetry.mvp



import org.junit.Assert.assertEquals

import org.junit.Assert.assertFalse

import org.junit.Assert.assertNull

import org.junit.Assert.assertTrue

import org.junit.Test



class RegistrationKeyUtilsTest {

    companion object {

        const val VALID_KEY = "REG-0123456789AB"

        const val VALID_BODY = "0123456789AB"

    }



    @Test

    fun `normalize adds REG prefix to body`() {

        assertEquals(VALID_KEY, RegistrationKeyUtils.normalize(VALID_BODY))

    }



    @Test

    fun `normalize uppercases and trims`() {

        assertEquals(VALID_KEY, RegistrationKeyUtils.normalize("  reg-0123456789ab  "))

    }



    @Test

    fun `isValid accepts server Crockford example with digits 0 1 8 9`() {

        assertTrue(RegistrationKeyUtils.isValid("REG-0123456789AB"))

        assertTrue(RegistrationKeyUtils.isValid("REG-890123456789"))

    }



    @Test

    fun `isValid rejects ambiguous Crockford symbols I L O U`() {

        assertFalse(RegistrationKeyUtils.isValid("REG-0123456789IL"))

        assertFalse(RegistrationKeyUtils.isValid("REG-IL0123456789"))

        assertFalse(RegistrationKeyUtils.isValid("REG-012345678OU"))

    }



    @Test

    fun `isValid rejects keys containing disallowed symbols`() {

        assertFalse(RegistrationKeyUtils.isValid("REG-ABCDEFGHI234"))

    }



    @Test

    fun `validationMessage rejects short key`() {

        assertEquals(

            "Формат: REG- и 12 символов Crockford (0-9, A-H, J-N, P-T, V-Z; без I, L, O, U)",

            RegistrationKeyUtils.validationMessage("REG-SHORT"),

        )

    }



    @Test

    fun `validationMessage accepts valid key`() {

        assertNull(RegistrationKeyUtils.validationMessage(VALID_KEY))

    }

}

