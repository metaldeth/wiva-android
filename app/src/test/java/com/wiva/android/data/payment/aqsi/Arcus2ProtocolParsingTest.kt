package com.wiva.android.data.payment.aqsi

import com.wiva.android.domain.model.AqsiPaymentResult
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фикстуры разборщика Arcus2 + JPAY без PAN/треков (task-03, KB §STORERC таблица 5). */
class Arcus2ProtocolParsingTest {

    private val win1251: Charset = Charset.forName("windows-1251")

    @Test
    fun buildPaymentOperationStart_matchesKbSeparatorAndCurrency() {
        val s = buildPaymentOperationStart(184)
        assertEquals("1\u001b1\u001b643\u001b1.84", s)
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildPaymentOperationStart_rejectsNegativeKopecks() {
        buildPaymentOperationStart(-1)
    }

    @Test
    fun crc8_matchesKbExample_forAsciiDigits123456789() {
        val data = "123456789".toByteArray(Charsets.US_ASCII)
        val c = Arcus2FrameCodec.crc8(data).toInt() and 0xFF
        assertEquals(0xF4, c)
    }

    @Test
    fun encodeDecode_roundTrip_emptyPayload() {
        val inner = JpayBinLen.wrap(ByteArray(0))
        val frame = Arcus2FrameCodec.encode(inner, 0)
        val decoded = Arcus2FrameCodec.decode(frame).getOrThrow()
        assertEquals(0.toByte(), decoded.pcb)
        val unwrapped = JpayBinLen.unwrap(decoded.payload).getOrThrow()
        assertEquals(0, unwrapped.size)
    }

    @Test
    fun parser_storercZeroVariants_areApproved() {
        listOf("STORERC:00", "STORERC:000", "STORERC:0").forEach { line ->
            assertApprovedFixture(line)
        }
    }

    @Test
    fun parser_storerc051_isDeclinedWithSafeCode() {
        assertDeclinedFixture("STORERC:051", "051")
    }

    @Test
    fun parser_ok_and_er_paths() {
        val ok = JpayPaymentOutcomeParser.interpretPaymentLine("OK")
        assertEquals(JpayInterpretation.IntermediateOk, ok)
        assertEquals(null, JpayPaymentOutcomeParser.toPaymentResult(ok))

        val er = JpayPaymentOutcomeParser.interpretPaymentLine("ER")
        assertEquals(JpayInterpretation.OperationEr, er)
        val mapped = JpayPaymentOutcomeParser.toPaymentResult(er)!!.getOrThrow()
        assertTrue(mapped is AqsiPaymentResult.Declined)
        assertTrue(mapped !is AqsiPaymentResult.Error)
        assertEquals(JpayPaymentOutcomeParser.DECLINED_PUBLIC_CODE_ER, (mapped as AqsiPaymentResult.Declined).publicCode)
    }

    @Test
    fun binLen_unwrap_rejectsTrailingBytes() {
        val inner = JpayBinLen.wrap(byteArrayOf(0xAA.toByte()))
        assertEquals(4, inner.size)
        val withGarbage = inner + byteArrayOf(0xEE.toByte(), 0xFF.toByte())
        assertTrue(JpayBinLen.unwrap(withGarbage).isFailure)
    }

    @Test
    fun decode_invalidStx_isFailureWithoutCrashing() {
        val garbage = byteArrayOf(0x12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val result = Arcus2FrameCodec.decode(garbage)
        assertTrue(result.isFailure)
    }

    @Test
    fun decode_truncatedFrame_isFailure() {
        val result = Arcus2FrameCodec.decode(byteArrayOf(Arcus2FrameCodec.STX, 0, 0, 0))
        assertTrue(result.isFailure)
    }

    @Test
    fun decode_wrongCrc_isFailure() {
        val inner = JpayBinLen.wrap("STORERC:00".toByteArray(Charsets.US_ASCII))
        val good = Arcus2FrameCodec.encode(inner, 0).clone()
        good[good.lastIndex] = (good[good.lastIndex] + 1).toByte()
        assertTrue(Arcus2FrameCodec.decode(good).isFailure)
    }

    private fun assertApprovedFixture(innerAscii: String) {
        val frame =
            Arcus2FrameCodec.encode(
                JpayBinLen.wrap(innerAscii.toByteArray(Charsets.US_ASCII)),
                0,
            )
        val payload =
            JpayBinLen.unwrap(Arcus2FrameCodec.decode(frame).getOrThrow().payload).getOrThrow()
        val line = String(payload, win1251).trim()
        val interp = JpayPaymentOutcomeParser.interpretPaymentLine(line)
        val pay =
            requireNotNull(JpayPaymentOutcomeParser.toPaymentResult(interp)) {
                "expected terminal outcome"
            }.getOrThrow()
        assertEquals(AqsiPaymentResult.Approved, pay)
    }

    private fun assertDeclinedFixture(innerAscii: String, expectedCode: String) {
        val frame =
            Arcus2FrameCodec.encode(
                JpayBinLen.wrap(innerAscii.toByteArray(Charsets.US_ASCII)),
                0,
            )
        val payload =
            JpayBinLen.unwrap(Arcus2FrameCodec.decode(frame).getOrThrow().payload).getOrThrow()
        val line = String(payload, win1251).trim()
        val interp = JpayPaymentOutcomeParser.interpretPaymentLine(line)
        val pay =
            requireNotNull(JpayPaymentOutcomeParser.toPaymentResult(interp)).getOrThrow()
            as AqsiPaymentResult.Declined
        assertEquals(expectedCode, pay.publicCode)
    }
}
