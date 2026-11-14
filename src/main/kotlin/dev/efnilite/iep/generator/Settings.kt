package dev.efnilite.iep.generator

import dev.efnilite.iep.IEP
import dev.efnilite.iep.style.Style

/**
 * All settings.
 */
data class Settings(val locale: String,
                    val style: String,
                    val radius: Int,
                    val time: Int,
                    val seed: Int,
                    val fall: Boolean,
                    val metric: Boolean,
                    val info: Boolean,
                    val rewards: MutableSet<Int>) {

    constructor(settings: Settings,
                locale: String = settings.locale,
                style: String = settings.style,
                radius: Int = settings.radius,
                time: Int = settings.time,
                seed: Int = settings.seed,
                fall: Boolean = settings.fall,
                metric: Boolean = settings.metric,
                info: Boolean = settings.info,
                rewards: MutableSet<Int> = settings.rewards) :

            this(locale = locale,
                style = style,
                radius = radius,
                time = time,
                seed = seed,
                fall = fall,
                metric = metric,
                info = info,
                rewards = rewards)

    companion object {

        fun String.asStyle(): Style {
            return IEP.getStyle(this)
        }

        /**
         * Repairs settings read from storage. Gson bypasses Kotlin's null checks, so an old or
         * hand-edited file can produce null fields or out-of-range values; those are replaced
         * with the matching value from [defaults].
         */
        @Suppress("USELESS_CAST", "SENSELESS_COMPARISON")
        fun sanitized(loaded: Settings, defaults: Settings): Settings {
            return Settings(
                locale = (loaded.locale as String?) ?: defaults.locale,
                style = (loaded.style as String?) ?: defaults.style,
                radius = if (loaded.radius in 3..6) loaded.radius else defaults.radius,
                time = if (loaded.time in 0..<24000) loaded.time else defaults.time,
                seed = if (loaded.seed in -1..1_000_000) loaded.seed else defaults.seed,
                fall = loaded.fall,
                metric = loaded.metric,
                info = loaded.info,
                rewards = (loaded.rewards as MutableSet<Int>?)?.toMutableSet() ?: mutableSetOf()
            )
        }
    }
}