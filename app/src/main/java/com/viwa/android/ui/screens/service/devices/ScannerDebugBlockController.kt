package com.viwa.android.ui.screens.service.devices

import com.viwa.android.hardware.scanner.ViwaScannerTrafficLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerDebugBlockState(
    val entries: List<ScannerDebugRowUi> = emptyList(),
)

data class ScannerDebugRowUi(
    val stableKey: Long,
    val timeLabel: String,
    val raw: String,
    val classification: String,
    val outcome: String?,
    val isSystem: Boolean,
)

class ScannerDebugBlockController(
    private val scannerTrafficLogger: ViwaScannerTrafficLogger,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ScannerDebugBlockState())
    val state: StateFlow<ScannerDebugBlockState> = _state.asStateFlow()

    init {
        scope.launch {
            scannerTrafficLogger.entries.collect { entries ->
                _state.update {
                    it.copy(
                        entries =
                            entries.map { entry ->
                                ScannerDebugRowUi(
                                    stableKey = entry.id.toLong(),
                                    timeLabel = ScannerDebugTimeFormat.format(entry.timestampMs),
                                    raw = entry.rawLine,
                                    classification = entry.classificationLabel,
                                    outcome = null,
                                    isSystem = entry.rawLine.isEmpty(),
                                )
                            },
                    )
                }
            }
        }
    }

    fun clearLog() {
        scannerTrafficLogger.clear()
    }
}
