package com.wiva.android.data.remote.sbp

import com.wiva.android.domain.model.SBPStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PaymasterXmlParserTest {
    @Test
    fun parseQPayGenerateQr_success() {
        val xml =
            """
            <w3s.response>
              <retval>0</retval>
              <retdesc>success</retdesc>
              <generate_qr_code>
                <qr>https://qr.nspk.ru/some-link?data=abc123</qr>
              </generate_qr_code>
            </w3s.response>
            """.trimIndent()

        val result = PaymasterXmlParser.parseQPayGenerateQr(xml)
        assertEquals(0, result.retval)
        assertEquals("success", result.retdesc)
        assertEquals("https://qr.nspk.ru/some-link?data=abc123", result.qr)
    }

    @Test
    fun parseQPayGenerateQr_error() {
        val xml =
            """
            <w3s.response>
              <retval>1</retval>
              <retdesc>Invalid request format</retdesc>
            </w3s.response>
            """.trimIndent()

        val result = PaymasterXmlParser.parseQPayGenerateQr(xml)
        assertEquals(1, result.retval)
        assertEquals("Invalid request format", result.retdesc)
        assertNull(result.qr)
    }

    @Test
    fun parseQPayGenerateQr_empty_returns_minus_one() {
        val result = PaymasterXmlParser.parseQPayGenerateQr("")
        assertEquals(-1, result.retval)
        assertNull(result.qr)
    }

    @Test
    fun parseQPayGenerateQr_trimsQr() {
        val xml =
            "<w3s.response><retval>0</retval><generate_qr_code><qr>  https://example.com  </qr></generate_qr_code></w3s.response>"
        val result = PaymasterXmlParser.parseQPayGenerateQr(xml)
        assertEquals("https://example.com", result.qr)
    }

    @Test
    fun parseQPayGenerateQr_invalidInputsDoNotThrow() {
        listOf(
            "<<<not-xml",
            "<unclosed><retval>1",
            "plain text response",
            "<w3s.response><retval>not-a-number</retval></w3s.response>",
        ).forEach { xml ->
            try {
                PaymasterXmlParser.parseQPayGenerateQr(xml)
            } catch (t: Throwable) {
                fail("threw for input <<$xml>>: $t")
            }
        }
    }

    @Test
    fun parseQPayOutInvoiceState() {
        assertEquals(
            4,
            PaymasterXmlParser.parseQPayOutInvoiceState(
                "<w3s.response><outinvoice><state>4</state></outinvoice></w3s.response>",
            ),
        )
        assertEquals(5, PaymasterXmlParser.parseQPayOutInvoiceState("<outinvoice><state>5</state></outinvoice>"))
        assertEquals(6, PaymasterXmlParser.parseQPayOutInvoiceState("<outinvoice><state>6</state></outinvoice>"))
        assertEquals(0, PaymasterXmlParser.parseQPayOutInvoiceState("<w3s.response></w3s.response>"))
    }

    @Test
    fun parseQPayStatusToSbpStatus() {
        assertEquals(SBPStatus.Success, PaymasterXmlParser.parseQPayStatusToSbpStatus(5))
        val denied = PaymasterXmlParser.parseQPayStatusToSbpStatus(6)
        assertTrue(denied is SBPStatus.Failed)
        assertEquals("DENIED", (denied as SBPStatus.Failed).reason)
        assertEquals(SBPStatus.Pending, PaymasterXmlParser.parseQPayStatusToSbpStatus(4))
        assertEquals(SBPStatus.Pending, PaymasterXmlParser.parseQPayStatusToSbpStatus(0))
    }

    @Test
    fun parseQPayCancelRetval() {
        assertEquals(
            0,
            PaymasterXmlParser.parseQPayCancelRetval("<w3s.response><retval>0</retval><retdesc>ok</retdesc></w3s.response>"),
        )
        assertEquals(5, PaymasterXmlParser.parseQPayCancelRetval("<w3s.response><retval>5</retval></w3s.response>"))
        assertEquals(-1, PaymasterXmlParser.parseQPayCancelRetval(""))
    }
}
