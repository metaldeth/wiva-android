# G1 — Калибровка воды (итог)

## Статус

Реализация выполнена: сервис, хранилище, вкладка сервисного меню, мок-тайминги, unit-тесты расчётов. Сборка: `gradlew.bat assembleDebug`, тесты: `gradlew.bat :app:testDebugUnitTest` — **успешно** (после `clean` при необходимости из-за промежуточного сбоя dex на Windows).

## Изменения по коду

| Область | Файлы |
|--------|--------|
| Модель и ключ | `domain/model/WaterCalibrationData.kt`, `data/local/db/JsonStoreKeys.kt` (`WATER_CALIBRATION`) |
| Логика | `services/calibration/WaterCalibrationCalculations.kt`, `services/calibration/WaterCalibrationService.kt` |
| Контроллер | `ControllerHardwareManager.hasActiveConnection()`, `ControllerConstants` (мок: 1 с до BEGIN, 20 мл/с), `ControllerConnection` (исправлено логирование сиропа `body`) |
| UI | `ui/screens/service/tabs/WivaWaterCalibrationTab.kt`, `WivaServiceMenuTabContent.kt` (вкладка «Калибровка воды») |
| ViewModel | `ServiceViewModel.kt` — состояние и вызовы сервиса |
| Тесты | `WaterCalibrationCalculationsTest.kt`, обновлён `ControllerConnectionMockTest` под новые задержки мока |

## Сопутствующие исправления сборки

Чтобы проект компилировался: `CategoryConfigMachineBuilder` (default `value` у `Entry`), `WivaTelemetryService` (`JsonPrimitive` в `buildJsonObject` для `cellVolumeImportTopic`).

## DoD

- [x] Тестовый налив: `ServiceCommand` 0x52, тело `0x0A` и объём; в моке — BEGIN через 1 с, SUCCESS через `volumeMl/20` с.
- [x] `durationSec` и метаданные налива сохраняются в JSON под `water_calibration`.
- [x] Запись коэффициента: ReadWaterPumpModel → расчёт `newTenths` → WriteWaterPumpModel → ожидание `ControllerACK` → `flowRateMlPerSec` без дефолта 20.
- [x] Вкладка показывает данные калибровки и сценарий налив → факт → сохранение.
- [x] Unit-тесты: `newTenths`, `flowRateMlPerSec`.
- [x] `assembleDebug` и `:app:testDebugUnitTest` проходят.

## Артефакты сессии

- `request.md` — постановка.
- `orchestrator-log.md` — сжатая фиксация (полный цикл субагентов по скиллу не запускался).
- `summary.md` — этот файл.

## Версия APK

`versionName` **26.04.07.02** (`versionCode` 47).
