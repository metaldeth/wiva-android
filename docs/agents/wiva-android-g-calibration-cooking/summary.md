# Summary: viwa-android-g-calibration-cooking

**Статус:** ✅ Завершено  
**Дата:** 2026-04-07

## Модули

| Модуль | Статус | Версия | Ключевые артефакты |
|---|---|---|---|
| G1 — Калибровка воды | ✅ | 26.04.07.02 / 47 | WaterCalibrationService, ViwaWaterCalibrationTab |
| G2 — Калибровка сиропов | ✅ | 26.04.07.02 / 47 | SyrupCalibrationService, ViwaSyrupCalibrationTab |
| G3 — Процесс готовки | ✅ | 26.04.07.03 / 48 | ViwaDrinkSelectionService, PreparingManager, ViwaControllerStateService |
| G4 — Остатки + телеметрия | ✅ | 26.04.07.04 / 49 | InventoryService, ViwaInventoryVolumesTab, saleImportTopic |

## Итоговая версия приложения

`26.04.07.04` / versionCode `49`

## Что реализовано

### Калибровка воды (G1)
- Тестовый налив: `ServiceCommand 0x52/0x0A`, замер времени BEGIN→SUCCESS
- Запись коэффициента помпы: `ReadWaterPumpModel` (0xBC) → пересчёт → `WriteWaterPumpModel` (0xBB) → ACK
- Сохранение `flowRateMlPerSec` в хранилище (`WATER_CALIBRATION`)
- **Без дефолта 20 мл/с** — если нет калибровки, готовка недоступна
- Unit-тесты: WaterCalibrationCalculationsTest

### Калибровка сиропов (G2)
- Тестовый налив: `ServiceCommand 0x52/0x09`, `physicalPort = containerNumber + 8`
- Пересчёт `newCF = currentCF * (actualVolumeMl / targetProductMl)`
- После сохранения: `cellVolumeImportTopic` + `cellStoreImportTopic` в телеметрию
- Unit-тесты: SyrupCalibrationMathTest

### Процесс готовки (G3)
- `ensureAutoMode` → поиск контейнера → `flowRateMlPerSec` → `ChooseDrink 0x50` → 200ms → `StartDrinkPreparing 0x55`
- Формула: `preparingTime = round(waterMl / flowRateMlPerSec)` — только из калибровки
- Состояния: START_PREPARING → BEGIN(timeSec) → SUCCESS | FAIL | CUP_TAKEN
- Без калибровки → FAIL с errorCode=WATER_NOT_CALIBRATED
- Прогресс-бар в PreparingScreen по `BEGIN(timeSec)`
- Unit-тесты: DrinkPreparationCalculationsTest, ChooseDrinkBodyBuilderTest

### Остатки + телеметрия (G4)
- Списание: `productWriteOff = dosage.product * ratio * concentrationRatio`; `waterMl = round(dosage.water * ratio)`
- `deductContainerVolume` + `WATER_USAGE_ML` счётчик
- `cellVolumeImportTopic` после каждого списания
- `saleImportTopic` после каждого успешного напитка
- Вкладка остатков в сервисном меню: ручной ввод + сохранение + телеметрия
- Unit-тесты: InventoryWriteOffMathTest

## Интеграционная проверка

- `assembleDebug` → ✅ BUILD SUCCESSFUL
- `testDebugUnitTest` → ✅ BUILD SUCCESSFUL

## Артефакты

- `docs/agents/viwa-android-g-calibration-cooking/modules/stage-g1-water-calibration/summary.md`
- `docs/agents/viwa-android-g-calibration-cooking/modules/stage-g2-syrup-calibration/summary.md`
- `docs/agents/viwa-android-g-calibration-cooking/modules/stage-g3-cooking-flow/summary.md`
- `docs/agents/viwa-android-g-calibration-cooking/modules/stage-g4-inventory-telemetry/summary.md`
