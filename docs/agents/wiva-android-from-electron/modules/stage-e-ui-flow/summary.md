# Итог этапа: stage-e-ui-flow (модуль E)

## Закрытые ID ТЗ

| ID | Статус | Комментарий |
|----|--------|-------------|
| **E1** | Закрыто | Главный экран: выбор напитка/объёма (300/700), вода (STANDARD/COLD/SPARK), концентрация (слабее/эталон/крепче), недоступность по остатку как `isContainerUnavailable` в electron. Happy-path на моке: бесплатный режим → `ChooseDrink` (тело как `chooseDrinkBody.ts` + формулы `DrinkSelectionService.ts`). Альтернатива: выкл. «Бесплатный режим» → `SendSumToPaymentTerminal` (0x48) + симуляция PAX 4 в моке → `ChooseDrink`. |
| **E2** | Закрыто (структура + доработка dev) | Порядок групп/подвкладок без изменений — `WivaServiceMenuStructure.kt` зеркалит electron. Добавлен переключатель **«Бесплатный режим»** в «Настройки → Режим разработки» (`JsonStoreKeys.DEV_FREE_MODE`). Уточнён текст вкладки «База и наполнение» (демо-каталог на главном экране). Остальные заглушки оборудования/обслуживания/метрик — по-прежнему до паритета с wiva по отдельным задачам. |
| **E3+** | Частично | Новый экран **`PreparingScreen`** (маршрут `preparing/{productId}/{estSeconds}`) — плейсхолдер после заказа; полный паритет с `PreparingPage` / IPC preparing — отдельные задачи. |

## Ключевые файлы

- `domain/model/customer/DrinkCatalogModels.kt`, `domain/customer/DemoDrinkCatalog.kt`
- `services/drink/ChooseDrinkBodyBuilder.kt`, `WivaDrinkSelectionService.kt`
- `ui/screens/customer/DrinkListViewModel.kt`, `DrinkListScreen.kt`, `PreparingScreen.kt`
- `ui/screens/home/HomeScreen.kt` — обёртка над `DrinkListScreen`
- `ui/navigation/WivaNavGraph.kt`, `Routes.kt`
- `data/local/db/JsonStoreKeys.kt` — `DEV_FREE_MODE`
- `ServiceViewModel.kt`, `WivaServiceMenuTabContent.kt` — free mode UI

## Тесты

- `ChooseDrinkBodyBuilderTest` — согласование с юнит-тестом wiva `DrinkSelectionService.test.ts` (байты времени/воды/порт).

## Сборка

`gradlew.bat assembleDebug`, `testDebugUnitTest` — OK (2026-04-01).

## Вопросы владельцу (§4.1)

Не блокировали: источники по выбору напитка и ChooseDrink зафиксированы в wiva_electron; отличие только в демо-данных каталога до телеметрии.
