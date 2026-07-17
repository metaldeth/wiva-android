package com.wiva.android.data.remote.telemetry.mvp

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnrollConflictCodeParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parseCode reads flat code field`() {
        assertEquals(
            "SERIAL_ALREADY_BOUND",
            EnrollConflictCodeParser.parseCode(
                json,
                """{"code":"SERIAL_ALREADY_BOUND","message":"already bound"}""",
            ),
        )
    }

    @Test
    fun `parseCode reads nest message code`() {
        assertEquals(
            "SERIAL_ALREADY_BOUND",
            EnrollConflictCodeParser.parseCode(
                json,
                """
                {
                  "statusCode": 409,
                  "message": {
                    "code": "SERIAL_ALREADY_BOUND",
                    "message": "Serial is already bound to another installation"
                  },
                  "error": "Conflict"
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `parseCode returns installation already enrolled without serial bound fallback`() {
        assertEquals(
            "INSTALLATION_ALREADY_ENROLLED",
            EnrollConflictCodeParser.parseCode(
                json,
                """
                {
                  "statusCode": 409,
                  "message": {
                    "code": "INSTALLATION_ALREADY_ENROLLED",
                    "message": "Installation already enrolled with different serial"
                  },
                  "error": "Conflict"
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `parseCode returns null for unknown conflict`() {
        assertNull(
            EnrollConflictCodeParser.parseCode(
                json,
                """{"statusCode":409,"message":"something else"}""",
            ),
        )
    }
}
