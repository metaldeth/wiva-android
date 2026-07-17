# OTA-обновления wiva-android

Кратко: сервер отдаёт `version.json` и APK из каталога `wiva-android/release/`. Имя файла релиза: **`wiva-android-{versionName}-release.apk`** (совпадает с `outputFileName` в `app/build.gradle.kts`).

## Прод: Android-сервер

| Поле | Значение |
|------|----------|
| Хост | `83.166.246.158` |
| Порт wiva `update-server` | **9083** |
| Каталог на VPS | `/opt/wiva-android/` |
| Проверка | `curl http://83.166.246.158:9083/version.json` |

Полная карта сервисов: `.cursor/rules/universal/infra-android-update-server.mdc`.

## Запуск update-server

Из корня репозитория **wiva-android**:

```bash
docker compose up -d update-server
```

Порт **9082**. Проверка (DoD этапа A3):

```bash
curl http://localhost:9082/version.json
```

Должен вернуться JSON с полями `version`, `url`, `changelog` (или 404, если в `release/` нет подходящего APK).

Если устройства ходят на другой хост, задайте URL для поля `url` в ответе:

```bash
export ANDROID_UPDATE_BASE_URL=http://192.168.1.100:9082
docker compose up -d update-server
```

Или в `.env` в корне `wiva-android`:

```env
ANDROID_UPDATE_BASE_URL=http://dev.ishaker.ru:9082
```

После смены `ANDROID_UPDATE_BASE_URL` контейнер нужно перезапустить (переменная читается при старте Node).

При ручном запуске `node update-server/server.js` без Docker задайте `BASE_URL` с **тем же хостом и портом**, на которых слушает процесс (иначе поле `url` в JSON укажет на неверный адрес). Каталог APK: `RELEASE_DIR` (по умолчанию `update-server/release`); для соответствия Docker удобно `RELEASE_DIR=<корень wiva-android>/release`.

## Каталог `release/`

Смонтирован в контейнер как `./release:/app/release:ro`. Положите сюда собранный release APK. Новый файл подхватывается без перезапуска контейнера при следующем `GET /version.json`.

## Приложение

- В экране **Сервис** задаётся URL сервера, кнопки **Сохранить**, **Проверить обновления**, **Установить**.
- Запрос: `GET {url}/version.json`, сравнение `version` с `versionName` установленного приложения.
- Загрузка APK во внутреннее хранилище и установка через `FileProvider` (`REQUEST_INSTALL_PACKAGES` в манифесте).

## Сборка APK

Локально (Windows):

```bat
gradlew.bat assembleRelease
```

APK: `app/build/outputs/apk/release/wiva-android-26.04.01.01-release.apk` (версия из `versionName`). Скопируйте в `release/` для раздачи.

Нужен keystore: `signing/release.jks` (как в legacy Android kiosk; пароли через `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` или env STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).
