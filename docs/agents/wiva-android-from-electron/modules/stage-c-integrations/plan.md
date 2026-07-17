# План: stage-c-integrations (C1–C4)

Волны (после каждой из C1–C3 — `assembleDebug`):

1. **C1 — MAX:** `domain` + `data/remote/max` + `MaxRepositoryImpl` + `JsonStoreKeys.MAX_SETTINGS` + Hilt (`IntegrationsModule`) + сервисный UI «Сохранить» для токена MAX.
2. **C2 — СБП:** `data/remote/sbp` + `SBPRepositoryImpl` + доменные модели + юнит-тесты `PaymasterXmlParserTest`.
3. **C3 — Нанокасса:** `data/remote/nanokassa` + доменные модели чека + `NanoKassaRepositoryImpl` + `JsonStoreKeys.MACHINE_REGISTRATION` (серийник для чека).
4. **C4:** единый `IntegrationsModule`, `testInstrumentationRunner` Hilt, `IntegrationsHiltInjectionTest` (androidTest).

Источник кода: `legacy Android kiosk` (§2 ТЗ).
