# Итог: G2 — калибровка сиропов (stage-g2-syrup-calibration)

## Статус

Реализовано по DoD: тестовый налив (ServiceCommand 0x52, тело 0x09…), пересчёт и сохранение `conversionFactor` в `TELEMETRY_MERGED_INVENTORY`, отправка обоих топиков телеметрии (или предупреждение в лог при offline), вкладка «Обслуживание → Калибровка сиропов», мок 0x09 с логом и ACK, unit-тесты.

## Ключевые файлы

| Назначение | Путь |
|------------|------|
| Чистая математика и тело команды | `app/src/main/java/com/wiva/android/services/calibration/SyrupCalibrationMath.kt` |
| Сервис налива и сохранения | `app/src/main/java/com/wiva/android/services/calibration/SyrupCalibrationService.kt` |
| Телеметрия import | `app/src/main/java/com/wiva/android/services/telemetry/ViwaTelemetryService.kt` (`sendCellVolumeImportFromConfig`, `sendCellStoreImportFromConfig`) |
| Сборка тела cellStoreImport | `app/src/main/java/com/wiva/android/services/telemetry/CellStoreImportBodyBuilder.kt` |
| categoryConfigMachine | `app/src/main/java/com/wiva/android/domain/telemetry/CategoryConfigMachineBuilder.kt` |
| Конфиг контейнеров | `MachineInventoryRepository` + `MachineInventoryRepositoryImpl` (`listContainersForCalibration`, `updateContainerConversionFactor`) |
| UI | `app/src/main/java/com/wiva/android/ui/screens/service/tabs/ViwaSyrupCalibrationTab.kt`, `ServiceViewModel`, `ViwaServiceMenuTabContent.kt` |
| Мок 0x09 | `app/src/main/java/com/wiva/android/hardware/controller/ControllerConnection.kt` |
| Тесты | `app/src/test/java/com/wiva/android/services/calibration/SyrupCalibrationMathTest.kt` |

## Сборка и тесты

- `gradlew.bat assembleDebug` — OK  
- `gradlew.bat :app:testDebugUnitTest` — OK  

## Версия APK

`versionName` / `versionCode` обновлены в `app/build.gradle.kts` (26.04.07.02).

## Рекомендации

- При появлении интеграционных тестов телеметрии — добавить проверку JSON `cellVolumeImportTopic` (ровно 6 ячеек) и структуры `cellStoreImportTopic` по снимку с electron.
