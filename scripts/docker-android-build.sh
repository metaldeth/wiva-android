#!/bin/sh
set -e

KEYSTORE_PATH="${KEYSTORE_PATH:-/app/signing/release.jks}"

missing=""
[ -z "${STORE_PASSWORD:-}" ] && missing="$missing STORE_PASSWORD"
[ -z "${KEY_ALIAS:-}" ] && missing="$missing KEY_ALIAS"
[ -z "${KEY_PASSWORD:-}" ] && missing="$missing KEY_PASSWORD"

if [ -n "$missing" ]; then
  echo "ERROR: Missing release signing env vars:$missing" >&2
  echo "Set STORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD before running this script." >&2
  exit 1
fi

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "ERROR: Keystore not found: $KEYSTORE_PATH" >&2
  exit 1
fi

cd /app

if [ -x /opt/gradle/bin/gradle ]; then
  GRADLE_BIN=/opt/gradle/bin/gradle
else
  chmod +x gradlew
  GRADLE_BIN=./gradlew
fi

$GRADLE_BIN assembleRelease --no-daemon --max-workers=2 \
  -Pandroid.injected.signing.store.file="$KEYSTORE_PATH" \
  -Pandroid.injected.signing.store.password="$STORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias="$KEY_ALIAS" \
  -Pandroid.injected.signing.key.password="$KEY_PASSWORD"

mkdir -p /app/release
cp /app/app/build/outputs/apk/release/*.apk /app/release/
echo "APK copied to release/:"
ls -lh /app/release/*.apk

if [ -n "${CI_HOST_UID:-}" ] && [ -n "${CI_HOST_GID:-}" ] && [ "$(id -u)" = 0 ]; then
  echo "chown build outputs to ${CI_HOST_UID}:${CI_HOST_GID}"
  for d in /app/app/build /app/release "/app/.gradle-docker"; do
    [ -e "$d" ] && chown -R "${CI_HOST_UID}:${CI_HOST_GID}" "$d" || true
  done
  [ -d /app/.gradle ] && chown -R "${CI_HOST_UID}:${CI_HOST_GID}" /app/.gradle || true
fi
