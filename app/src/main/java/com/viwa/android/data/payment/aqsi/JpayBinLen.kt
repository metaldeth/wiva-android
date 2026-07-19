package com.viwa.android.data.payment.aqsi

/**
 * Формат BinLen из KB (JPAY): [SOH=0x01][LEN u16 LE][DATA].
 */
internal object JpayBinLen {

    private const val SOH: Byte = 0x01

    fun wrap(data: ByteArray): ByteArray {
        val len = data.size
        require(len <= 0xFFFF) { "data too large" }
        return ByteArray(3 + len).also { out ->
            out[0] = SOH
            out[1] = (len and 0xFF).toByte()
            out[2] = ((len ushr 8) and 0xFF).toByte()
            if (len > 0) {
                System.arraycopy(data, 0, out, 3, len)
            }
        }
    }

    fun unwrap(message: ByteArray): Result<ByteArray> {
        if (message.size < 3) {
            return Result.failure(Arcus2ProtocolException("binlen too short"))
        }
        if (message[0] != SOH) {
            return Result.failure(Arcus2ProtocolException("bad SOH"))
        }
        val len = (message[1].toInt() and 0xFF) or ((message[2].toInt() and 0xFF) shl 8)
        val expectedTotal = 3 + len
        if (message.size < expectedTotal) {
            return Result.failure(Arcus2ProtocolException("truncated binlen"))
        }
        if (message.size != expectedTotal) {
            return Result.failure(Arcus2ProtocolException("binlen trailing bytes"))
        }
        if (len == 0) return Result.success(ByteArray(0))
        return Result.success(message.copyOfRange(3, expectedTotal))
    }
}
