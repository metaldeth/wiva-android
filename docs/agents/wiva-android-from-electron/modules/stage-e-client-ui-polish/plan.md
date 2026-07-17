# План (краткий)

1. Зафиксировать соответствие экранов electron → Kotlin/Compose (таблица в `summary.md`).
2. Токены цветов/фона как в SCSS wiva (`WivaCustomerUiTokens.kt`).
3. `DrinkListScreen`: фон+виньет, шапка как `HeaderAction`, горизонтальные карточки как `DrinkCard`, primary bar как `PrimaryActionButton`, подсказка «Выберите напиток», заглушка промо, `LazyRow` + ключи.
4. `CustomerPaymentSheet` + доработка `DrinkListViewModel`: платный режим → диалог выбора СБП/карта → существующий `PaymentTerminalService` + мок Pax.
5. `PreparingScreen`: панель как `PreparingPage`/`PreparingProgressView`, детерминированный круг прогресса.
6. Сборки `assembleDebug`, `assembleRelease`; версия `versionName` в `app/build.gradle.kts`.
7. Обновить `CHECKLIST_WIVA_ANDROID_STAGES.md` (DoD E3+ polish).
