#!/bin/sh
# Очистка workspace перед/после job.
# Сборка идёт через docker build --target ci-build, поэтому app/build в клоне не нужен.
# Порядок: rm от job -> docker alpine -> sudo (WIVA_CI_SUDO_CLEANUP=1).
WS="${CI_PROJECT_DIR:-}"
[ -n "$WS" ] && [ -d "$WS" ] || exit 0

rm -rf "${WS}/app/build" "${WS}/build" "${WS}/.gradle" "${WS}/.kotlin" "${WS}/.gradle-docker" 2>/dev/null || true
rm -f "${WS}/release"/*.apk 2>/dev/null || true

if command -v docker >/dev/null 2>&1; then
  docker run --rm -v "${WS}:/ws:rw" alpine:3.20 sh -c \
    'rm -rf /ws/app/build /ws/build /ws/.gradle /ws/.kotlin /ws/.gradle-docker; rm -f /ws/release/*.apk' \
    2>/dev/null || true
fi

if [ "${WIVA_CI_SUDO_CLEANUP:-}" = "1" ] && command -v sudo >/dev/null 2>&1; then
  sudo -n rm -rf "${WS}/app/build" "${WS}/build" "${WS}/.gradle" "${WS}/.kotlin" "${WS}/.gradle-docker" 2>/dev/null || true
  sudo -n find "${WS}/release" -maxdepth 1 -type f -name '*.apk' -delete 2>/dev/null || true
fi

exit 0
