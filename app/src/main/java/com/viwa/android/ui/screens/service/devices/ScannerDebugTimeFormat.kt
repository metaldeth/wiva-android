package com.viwa.android.ui.screens.service.devices

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ScannerDebugTimeFormat {
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun format(timestampMillis: Long): String = format.format(Date(timestampMillis))
}

