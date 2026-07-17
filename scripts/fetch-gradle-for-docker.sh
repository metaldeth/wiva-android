#!/bin/sh
# Скачивает gradle-*-bin.zip на хост (shell runner) в gradle-docker-prebuild/ перед docker build.
# Сеть хоста часто стабильнее, чем wget внутри docker build.
set -e
VERSION="${GRADLE_VERSION:-8.11.1}"
DIR="${GRADLE_PREBUILD_DIR:-gradle-docker-prebuild}"
OUT="${DIR}/gradle-${VERSION}-bin.zip"
mkdir -p "$DIR"

if [ -n "${WIVA_GRADLE_CACHE_FILE:-}" ] && [ -r "${WIVA_GRADLE_CACHE_FILE}" ]; then
  echo "fetch-gradle: copy from WIVA_GRADLE_CACHE_FILE"
  cp -f "${WIVA_GRADLE_CACHE_FILE}" "$OUT"
  test -s "$OUT"
  ls -lh "$OUT"
  exit 0
fi

if [ -s "$OUT" ]; then
  echo "fetch-gradle: already exists $OUT"
  ls -lh "$OUT"
  exit 0
fi

download_one() {
  url="$1"
  echo "fetch-gradle: trying $url"
  rm -f "$OUT"
  if command -v wget >/dev/null 2>&1; then
    if wget -4 --timeout=300 -t 2 -O "$OUT" "$url" 2>/dev/null && [ -s "$OUT" ]; then
      return 0
    fi
  fi
  rm -f "$OUT"
  if command -v curl >/dev/null 2>&1; then
    if curl -4 -fL --connect-timeout 30 --max-time 900 -o "$OUT" "$url" 2>/dev/null && [ -s "$OUT" ]; then
      return 0
    fi
  fi
  rm -f "$OUT"
  return 1
}

if [ -n "${WIVA_GRADLE_DIST_URL:-}" ]; then
  download_one "$WIVA_GRADLE_DIST_URL" || {
    echo "fetch-gradle: WIVA_GRADLE_DIST_URL failed" >&2
    exit 1
  }
  echo "fetch-gradle: ok (WIVA_GRADLE_DIST_URL)"
  ls -lh "$OUT"
  exit 0
fi

for url in \
  "https://mirrors.aliyun.com/gradle/gradle-${VERSION}-bin.zip" \
  "https://mirrors.cloud.tencent.com/gradle/gradle-${VERSION}-bin.zip" \
  "https://repo.huaweicloud.com/gradle/gradle-${VERSION}-bin.zip" \
  "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip"
do
  if download_one "$url"; then
    echo "fetch-gradle: ok"
    ls -lh "$OUT"
    exit 0
  fi
done

echo "fetch-gradle: all URLs failed; put zip on server and set WIVA_GRADLE_CACHE_FILE or WIVA_GRADLE_DIST_URL" >&2
exit 1
