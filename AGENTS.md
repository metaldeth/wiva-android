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

**Обязательная платформа для проверки:** приложение **Wiva Android** должно устанавливаться, запускаться и проходить релизный smoke на AVD с именем **`wiva`** (не заменять «на любой пиксель» без причины — вёрстка и UX завязаны на этот профиль).

| Параметр | Значение |
|----------|----------|
| Имя AVD | **`wiva`** |
| API | **25** (Android 7.1.1), образ **Google APIs**, **x86** |
| Экран | **768×1024**, портрет, **mdpi (120 dpi)** — ориентир **15.6″**, как эталонный wiva / профиль `snack-156-768x1024` |
| Каталог AVD (эталон Windows) | **`F:\AndroidAVD`** |

Чтобы **`wiva`** был виден в Device Manager и в `emulator -list-avds`, задайте переменные окружения: **`ANDROID_AVD_HOME=F:\AndroidAVD`**, при необходимости **`ANDROID_SDK_ROOT=F:\AndroidSDK`** (или `ANDROID_HOME`).

Совпадает с `minSdk = 25` в `app/build.gradle.kts`. В APK не заданы ограничивающие `abiFilters` — сборка содержит x86/x86_64, эмулятор ставится без `INSTALL_FAILED_NO_MATCHING_ABIS`.

Для теста на этом эмуляторе используйте **release**, а не debug: так проверяются R8/ProGuard, Hilt и поведение, близкое к продакшену (как в `legacy Android kiosk`).

1. Запустите AVD **wiva** (см. таблицу выше).
2. Установка и запуск (Windows, SDK из `local.properties` → `sdk.dir`):

```bat
set ANDROID_HOME=F:\AndroidSDK
gradlew.bat installRelease
%ANDROID_HOME%\platform-tools\adb.exe shell am start -n com.wiva.android/.ui.MainActivity
```

Если уже стоит сборка с другой подписью: `adb uninstall com.wiva.android`, затем снова `installRelease`.

Полный OTA и Docker: `docs/OTA_UPDATE.md`.

## Офис / железо (этап F)

Офисная приёмка контроллера и платёжного терминала — шаблон чеклиста с полями подписи, версии APK/build, мок↔реал и ссылками на эталон **wiva_electron**:

- **`docs/OFFICE_HARDWARE_CHECKLIST.md`** — основной шаблон (F1 контроллер, F2 терминал, PCI, без PAN/CVV в примерах).
- Основное ТЗ: `docs/TZ_WIVA_ANDROID_FROM_ELECTRON.md` (**§2** источники по контроллеру/терминалу, **§3** мок).
- Порты serial: `wiva_electron/docs/SERIAL_PORTS_IMPLEMENTATION.md` (external reference repo).

Агентский трек модуля F: `docs/agents/wiva-android-from-electron/modules/stage-f-office/summary.md`.

## Медиа экрана выбора напитка (из wiva_electron)

Ролики промо и PNG горизонтальных карточек копируются из `wiva_electron/src/renderer/assets/video` и `.../assets/img/horizontalCard` в `app/src/main/assets/wiva_electron/video/` и `.../img/horizontalCard/` (те же имена файлов). Маппинг в коде: `ui/screens/customer/WivaElectronAssets.kt`; воспроизведение: `WivaPromoVideoCard.kt`, Coil в `WivaDrinkCard` (`DrinkListScreen.kt`). При смене набора роликов в electron обновить список `PROMO_VIDEO_FILES` и при необходимости пересинхронизировать файлы.
