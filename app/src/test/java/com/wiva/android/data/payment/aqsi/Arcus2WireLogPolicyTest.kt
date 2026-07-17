package com.wiva.android.data.payment.aqsi

import com.wiva.android.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber

/** Task-07 A7: политика байтового лога Arcus2 (только debug, без полного payload). */
class Arcus2WireLogPolicyTest {

    private class RecordingTree : Timber.Tree() {
        val entries = mutableListOf<Pair<String, String>>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (tag != null) entries += tag to message
        }
    }

    @Test
    fun logArcus2WireFrameHead_whenDebugDisabled_emitsNothing() {
        val tree = RecordingTree()
        Timber.plant(tree)
        try {
            val frame = ByteArray(64) { it.toByte() }
            logArcus2WireFrameHead("tx", frame, tag = "Arcus2", debugEnabled = false)
            assertTrue(tree.entries.isEmpty())
        } finally {
            Timber.uproot(tree)
        }
    }

    @Test
    fun logArcus2WireFrameHead_whenDebugEnabled_logsTagLengthAndHeadOnly() {
        val tree = RecordingTree()
        Timber.plant(tree)
        try {
            val frame = ByteArray(100) { 0xAB.toByte() }
            logArcus2WireFrameHead("rx", frame, tag = "Arcus2", debugEnabled = true)
            assertEquals(1, tree.entries.size)
            val (tag, msg) = tree.entries.first()
            assertEquals("Arcus2", tag)
            assertTrue(msg.contains("rx arcus2 len=100"))
            val hexTokens = msg.substringAfter("head=").trim().split(' ')
            assertEquals(24, hexTokens.size)
        } finally {
            Timber.uproot(tree)
        }
    }

    @Test
    fun arcus2WireBytesLoggingEnabled_matchesBuildConfigDebug() {
        assertEquals(BuildConfig.DEBUG, arcus2WireBytesLoggingEnabled())
    }
}
