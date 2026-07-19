package com.viwa.android.domain.model

/** Индикатор остатка ячейки (TZ §3.1.1, web getVolumeStatus). */
enum class CellVolumeStatus {
    NORMAL,
    WARNING,
    STOP,
}

/** volume <= blockVolume → STOP; <= sosVolume → WARNING; иначе NORMAL. */
fun resolveCellVolumeStatus(
    volume: Int,
    blockVolume: Int,
    sosVolume: Int,
): CellVolumeStatus =
    when {
        volume <= blockVolume -> CellVolumeStatus.STOP
        volume <= sosVolume -> CellVolumeStatus.WARNING
        else -> CellVolumeStatus.NORMAL
    }
