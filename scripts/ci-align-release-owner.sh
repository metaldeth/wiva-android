#!/bin/sh
# После docker cp файлы в release/ часто root:root на хосте.
# Владелец должен совпасть с пользователем job, чтобы дальше cp и git clean не падали по правам.
set -e
WS="${CI_PROJECT_DIR:-.}"
REL="${WS}/release"
UID="$(id -u)"
GID="$(id -g)"

mkdir -p "$REL"
set -- "${REL}"/*.apk
[ -e "$1" ] || exit 0

if chown -R "${UID}:${GID}" "$REL" 2>/dev/null; then
  echo "ci-align-release-owner: chown ok (${UID}:${GID})"
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: chown release/ не удался и docker недоступен" >&2
  exit 1
fi

docker run --rm -v "${WS}:/ws:rw" alpine:3.20 sh -c "chown -R ${UID}:${GID} /ws/release"
echo "ci-align-release-owner: chown через docker -> ${UID}:${GID}"
