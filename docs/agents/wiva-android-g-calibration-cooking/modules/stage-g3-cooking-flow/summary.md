# Итог модуля G3 — Процесс готовки напитка

## Статус

Реализован полный флоу: `ensureAutoMode` → поиск контейнера по `taste.id` → проверка калибровки воды → `ChooseDrink` (0x50) → пауза 200 ms → `StartDrinkPreparing` (0x55) → ожидание `DrinkPreparingSuccess` → UI «готово» и колбэк `InventoryWriteOffCallback` (заглушка для G4).

## Ключевые файлы

| Назначение | Путь |
|------------|------|
| Состояния (домен) | `domain/model/preparing/PreparingState.kt` |
| Режим Auto | `services/controller/ViwaControllerStateService.kt` |
| Рецепт 0x50 | `services/drink/ViwaDrinkSelectionService.kt`, `ChooseDrinkBodyBuilder.kt` |
| Расчёт времени | `services/drink/DrinkPreparationCalculations.kt` |
| Оркестрация | `services/preparing/PreparingManager.kt` |
| Колбэки G4 | `services/preparing/InventoryWriteOffCallback.kt`, `PreparingStateCallback.kt` |
| Контейнер по вкусу | `MachineInventoryRepository.findDrinkContainerByTasteId` |
| UI | `PreparingScreen.kt`, `PreparingViewModel.kt`, `DrinkListViewModel.kt`, `WivaNavGraph.kt` |
| DI | `di/PreparingModule.kt` |
| Тесты | `ChooseDrinkBodyBuilderTest.kt`, `DrinkPreparationCalculationsTest.kt` |

## Поведение

- **Без калибровки** (`flowRateMlPerSec` null или ≤ 0): `PrepareDrinkResult.Error(WATER_NOT_CALIBRATED, …)` и баннер на списке напитков.
- **Авто-режим**: при ошибке — `AUTO_MODE_SWITCH_FAILED`.
- **Контейнер не найден** — `CONTAINER_NOT_FOUND`.
- **Прогресс**: обратный отсчёт «Осталось ~ N с из T с»; по `DrinkPreparingSuccess` — экран «ВОЗЬМИТЕ НАПИТОК».

## Версия

`versionName` / `versionCode` обновлены в `app/build.gradle.kts` (26.04.07.03 / 48).

## Сборка

- `gradlew.bat assembleDebug` — OK  
- `gradlew.bat :app:testDebugUnitTest` — OK  

## Артефакты

- `request.md` — фиксация запроса  
- `orchestrator-log.md` — лог оркестратора  
