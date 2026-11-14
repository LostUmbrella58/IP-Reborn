package dev.efnilite.iep.leaderboard

import dev.efnilite.iep.config.Config
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Score(val name: String, val score: Double, val time: Long, val seed: Int) {

    /**
     * @return The time in a formatted manner.
     */
    fun getFormattedTime(): String {
        return timeFormatter.format(Instant.ofEpochMilli(time))
    }

    companion object {

        // Lazy so constructing a Score (e.g. while parsing a leaderboard file) doesn't
        // require the config to be loaded.
        val timeFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern(Config.CONFIG.getString("time-format"))
                .withZone(ZoneOffset.UTC)
        }

        fun Double.pretty(digits: Int = 1) = "%.${digits}f".format(this)

    }
}