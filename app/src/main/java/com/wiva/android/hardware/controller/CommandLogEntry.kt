package com.wiva.android.hardware.controller

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/***/
data class CommandLogEntry(
    val direction: CommandLogDirection,
    val timestampIso: String,
    val commandHex: String,
    val commandName: String,
    val payload: List<Int>,
) {
    enum class CommandLogDirection {
        TX,
        RX,
    }

    companion object {
        private val isoUtc: ThreadLocal<SimpleDateFormat> =
            ThreadLocal.withInitial {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }

        fun nowIso(): String = isoUtc.get().format(Date())

        fun tx(
            command: RequestCommand,
            body: ByteArray,
        ): CommandLogEntry =
            CommandLogEntry(
                direction = CommandLogDirection.TX,
                timestampIso = nowIso(),
                commandHex = formatCmdHex(command.code),
                commandName = requestCommandName(command),
                payload = body.map { it.toInt() and 0xff },
            )

        fun rx(
            command: ResponseCommand,
            payload: ByteArray,
        ): CommandLogEntry =
            CommandLogEntry(
                direction = CommandLogDirection.RX,
                timestampIso = nowIso(),
                commandHex = formatCmdHex(command.code),
                commandName = responseCommandName(command),
                payload = payload.map { it.toInt() and 0xff },
            )

        private fun formatCmdHex(code: Int): String = "0x${code.toString(16).padStart(2, '0')}"

        private fun requestCommandName(command: RequestCommand): String = command.name

        private fun responseCommandName(command: ResponseCommand): String = command.name
    }
}
