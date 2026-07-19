package com.viwa.android.domain.model.customer

/**
 * Вариант анимации привлечения внимания к основной кнопке («Налить воду» / оплата).
 * Ключ хранения: `primaryButtonPulseStyle` в JsonStore.
 */
enum class PrimaryButtonPulseStyle(
    val storageKey: String,
    val label: String,
    val description: String,
) {
    PulseScale(
        storageKey = "pulse_scale",
        label = "Пульс масштаба",
        description = "Плавное увеличение и уменьшение кнопки.",
    ),
    BreathingGlow(
        storageKey = "breathing_glow",
        label = "Дыхание свечения",
        description = "Мягкая смена яркости подсветки по контуру.",
    ),
    Shimmer(
        storageKey = "shimmer",
        label = "Перелив",
        description = "Бегущий блик по верхней кромке.",
    ),
    Bounce(
        storageKey = "bounce",
        label = "Пружина",
        description = "Короткие упругие «подпрыгивания» масштаба.",
    ),
    Wave(
        storageKey = "wave",
        label = "Волна",
        description = "Лёгкое покачивание масштабом и смещением.",
    ),
    ;

    companion object {
        fun fromStorage(raw: String?): PrimaryButtonPulseStyle {
            if (raw.isNullOrBlank()) return PulseScale
            return entries.firstOrNull { it.storageKey == raw.trim() } ?: PulseScale
        }
    }
}
