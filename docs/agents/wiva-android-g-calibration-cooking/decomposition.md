# Декомпозиция: viwa-android-g-calibration-cooking

## Модули

### Модуль 1: stage-g1-water-calibration

- **Описание:** Вкладка «Калибровка воды» в сервисном меню. Тестовый налив (`ServiceCommand 0x52/0x0A`), замер времени BEGIN→SUCCESS, ввод фактического объёма, запись коэффициента (`WriteWaterPumpModel`), сохранение `flowRateMlPerSec` в локальное хранилище.
- **Скоуп:** `ui/screens/service/tabs/`, `services/calibration/WaterCalibrationService.kt` (новый), `domain/` (модели), `data/local/` (хранилище)
- **Зависимости:** нет (аппаратный слой готов, этап B)

### Модуль 2: stage-g2-syrup-calibration

- **Описание:** Вкладка «Калибровка сиропов/концентрата» в сервисном меню. Тестовый налив по контейнеру (`ServiceCommand 0x52/0x09`), ввод фактического объёма, пересчёт `conversionFactor`, сохранение в конфиг, отправка `cellVolumeImportTopic` + `cellStoreImportTopic` в телеметрию.
- **Скоуп:** `ui/screens/service/tabs/`, `services/calibration/SyrupCalibrationService.kt` (новый), `domain/`, `data/local/`
- **Зависимости:** нет (параллельно G1)

### Модуль 3: stage-g3-cooking-flow

- **Описание:** Полный флоу готовки: `ensureAutoMode` → найти контейнер → `ChooseDrink 0x50` → 200ms → `StartDrinkPreparing 0x55` → состояния BEGIN/SUCCESS/FAIL/CUP_TAKEN → прогресс-бар в UI. Расчёт `preparingTime = round(waterMl / flowRateMlPerSec)` только из калибровки. Без `flowRateMlPerSec` — FAIL с `WATER_NOT_CALIBRATED`.
- **Скоуп:** `services/drink/ViwaDrinkPreparingService.kt` (расширить), `services/drink/DrinkSelectionService.kt` (новый), `modules/preparing/PreparingManager.kt` (новый), `ui/screens/customer/PreparingScreen.kt` (подключить), `hardware/controller/` (команды уже есть)
- **Зависимости:** G1 завершён (нужен `flowRateMlPerSec` в хранилище)

### Модуль 4: stage-g4-inventory-telemetry

- **Описание:** Списание остатков на `DrinkPreparingSuccess` (productWriteOff + waterUsageMl), отправка `cellVolumeImportTopic`. Отправка `saleImportTopic` после успешной готовки. Ручная вкладка остатков в сервисном меню (`applyCellVolumes`).
- **Скоуп:** `modules/preparing/PreparingManager.kt` (вызов списания), `services/inventory/InventoryService.kt` (новый), `ui/screens/service/tabs/` (вкладка остатков), `services/telemetry/ViwaTelemetryService.kt` (методы send)
- **Зависимости:** G3 завершён (нужен SUCCESS-event и `PreparingManager`)

## Волны выполнения

| Волна | Модули | Комментарий |
| ----- | ------ | ----------- |
| 1 | stage-g1-water-calibration, stage-g2-syrup-calibration | Независимые, параллельно |
| 2 | stage-g3-cooking-flow | Зависит от G1 (flowRateMlPerSec) |
| 3 | stage-g4-inventory-telemetry | Зависит от G3 (PreparingManager, SUCCESS) |

## Общие сущности

- **Модели:** `ContainerConfig`, `DosageConfig`, `WaterCalibrationInfo` — shared domain
- **Хранилище:** ключи в `JsonStoreKeys.kt` — добавить `WATER_CALIBRATION`, `WATER_USAGE_ML`
- **Контроллер:** `RequestCommand` (ChooseDrink=0x50, ServiceCommand=0x52, StartDrinkPreparing=0x55, ReadWaterPumpModel=0xBC, WriteWaterPumpModel=0xBB) — уже есть
- **Телеметрия:** `ViwaTelemetryService` — добавить методы `sendCellVolumeImportFromConfig`, `sendCellStoreImportFromConfig`, `sendSaleImportTopic`
- **PreparingState enum:** START_PREPARING, BEGIN, SUCCESS, FAIL, CUP_TAKEN — нужен для G3 и G4
