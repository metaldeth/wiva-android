# task-04 — отчёт по тестам (DI: AqsiModule)

**Дата:** 2026-05-12  
**sessionId:** `wiva-aqsi-card-payment`

## Команды (финальная проверка после cleanup)

| Команда | Результат |
|---------|-----------|
| `gradlew.bat assembleDebug` | OK |
| `gradlew.bat :app:testDebugUnitTest --tests com.wiva.android.di.AqsiModuleProvidesContractTest` | OK (запуск **последовательно**; параллельный запуск со второй сборкой падал из‑за **KSP file lock**, не из‑за тестов) |
| `gradlew.bat :app:connectedDebugAndroidTest` | OK — **3 теста** на эмуляторе **snack-101-800x1280** (AVD); runner процесса: `com.wiva.android.WivaHiltTestRunner` |

**Ранее (узкий instrumented-запуск):**  
`gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wiva.android.AqsiHiltInjectionTest` — OK, **2 теста** (для отладки / фильтра по классу).

## Post-review cleanup: Version Catalog

После замечаний ревью зависимости **androidTest** выровнены через каталог версий:

- **`gradle/libs.versions.toml`:** добавлены aliases `hilt-android-testing`, `androidx-test-junit`, `androidx-test-runner`.
- **`app/build.gradle.kts`:** зависимости `androidTest*` переведены на `libs.*` (те же артефакты, единый источник версий).

Сборка и тесты после этого — см. таблицу выше.

## Исправление instrumented smoke (после падения)

**Симптом:** `Unable to instantiate instrumentation ... dagger.hilt.android.testing.HiltTestRunner: ClassNotFoundException`, 0 tests.

**Причина:** строка `dagger.hilt.android.testing.HiltTestRunner` в `defaultConfig.testInstrumentationRunner` не соответствует классу, попадающему в androidTest APK (артикул `hilt-android-testing` рассчитан на паттерн из [документации Hilt](https://dagger.dev/hilt/instrumentation-testing.html): свой наследник `AndroidJUnitRunner` + `HiltTestApplication`).

**Что сделано:** добавлен `com.wiva.android.WivaHiltTestRunner`; в `app/build.gradle.kts` задан `testInstrumentationRunner = "com.wiva.android.WivaHiltTestRunner"`.

## Что покрыто тестами

1. **Компиляция Hilt / графа:** успешные `assembleDebug`, ksp/Hilt без ошибок.
2. **Instrumented:** полный `connectedDebugAndroidTest` — 3 теста; smoke `AqsiHiltInjectionTest` — резолв `AqsiRepository`, singleton `AqsiLastOperationSnapshotHolder` через `@HiltAndroidTest` и обновлённый runner.
3. **JVM-контракт провайдеров:** `AqsiModuleProvidesContractTest` — типы провайдеров `AqsiModule` при ручной сборке аргументов.
4. **Singleton holder:** `assertSame` для двух `@Inject` полей `AqsiLastOperationSnapshotHolder` в `AqsiHiltInjectionTest`.

## Файлы тестов и runner

- `app/src/androidTest/java/com/wiva/android/WivaHiltTestRunner.kt`
- `app/src/androidTest/java/com/wiva/android/AqsiHiltInjectionTest.kt`
- `app/src/test/java/com/wiva/android/di/AqsiModuleProvidesContractTest.kt`
