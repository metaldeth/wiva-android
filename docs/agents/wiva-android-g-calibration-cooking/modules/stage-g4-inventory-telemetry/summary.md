# G4 — Остатки и телеметрия: итог

## Статус

Реализация выполнена; `gradlew.bat assembleDebug` и `gradlew.bat :app:testDebugUnitTest` — **успешно** (версия `26.04.07.04` / `versionCode` 49).

## Сделано

| Область | Файлы / изменения |
|--------|---------------------|
| Списание после готовки | `InventoryService`, `InventoryWriteOffMath` (формула как electron `applyInventoryWriteOff`) |
| Репозиторий | `MachineInventoryRepository`: `deductContainerVolume`, `applyCellVolumes`, `findContainerByNumber`, `findDrinkContainerByProductId`; в `MachineInventoryRepositoryImpl` — `Lazy<ViwaTelemetryService>` для вызова `sendCellVolumeImportFromConfig` после ручных объёмов (без цикла DI с `ViwaTelemetryService`) |
| Счётчик воды | `JsonStoreKeys.WATER_USAGE_ML`, запись в `InventoryService.applyWriteOff` |
| Preparing | `PreparingManager`: вместо заглушки — `inventoryService.applyWriteOff` + `telemetryService.sendSaleImportTopic` после `DrinkPreparingSuccess`; параметры `saleTotalPriceRub` / `salePayMethod` |
| Клиент | `DrinkListViewModel`: передача суммы и способа оплаты в `prepareDrink` для платного флоу |
| Телеметрия | `SaleImportItem` (+ `totalChargedRub`), перегрузка `ViwaTelemetryService.sendSaleImportTopic(List<SaleImportItem>)` с маппингом в `SaleImportItemJson` и предупреждением при offline |
| DI | Удалён `InventoryWriteOffCallback`; `PreparingModule` только для `PreparingStateCallback` |
| Сервисное меню | Вкладка «Остатки» (`ViwaInventoryVolumesTab`): список ячеек, ввод объёма, «Сохранить» → `applyCellVolumes` |
| Тесты | `InventoryWriteOffMathTest` — расчёт product/water и концентрации |

## Артефакты сессии

- `request.md` — постановка
- `orchestrator-log.md` — краткий лог (полный пайплайн analyst→planner не запускался, задача была детально задана в запросе)
- `summary.md` — этот файл

## Рекомендации

- При необходимости e2e: мок контроллера + проверка `WATER_USAGE_ML` и merge-конфига после успешной готовки.
- Документация `TELEMETRY_EXCHANGES_INVENTORY.md` по желанию обновить под новую перегрузку `sendSaleImportTopic`.
