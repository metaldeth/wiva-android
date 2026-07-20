package com.viwa.android.data.payment.aqsi

/**
 * Низкоуровневый кадр Arcus2 (KB aQsi protocol): STX=0x5A, PCB, DATA_LENGTH LE, DATA, CRC8
 * по всем байтам кроме STX (полином 0x07, init 0 — см. таблицу в документации).
 */
internal object Arcus2FrameCodec {

    const val STX: Byte = 0x5A

 /** CRC8 всего блока кроме STX (PCB + длина LE + DATA). */
    fun crc8(blockSansStx: ByteArray): Byte {
        var crc = 0
        for (element in blockSansStx) {
            crc = crc xor (element.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x80 != 0) {
                    (crc shl 1) xor 0x07
                } else {
                    crc shl 1
                }
                crc = crc and 0xFF
            }
        }
        return crc.toByte()
    }

    fun encode(payload: ByteArray, pcb: Byte): ByteArray {
        val len = payload.size
        require(len <= 0xFFFF) { "payload too large" }
        val sansStx = ByteArray(3 + payload.size)
        sansStx[0] = pcb
        sansStx[1] = (len and 0xFF).toByte()
        sansStx[2] = ((len ushr 8) and 0xFF).toByte()
        if (payload.isNotEmpty()) {
            System.arraycopy(payload, 0, sansStx, 3, payload.size)
        }
        val crc = crc8(sansStx)
        return ByteArray(1 + sansStx.size + 1).also { out ->
            out[0] = STX
            System.arraycopy(sansStx, 0, out, 1, sansStx.size)
            out[out.lastIndex] = crc
        }
    }

    fun decode(frame: ByteArray): Result<Arcus2DecodedFrame> {
        if (frame.size < 1 + 3 + 1) {
            return Result.failure(Arcus2ProtocolException("frame too short"))
        }
        if (frame[0] != STX) {
            return Result.failure(Arcus2ProtocolException("bad STX"))
        }
        val sansStx = frame.copyOfRange(1, frame.size - 1)
        val expectedCrc = frame[frame.lastIndex].toInt() and 0xFF
        val actualCrc = crc8(sansStx).toInt() and 0xFF
        if (expectedCrc != actualCrc) {
            return Result.failure(Arcus2ProtocolException("crc mismatch"))
        }
        val pcb = sansStx[0]
        val dataLen = (sansStx[1].toInt() and 0xFF) or ((sansStx[2].toInt() and 0xFF) shl 8)
        if (sansStx.size != 3 + dataLen) {
            return Result.failure(Arcus2ProtocolException("length mismatch"))
        }
        val data =
            if (dataLen == 0) {
                ByteArray(0)
            } else {
                sansStx.copyOfRange(3, 3 + dataLen)
            }
        return Result.success(Arcus2DecodedFrame(pcb = pcb, payload = data))
    }
}

internal data class Arcus2DecodedFrame(
    val pcb: Byte,
    val payload: ByteArray,
)
