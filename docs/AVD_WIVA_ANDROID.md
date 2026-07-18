# AVD `wiva-android` — runbook

Основной профиль проекта Wiva Android: экран исходного AVD `evoq`, Android 11
как на офисной плате (`k3568_a`) и обязательная landscape-ориентация.

## Environment

| Variable | Value |
|----------|-------|
| `ANDROID_AVD_HOME` | `F:\AndroidAVD` |
| `ANDROID_SDK_ROOT` / `ANDROID_HOME` | `F:\AndroidSDK` (или `local.properties` → `sdk.dir`) |

## AVD profile

| Parameter | Value |
|-----------|-------|
| Name | **`wiva-android`** |
| API | **30** (Android 11) |
| ABI | **x86_64** |
| Screen | physical **768×1024**, **120 dpi**; landscape logical **1024×768** |
| Orientation | **landscape** (`hw.initialOrientation=landscape`) |
| RAM / CPU | **2048 MB / 4 cores** |
| ADB serial (typical) | **`emulator-5556`** |

## Physical board reference

| Parameter | Value |
|-----------|-------|
| ADB target | `192.168.50.163:5555` |
| Device | `k3568_a` |
| OS | Android 11 / API 30 |
| ABI | **arm64** |
| Screen | 1080×1920 @ 160 dpi |
| RAM | 2 GB |

## Launch (PowerShell)

```powershell
$env:ANDROID_AVD_HOME = 'F:\AndroidAVD'
$env:ANDROID_SDK_ROOT = 'F:\AndroidSDK'
& "$env:ANDROID_SDK_ROOT\emulator\emulator.exe" -avd wiva-android -port 5556
```

После загрузки ожидается `emulator-5556`. Проверка:

```powershell
adb -s emulator-5556 shell getprop ro.build.version.release
adb -s emulator-5556 shell wm size
adb -s emulator-5556 shell wm density
```

## Install and run

```bat
set ANDROID_HOME=F:\AndroidSDK
gradlew.bat installDebug
%ANDROID_HOME%\platform-tools\adb.exe -s emulator-5556 shell am start -n com.wiva.android/.ui.MainActivity
```

Enrollment key хранится только в gitignored `local.properties`
(`telemetry.enrollmentKey`) или `WIVA_TELEMETRY_ENROLLMENT_KEY`; см.
`docs/SIMPLE_TELEMETRY_MVP_ANDROID.md`.

## Related

- Telemetry MVP: `docs/SIMPLE_TELEMETRY_MVP_ANDROID.md`
- Production server: `c:\wiva\wiva-telemetry\docs\deployment\server.md`
- Legacy AVD `wiva` (API 25) используется только для проверки `minSdk`
