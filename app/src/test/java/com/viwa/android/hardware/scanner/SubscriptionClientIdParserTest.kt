package com.viwa.android.hardware.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionClientIdParserTest {
    @Test
    fun fromRaw_clientPrefix_returnsUuid() {
        val uuid = "2caaf0b2-2b7f-4c09-9bef-dafd984c9a66"
        assertEquals(uuid, SubscriptionClientIdParser.fromScannerRawLine("CLIENT_$uuid"))
    }

    @Test
    fun fromRaw_plainUuid_returnsNull() {
        val uuid = "2caaf0b2-2b7f-4c09-9bef-dafd984c9a66"
        assertNull(SubscriptionClientIdParser.fromScannerRawLine(uuid))
    }

    @Test
    fun fromRaw_lowercaseClientPrefix_returnsNull() {
        val uuid = "2caaf0b2-2b7f-4c09-9bef-dafd984c9a66"
        assertNull(SubscriptionClientIdParser.fromScannerRawLine("client_$uuid"))
    }

    @Test
    fun fromRaw_garbage_returnsNull() {
        assertNull(SubscriptionClientIdParser.fromScannerRawLine("not-a-uuid"))
    }
}
