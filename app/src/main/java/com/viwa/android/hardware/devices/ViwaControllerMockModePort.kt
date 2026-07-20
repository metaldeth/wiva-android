package com.viwa.android.hardware.devices

import com.viwa.android.data.local.db.JsonStoreKeys
import com.viwa.android.data.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViwaControllerMockModePort
@Inject
constructor(
    private val configRepository: ConfigRepository,
) {
    suspend fun isMockEnabled(): Boolean =
        configRepository.get(JsonStoreKeys.USE_MOCK_CONTROLLER)?.toBooleanStrictOrNull() == true

    suspend fun setMockEnabled(enabled: Boolean) {
        configRepository.set(JsonStoreKeys.USE_MOCK_CONTROLLER, enabled.toString())
    }
}
