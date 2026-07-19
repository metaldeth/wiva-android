package com.viwa.android.ui.screens.service

/**
 * Одноразовый переход на вкладку «Дашборд» при старте сервисного меню из intent
 * ([com.viwa.android.ui.MainActivity] — extra `open_service_dashboard`).
 */
object ServiceScreenLaunch {
    @Volatile
    var selectDashboardOnOpen: Boolean = false
}
