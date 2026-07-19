package com.viwa.android.hardware.controller

import javax.inject.Inject
import timber.log.Timber

/**
 * Форматирование TX и разбор RX как в [.ControllerProtocol].
 *
 * TX: `[0xfe, body.length + 1, command,.body]`
 * RX: ищем `0xd5`, длина = `buffer[1]`, полная длина кадра = `length + 2`, команда `[2]`, тело `[3.]`.
 */
class ControllerProtocol
@Inject
constructor() {
    data class ParsedRx(
        val command: ResponseCommand,
        val payload: ByteArray,
    )

    fun formatRequest(command: RequestCommand, body: ByteArray): ByteArray {
        val len = body.size + 1
        return byteArrayOf(0xfe.toByte(), len.toByte(), command.code.toByte()) + body
    }

 /**
 * @return список полных сообщений и остаток буфера (как `processBuffer` в TS).
 */
    fun processBuffer(buffer: ByteArray): Pair<List<ParsedRx>, ByteArray> {
        val messages = mutableListOf<ParsedRx>()
        var remaining = buffer

        while (true) {
            val startIndex = remaining.indexOfFirst { it == START_RX }
            if (startIndex == -1) break
            if (startIndex > 0) {
                remaining = remaining.copyOfRange(startIndex, remaining.size)
            }
            if (remaining.size < 2) break

            val length = remaining[1].toInt() and 0xff
            val messageTotalLength = length + 2
            if (messageTotalLength > remaining.size) break

            val message = remaining.copyOfRange(0, messageTotalLength)
            val cmdByte = message[2].toInt() and 0xff
            val payload = message.copyOfRange(3, message.size)
            val cmd = ResponseCommand.fromCode(cmdByte)
            if (cmd != null) {
                messages.add(ParsedRx(cmd, payload))
            } else {
                Timber.tag(TAG).w("RX: неизвестная команда 0x%02x, кадр отброшен", cmdByte)
            }
            remaining = remaining.copyOfRange(messageTotalLength, remaining.size)
        }

        return messages to remaining
    }

    fun parseResponse(data: ByteArray): ParsedRx? {
        val (list, _) = processBuffer(data)
        return list.firstOrNull()
    }

    companion object {
        private const val TAG = "ViwaController"
        private val START_RX = 0xd5.toByte()
    }
}
