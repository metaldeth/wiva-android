package com.viwa.android.domain.repository

import com.viwa.android.domain.model.TelemetryCellsSnapshot
import kotlinx.coroutines.flow.StateFlow

/** Read/write JsonStore snapshot ячеек MVP telemetry (atomic replace). */
interface TelemetryCellsRepository {
    val snapshotFlow: StateFlow<TelemetryCellsSnapshot?>

    suspend fun getSnapshot(): TelemetryCellsSnapshot?

    /** Полная замена snapshot (cells + products + revisions). */
    suspend fun replaceSnapshot(snapshot: TelemetryCellsSnapshot)

    suspend fun clearSnapshot()
}
