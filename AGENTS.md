# Wiva Android — конфигурация для AI

| Поле | Значение |
|------|----------|
| **type** | android |
| **buildCommand** | `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`) |
| **tdd** | true |
| **testCommand** | `gradlew.bat :app:testDebugUnitTest` (Windows) |

## Сервисное меню

Настройки и служебные сценарии — только в рамках структуры из **ТЗ §2** (таблица «Сервисное меню»): порядок вкладок как в `wiva_electron` `ServiceMenu/constants.ts`, вёрстка как в `legacy Android kiosk` `ServiceMenuScreen`. Код: `ui/screens/service/WivaServiceMenuStructure.kt`, `ServiceScreen.kt`, `WivaServiceMenuTabContent.kt`; новые вкладки — пакет `.../service/tabs/` по образцу киоска.

## Эмулятор для проверки релиза

**Основная платформа проекта:** приложение **Wiva Android** должно
устанавливаться, запускаться и проходить smoke на AVD
**`wiva-android`**. Профиль повторяет экран исходного `evoq`, использует
Android 11 как подключённая плата и всегда запускается в landscape.

| Параметр | Значение |
|----------|----------|
| Имя AVD | **`wiva-android`** |
| API | **30** (Android 11), образ **Google APIs**, **x86_64** |
| Экран | physical **768×1024**, **mdpi (120 dpi)**; landscape даёт logical **1024×768** |
| Ориентация | **landscape** (`hw.initialOrientation=landscape`) |
| Типичный ADB serial | **`emulator-5556`** |
| Каталог AVD (эталон Windows) | **`F:\AndroidAVD`** |

Чтобы профиль был виден в Device Manager и `emulator -list-avds`, задайте
**`ANDROID_AVD_HOME=F:\AndroidAVD`** и
**`ANDROID_SDK_ROOT=F:\AndroidSDK`** (или `ANDROID_HOME`).

AVD `wiva` (API 25) остаётся дополнительной проверкой совместимости с
`minSdk`, но не является основным профилем проекта.

Для теста на этом эмуляторе используйте **release**, а не debug: так проверяются R8/ProGuard, Hilt и поведение, близкое к продакшену (как в `legacy Android kiosk`).

1. Запустите AVD **wiva-android** (см. таблицу выше; runbook: `docs/AVD_WIVA_ANDROID.md`).
2. Установка и запуск (Windows, SDK из `local.properties` → `sdk.dir`):

```bat
set ANDROID_HOME=F:\AndroidSDK
gradlew.bat installRelease
%ANDROID_HOME%\platform-tools\adb.exe -s emulator-5556 shell am start -n com.wiva.android/.ui.MainActivity
```

Если уже стоит сборка с другой подписью: `adb uninstall com.wiva.android`, затем снова `installRelease`.

Полный OTA и Docker: `docs/OTA_UPDATE.md`.

## Simple Telemetry MVP

- Документация клиента: **`docs/SIMPLE_TELEMETRY_MVP_ANDROID.md`** (REST enroll, WS fallback, 409/rebind, ONLINE после `hello`).
- Локальные ключи (не коммитить): **`local.properties.sample`** → скопировать в `local.properties`; `telemetry.enrollmentKey` = `MACHINE_ENROLLMENT_KEY` на сервере (`wiva-telemetry/.env.example`).
- Серверная документация: `c:\wiva\wiva-telemetry\AGENTS.md`, deploy: `c:\wiva\wiva-telemetry\docs\deployment\server.md`.

## Офис / железо (этап F)

Офисная приёмка контроллера и платёжного терминала — шаблон чеклиста с полями подписи, версии APK/build, мок↔реал и ссылками на эталон **wiva_electron**:

- **`docs/OFFICE_HARDWARE_CHECKLIST.md`** — основной шаблон (F1 контроллер, F2 терминал, PCI, без PAN/CVV в примерах).
- Основное ТЗ: `docs/TZ_WIVA_ANDROID_FROM_ELECTRON.md` (**§2** источники по контроллеру/терминалу, **§3** мок).
- Порты serial: `wiva_electron/docs/SERIAL_PORTS_IMPLEMENTATION.md` (external reference repo).

Агентский трек модуля F: `docs/agents/wiva-android-from-electron/modules/stage-f-office/summary.md`.

## Медиа экрана выбора напитка (из wiva_electron)

Ролики промо и PNG горизонтальных карточек копируются из `wiva_electron/src/renderer/assets/video` и `.../assets/img/horizontalCard` в `app/src/main/assets/wiva_electron/video/` и `.../img/horizontalCard/` (те же имена файлов). Маппинг в коде: `ui/screens/customer/WivaElectronAssets.kt`; воспроизведение: `WivaPromoVideoCard.kt`, Coil в `WivaDrinkCard` (`DrinkListScreen.kt`). При смене набора роликов в electron обновить список `PROMO_VIDEO_FILES` и при необходимости пересинхронизировать файлы.
