package com.wiva.android.ui.screens.service

/**
 * Stable Compose [androidx.compose.ui.platform.testTag] values for service menu automation.
 *
 * With [androidx.compose.ui.semantics.testTagsAsResourceId] on [ServiceScreen], UiAutomator can
 * target nodes via `By.res("com.wiva.android:id/<tag>")` (package + tag as synthetic resource id).
 */
object ServiceMenuTestTags {
    const val SERVICE_MENU_ROOT = "service_menu_root"
    const val SERVICE_MENU_CLOSE = "service_menu_close"

    const val TELEMETRY_CONNECTION_ROOT = "telemetry_connection_root"
    const val TELEMETRY_CONNECTION_STATUS_CARD = "telemetry_connection_status_card"
    const val TELEMETRY_CONNECTION_STATUS_TEXT = "telemetry_connection_status_text"
    const val TELEMETRY_CONNECTION_RECONNECT = "telemetry_connection_reconnect"
    const val TELEMETRY_SERIAL_INPUT = "telemetry_serial_input"
    const val TELEMETRY_RESERVE_SERIAL = "telemetry_reserve_serial"
    const val TELEMETRY_REGISTER = "telemetry_register"
    const val TELEMETRY_CONNECT_WS = "telemetry_connect_ws"
    const val TELEMETRY_DISCONNECT_WS = "telemetry_disconnect_ws"
    const val TELEMETRY_REBIND_CARD = "telemetry_rebind_card"
    const val TELEMETRY_REBIND_CONFIRM = "telemetry_rebind_confirm"
    const val TELEMETRY_REBIND_CANCEL = "telemetry_rebind_cancel"
    const val TELEMETRY_REG_KEY_INPUT = "telemetry_reg_key_input"
    const val TELEMETRY_QR_SCANNED_BANNER = "telemetry_qr_scanned_banner"
    const val TELEMETRY_BANNER = "telemetry_banner"
    const val TELEMETRY_BUSY = "telemetry_busy"

    const val TELEMETRY_ADDRESSES_ROOT = "telemetry_addresses_root"
    const val TELEMETRY_API_URL_INPUT = "telemetry_api_url_input"
    const val TELEMETRY_WS_URL_INPUT = "telemetry_ws_url_input"
    const val TELEMETRY_SAVE_ADDRESSES = "telemetry_save_addresses"

    fun serviceGroupTag(groupId: WivaServiceGroupId): String =
        when (groupId) {
            WivaServiceGroupId.Dashboard -> "service_group_dashboard"
            WivaServiceGroupId.Telemetry -> "service_group_telemetry"
            WivaServiceGroupId.Debug -> "service_group_debug"
            WivaServiceGroupId.Integrations -> "service_group_integrations"
            WivaServiceGroupId.CardPayment -> "service_group_card_payment"
            WivaServiceGroupId.Equipment -> "service_group_equipment"
            WivaServiceGroupId.Maintenance -> "service_group_maintenance"
            WivaServiceGroupId.Settings -> "service_group_settings"
            WivaServiceGroupId.Updater -> "service_group_updater"
            WivaServiceGroupId.Performance -> "service_group_performance"
            WivaServiceGroupId.Metrics -> "service_group_metrics"
        }

    fun serviceSubTabTag(subTabId: WivaServiceSubTabId): String =
        when (subTabId) {
            WivaServiceSubTabId.DashboardOverview -> "service_subtab_dashboard_overview"
            WivaServiceSubTabId.TelemetryConnection -> "service_subtab_telemetry_connection"
            WivaServiceSubTabId.TelemetryAddresses -> "service_subtab_telemetry_addresses"
            WivaServiceSubTabId.TelemetryWsLogs -> "service_subtab_telemetry_ws_logs"
            WivaServiceSubTabId.TelemetryInventory -> "service_subtab_telemetry_inventory"
            WivaServiceSubTabId.TelemetryTests -> "service_subtab_telemetry_tests"
            WivaServiceSubTabId.DebugWsLogs -> "service_subtab_debug_ws_logs"
            WivaServiceSubTabId.DebugController -> "service_subtab_debug_controller"
            WivaServiceSubTabId.DebugSubscription -> "service_subtab_debug_subscription"
            WivaServiceSubTabId.DebugKeyboardTest -> "service_subtab_debug_keyboard_test"
            WivaServiceSubTabId.Sbp -> "service_subtab_sbp"
            WivaServiceSubTabId.Nanokassa -> "service_subtab_nanokassa"
            WivaServiceSubTabId.Max -> "service_subtab_max"
            WivaServiceSubTabId.Controller -> "service_subtab_controller"
            WivaServiceSubTabId.Payment -> "service_subtab_payment"
            WivaServiceSubTabId.Scanner -> "service_subtab_scanner"
            WivaServiceSubTabId.Rfid -> "service_subtab_rfid"
            WivaServiceSubTabId.Inventory -> "service_subtab_inventory"
            WivaServiceSubTabId.CalibrationSyrup -> "service_subtab_calibration_syrup"
            WivaServiceSubTabId.CalibrationWater -> "service_subtab_calibration_water"
            WivaServiceSubTabId.PreparingTime -> "service_subtab_preparing_time"
            WivaServiceSubTabId.DevMode -> "service_subtab_dev_mode"
            WivaServiceSubTabId.Idle -> "service_subtab_idle"
            WivaServiceSubTabId.Window -> "service_subtab_window"
            WivaServiceSubTabId.Theme -> "service_subtab_theme"
            WivaServiceSubTabId.PerformanceGeneral -> "service_subtab_performance_general"
            WivaServiceSubTabId.Animation -> "service_subtab_animation"
            WivaServiceSubTabId.MetricsMemory -> "service_subtab_metrics_memory"
            WivaServiceSubTabId.CardPaymentMethod -> "service_subtab_card_payment_method"
            WivaServiceSubTabId.TwoCanServiceSettings -> "service_subtab_two_can_settings"
            WivaServiceSubTabId.AqsiServiceSettings -> "service_subtab_aqsi_settings"
            WivaServiceSubTabId.AqsiServiceDiagnostics -> "service_subtab_aqsi_diagnostics"
        }

    /** Every declared tag must be unique (UiAutomator resource-id collision). */
    val allDeclaredTags: Set<String> =
        setOf(
            SERVICE_MENU_ROOT,
            SERVICE_MENU_CLOSE,
            TELEMETRY_CONNECTION_ROOT,
            TELEMETRY_CONNECTION_STATUS_CARD,
            TELEMETRY_CONNECTION_STATUS_TEXT,
            TELEMETRY_CONNECTION_RECONNECT,
            TELEMETRY_SERIAL_INPUT,
            TELEMETRY_RESERVE_SERIAL,
            TELEMETRY_REGISTER,
            TELEMETRY_CONNECT_WS,
            TELEMETRY_DISCONNECT_WS,
            TELEMETRY_REBIND_CARD,
            TELEMETRY_REBIND_CONFIRM,
            TELEMETRY_REBIND_CANCEL,
            TELEMETRY_REG_KEY_INPUT,
            TELEMETRY_QR_SCANNED_BANNER,
            TELEMETRY_BANNER,
            TELEMETRY_BUSY,
            TELEMETRY_ADDRESSES_ROOT,
            TELEMETRY_API_URL_INPUT,
            TELEMETRY_WS_URL_INPUT,
            TELEMETRY_SAVE_ADDRESSES,
        ) +
            WivaServiceMenuGroups.map { serviceGroupTag(it.id) }.toSet() +
            WivaServiceMenuGroups.flatMap { group -> group.subTabs.map { serviceSubTabTag(it.id) } }.toSet()
}
