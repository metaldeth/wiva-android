package com.wiva.android.ui.screens.service

/** Группы и подвкладки сервисного меню; телеметрия — четыре горизонтальных таба (подключение, адреса, наполнение, тесты). Интеграции (СБП, Нанокасса, MAX) — отдельная группа. Rail+табы. */

sealed interface WivaServiceGroupId {
 /** Сводка по автомату: ячейки, связь, вода. */
    data object Dashboard : WivaServiceGroupId

    data object Telemetry : WivaServiceGroupId

 /** Логи сети и дебаг контроллера. */
    data object Debug : WivaServiceGroupId

    data object Integrations : WivaServiceGroupId

 /** Настройка и диагностика карточного метода (PAX / aQsi), см. ТЗ A6. */
    data object CardPayment : WivaServiceGroupId

    data object Equipment : WivaServiceGroupId

    data object Maintenance : WivaServiceGroupId

    data object Settings : WivaServiceGroupId

    data object Updater : WivaServiceGroupId

    data object Performance : WivaServiceGroupId

    data object Metrics : WivaServiceGroupId
}

sealed interface WivaServiceSubTabId {
    data object DashboardOverview : WivaServiceSubTabId

    data object TelemetryConnection : WivaServiceSubTabId

    data object TelemetryAddresses : WivaServiceSubTabId

 /** Перенесено в группу «Дебаг». */
    data object TelemetryWsLogs : WivaServiceSubTabId

 /** Итог merge базы + матрицы. */
    data object TelemetryInventory : WivaServiceSubTabId

    data object TelemetryTests : WivaServiceSubTabId

    data object DebugWsLogs : WivaServiceSubTabId

    data object DebugController : WivaServiceSubTabId

    data object DebugSubscription : WivaServiceSubTabId

 /** Тест WivaNumericKeyboard / WivaAlphanumericKeyboard без IME. */
    data object DebugKeyboardTest : WivaServiceSubTabId

    data object Sbp : WivaServiceSubTabId

    data object Nanokassa : WivaServiceSubTabId

    data object Max : WivaServiceSubTabId

    data object Controller : WivaServiceSubTabId

    data object Payment : WivaServiceSubTabId

    data object Scanner : WivaServiceSubTabId

    data object Rfid : WivaServiceSubTabId

    data object Inventory : WivaServiceSubTabId

    data object CalibrationSyrup : WivaServiceSubTabId

    data object CalibrationWater : WivaServiceSubTabId

    data object PreparingTime : WivaServiceSubTabId

    data object DevMode : WivaServiceSubTabId

    data object Idle : WivaServiceSubTabId

    data object Window : WivaServiceSubTabId

    data object Theme : WivaServiceSubTabId

    data object PerformanceGeneral : WivaServiceSubTabId

    data object Animation : WivaServiceSubTabId

    data object MetricsMemory : WivaServiceSubTabId

 /** Вкладка «Метод» — выбор 2can/PAX или aQsi. */
    data object CardPaymentMethod : WivaServiceSubTabId

    data object TwoCanServiceSettings : WivaServiceSubTabId

    data object AqsiServiceSettings : WivaServiceSubTabId

    data object AqsiServiceDiagnostics : WivaServiceSubTabId
}

data class WivaServiceSubTabSpec(
    val id: WivaServiceSubTabId,
    val label: String,
)

data class WivaServiceGroupSpec(
    val id: WivaServiceGroupId,
    val label: String,
    val subTabs: List<WivaServiceSubTabSpec>,
)

val WivaServiceMenuGroups: List<WivaServiceGroupSpec> =
    listOf(
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Dashboard,
            label = "Дашборд",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DashboardOverview, "Обзор"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Telemetry,
            label = "Телеметрия",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.TelemetryConnection, "Подключение"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.TelemetryAddresses, "Адреса"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.TelemetryInventory, "Наполнение"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.TelemetryTests, "Тесты"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Debug,
            label = "Дебаг",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DebugWsLogs, "Логи сети"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DebugController, "Дебаг контроллера"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DebugSubscription, "Подписка"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DebugKeyboardTest, "Клавиатура (тест)"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Integrations,
            label = "Интеграции",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Sbp, "СБП"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Nanokassa, "Нанокасса"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Max, "MAX"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.CardPayment,
            label = "Оплата картой",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.CardPaymentMethod, "Метод"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.TwoCanServiceSettings, "2can"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.AqsiServiceSettings, "Новый считыватель"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.AqsiServiceDiagnostics, "Тесты и журнал"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Equipment,
            label = "Оборудование",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Controller, "Контроллер"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Scanner, "Сканер"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Rfid, "RFID"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Payment, "Платёжник"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Maintenance,
            label = "Обслуживание",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Inventory, "Остатки"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.CalibrationSyrup, "Калибровка сиропов"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.CalibrationWater, "Калибровка воды"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.PreparingTime, "Время готовки"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Settings,
            label = "Настройки",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.DevMode, "Режим разработки"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Idle, "Ожидание"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Window, "Окно"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Theme, "Тема"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Updater,
            label = "Обновление",
            subTabs = emptyList(),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Performance,
            label = "Производительность / Оптимизация",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.PerformanceGeneral, "Общие настройки"),
                    WivaServiceSubTabSpec(WivaServiceSubTabId.Animation, "Анимации"),
                ),
        ),
        WivaServiceGroupSpec(
            id = WivaServiceGroupId.Metrics,
            label = "Метрики",
            subTabs =
                listOf(
                    WivaServiceSubTabSpec(WivaServiceSubTabId.MetricsMemory, "Память и ресурсы"),
                ),
        ),
    )

fun findWivaServiceGroup(id: WivaServiceGroupId): WivaServiceGroupSpec =
    WivaServiceMenuGroups.firstOrNull { it.id == id }
        ?: error("Unknown WivaServiceGroupId: $id")

fun WivaServiceGroupSpec.subTabIndexOf(subTabId: WivaServiceSubTabId?): Int {
    if (subTabId == null) return -1
    return subTabs.indexOfFirst { it.id == subTabId }
}
