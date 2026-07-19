package com.viwa.android.hardware

import java.io.File
import java.util.Comparator

/**
 * Имена узлов в `/dev`, которые считаем последовательными портами (SoC UART, USB-serial, WCH и т.д.).
 */
internal val SERIAL_TTY_NODE_REGEX =
    Regex(
        "^tty(?:" +
            "S\\d+" +
            "|USB\\d+" +
            "|ACM\\d+" +
            "|AMA\\d+" +
            "|WCH\\d+" +
            "|GS\\d+" +
            "|FIQ\\d+" +
            "|O\\d+" +
            "|HS\\d+" +
            "|XRUSB\\d+" +
            "|PCH\\d+" +
            "|SAC\\d+" +
            "|mxc\\d+" +
            "|MSM\\d+" +
            "|MTT\\d+" +
            ")$",
    )

/**
 * Сканирует `/dev` и возвращает пути к serial-узлам (в т.ч. высокие `ttyS*` на RK3568/WCH).
 */
object NativeSerialPortDetector {
    private val pathComparator =
        Comparator<String> { a, b ->
            val na = a.substringAfterLast('/')
            val nb = b.substringAfterLast('/')
            val prefixCmp = letterPrefix(na).compareTo(letterPrefix(nb))
            if (prefixCmp != 0) return@Comparator prefixCmp
            numericSuffix(na).compareTo(numericSuffix(nb))
        }

    private fun letterPrefix(ttyName: String): String = ttyName.takeWhile { !it.isDigit() }

    private fun numericSuffix(ttyName: String): Int {
        val digits = ttyName.dropWhile { !it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    fun detectPortPaths(): List<String> {
        val dev = File("/dev")
        if (!dev.isDirectory) return emptyList()

        val names =
            try {
                dev.list()?.asSequence()?.toList().orEmpty()
            } catch (_: SecurityException) {
                emptyList()
            }

        return names
            .asSequence()
            .filter { SERIAL_TTY_NODE_REGEX.matches(it) }
            .map { File(dev, it).absolutePath }
            .filter { path ->
                try {
                    File(path).exists()
                } catch (_: SecurityException) {
                    false
                }
            }
            .distinct()
            .sortedWith(pathComparator)
            .toList()
    }
}
