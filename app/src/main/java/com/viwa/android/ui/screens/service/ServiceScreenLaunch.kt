package com.viwa.android.ui.screens.service

/**
 * Одноразовые переходы при старте сервисного меню из intent
 * ([com.viwa.android.ui.MainActivity] — extras `open_service_dashboard` / `open_service_equipment`).
 */
object ServiceScreenLaunch {
    @Volatile
    var selectDashboardOnOpen: Boolean = false

    @Volatile
    var selectEquipmentDevicesOnOpen: Boolean = false
}
