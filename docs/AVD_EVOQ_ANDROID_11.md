# AVD `evoq_android_11` — runbook

Профиль для разработки/ smoke под **Android 11 / API 30**, близкий к офисной плате Evoq (`k3568_a`).

## Environment

| Variable | Value |
|----------|-------|
| `ANDROID_AVD_HOME` | `F:\AndroidAVD` |
| `ANDROID_SDK_ROOT` / `ANDROID_HOME` | `F:\AndroidSDK` (или из `local.properties` → `sdk.dir`) |

## AVD profile

| Parameter | Value |
|-----------|-------|
| Name | **`evoq_android_11`** |
| API | **30** (Android 11) |
| ABI | **x86_64** (emulator) |
| Screen | **1080×1920**, portrait, **160 dpi** |
| ADB serial (typical) | **`emulator-5556`** |

## Physical board reference (not an emulator)

| Parameter | Value |
|-----------|-------|
| ADB target | `192.168.50.163:5555` |
| Device | `k3568_a` |
| OS | Android 11 / API 30 |
| ABI | **arm64** |
| Screen | 1080×1920 @ 160 dpi |
| RAM | 2 GB |

Для платы используйте **arm64** APK (`assembleDebug` / `installDebug` без x86-only ограничений).

## Launch (PowerShell)

```powershell
$env:ANDROID_AVD_HOME = 'F:\AndroidAVD'
$env:ANDROID_SDK_ROOT = 'F:\AndroidSDK'
& "$env:ANDROID_SDK_ROOT\emulator\emulator.exe" -avd evoq_android_11
```

Дождаться boot → `adb devices` (ожидается `emulator-5556`).

## Install debug (Windows)

```bat
set ANDROID_HOME=F:\AndroidSDK
gradlew.bat installDebug
%ANDROID_HOME%\platform-tools\adb.exe -s emulator-5556 shell am start -n com.wiva.android/.ui.MainActivity
```

Enrollment key — в gitignored `local.properties` (`telemetry.enrollmentKey`) или env `WIVA_TELEMETRY_ENROLLMENT_KEY`; см. `docs/SIMPLE_TELEMETRY_MVP_ANDROID.md`.

## Related

- Эталонный release-smoke AVD **`wiva`** (API 25, 768×1024): `AGENTS.md`
- Telemetry MVP client: `docs/SIMPLE_TELEMETRY_MVP_ANDROID.md`
- Production server: `c:\wiva\wiva-telemetry\docs\deployment\server.md`
