package com.viwa.android.hardware.serial

/** Роли USB/serial портов (совместимо с shaker SerialPortRole). */
enum class PortRole {
    SCANNER,
    PAYMENT,
    CONTROLLER,
    UNKNOWN,
    UNASSIGNED,
}

fun PortRole.toAssignmentStorage(): PortRole =
    when (this) {
        PortRole.UNKNOWN -> PortRole.UNASSIGNED
        else -> this
    }

fun PortRole.fromAssignmentStorage(): PortRole =
    when (this) {
        PortRole.UNASSIGNED -> PortRole.UNKNOWN
        else -> this
    }
