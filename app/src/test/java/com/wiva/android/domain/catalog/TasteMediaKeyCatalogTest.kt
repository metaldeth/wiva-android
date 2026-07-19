package com.wiva.android.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteMediaKeyCatalogTest {

    @Test
    fun allKeys_shouldContainExactly14AllowlistedKeys() {
        assertEquals(14, TasteMediaKeyCatalog.ALL_KEYS.size)
        assertEquals(TasteMediaKeyCatalog.ALL_KEYS.toSet().size, 14)
    }

    @Test
    fun eachKey_shouldMapToKnownAssetAndHaveRuName() {
        for (key in TasteMediaKeyCatalog.ALL_KEYS) {
            assertTrue("missing asset for $key", TasteMediaKeyCatalog.hasAssetMapping(key))
            assertTrue("missing RU name for $key", !TasteMediaKeyCatalog.nameRu(key).isNullOrBlank())
            assertTrue(TasteMediaKeyCatalog.isValid(key))
        }
    }

    @Test
    fun isValid_shouldRejectUnknownKey() {
        assertTrue(!TasteMediaKeyCatalog.isValid("unknown-taste"))
    }
}
