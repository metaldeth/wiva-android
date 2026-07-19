package com.wiva.android.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CellVolumeStatusTest {

    @Test
    fun volumeAtOrBelowBlock_isStop() {
        assertEquals(CellVolumeStatus.STOP, resolveCellVolumeStatus(volume = 0, blockVolume = 0, sosVolume = 100))
        assertEquals(CellVolumeStatus.STOP, resolveCellVolumeStatus(volume = 50, blockVolume = 50, sosVolume = 200))
    }

    @Test
    fun volumeAtOrBelowSos_isWarning() {
        assertEquals(CellVolumeStatus.WARNING, resolveCellVolumeStatus(volume = 100, blockVolume = 0, sosVolume = 100))
        assertEquals(CellVolumeStatus.WARNING, resolveCellVolumeStatus(volume = 150, blockVolume = 50, sosVolume = 200))
    }

    @Test
    fun volumeAboveSos_isNormal() {
        assertEquals(CellVolumeStatus.NORMAL, resolveCellVolumeStatus(volume = 500, blockVolume = 0, sosVolume = 100))
        assertEquals(CellVolumeStatus.NORMAL, resolveCellVolumeStatus(volume = 201, blockVolume = 50, sosVolume = 200))
    }
}
