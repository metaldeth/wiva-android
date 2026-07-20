package com.viwa.android.hardware.scanner

import com.viwa.android.hardware.serial.PortRole
import com.viwa.android.hardware.serial.ViwaSerialPort
import javax.inject.Inject
import javax.inject.Singleton
import com.viwa.android.di.AppIoScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ViwaScannerStartupInitializer
@Inject
constructor(
    private val serialPort: ViwaSerialPort,
    private val portAutoDiscovery: ViwaScannerPortAutoDiscovery,
    @AppIoScope private val ioScope: CoroutineScope,
) {
    fun start(onAssigned: () -> Unit = {}) {
        ioScope.launch {
            assignIfNeeded()
            onAssigned()
        }
    }

    suspend fun assignIfNeeded(): Boolean {
        if (serialPort.assignedDeviceName(PortRole.SCANNER) != null) {
            return false
        }
        if (!portAutoDiscovery.hasCandidates()) {
            Timber.tag(TAG).d("Scanner startup skipped: no auto-discovery candidates")
            return false
        }
        val path = portAutoDiscovery.discover() ?: return false
        return serialPort.assign(path, PortRole.SCANNER).fold(
            onSuccess = {
                Timber.tag(TAG).i("Scanner auto-assigned to %s", path)
                true
            },
            onFailure = { error ->
                Timber.tag(TAG).w(error, "Scanner auto-assign failed for %s", path)
                false
            },
        )
    }

    companion object {
        private const val TAG = "ScannerPort"
    }
}
