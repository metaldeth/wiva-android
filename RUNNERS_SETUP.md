# GitLab Runner Setup

## Проверка существующих runner'ов

Посмотреть зарегистрированные runner'ы на сервере:

```bash
sudo gitlab-runner list
```

Пример уже найденного runner'а:

- `android-kiosk-runner (legacy name)`
- `Executor=shell`
- `URL=https://gitlab.com`

Проверить статус systemd-сервиса:

```bash
sudo systemctl status gitlab-runner
```

Проверить процессы runner'а:

```bash
ps aux | grep gitlab-runner
```

Если runner используется через Docker:

```bash
docker ps --filter "name=gitlab-runner"
```

## Создание runner'а для `wiva-android`

Новый runner должен называться:

- `viwa-android-runner`

Важно: если токен runner'а начинается с `glrt-`, это новый GitLab authentication token.
В этом режиме `tag-list`, `locked`, `run-untagged`, `access-level` и похожие параметры
нельзя передавать в `gitlab-runner register`. Их нужно задать в GitLab UI при создании runner'а.

### Интерактивная регистрация

```bash
sudo gitlab-runner register
```

Во время регистрации указать:

- GitLab URL: `https://gitlab.com`
- Token: токен runner'а проекта `wiva-android`
- Description: `viwa-android-runner`
- Executor: `shell`

Теги для runner'а (`android,wiva`) нужно задать в GitLab UI заранее, на шаге создания runner'а.

### Non-interactive регистрация

```bash
sudo gitlab-runner register \
  --non-interactive \
  --url "https://gitlab.com" \
  --token "<WIVA_RUNNER_TOKEN>" \
  --executor "shell" \
  --description "viwa-android-runner"
```

Где `<WIVA_RUNNER_TOKEN>` — токен нового runner'а из GitLab проекта `wiva-android`.

Если GitLab всё равно ругается на `--description`, используй самый минимальный вариант:

```bash
sudo gitlab-runner register \
  --non-interactive \
  --url "https://gitlab.com" \
  --token "<WIVA_RUNNER_TOKEN>" \
  --executor "shell"
```

После этого имя и теги проверь и при необходимости поправь в GitLab UI.

## Проверка после регистрации

После создания runner'а снова выполнить:

```bash
sudo gitlab-runner list
```

Ожидаемый результат: в списке должны быть как минимум:

- `android-kiosk-runner (legacy name)`
- `viwa-android-runner`
