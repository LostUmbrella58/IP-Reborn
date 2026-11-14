package dev.efnilite.iep.leaderboard

import dev.efnilite.iep.IEP
import dev.efnilite.iep.storage.Storage
import com.google.gson.reflect.TypeToken
import dev.efnilite.ip.foundation.util.Task
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a mode's leaderboard
 * @param name The name of the leaderboard
 * @param minScore The minimum score required to be on the leaderboard
 */
data class Leaderboard(
    val name: String,
    var minScore: Double = 0.0,
    var sort: Sort = Sort.SCORE
) {

    // Concurrent: written on the main thread (update/reset) and by the async loader,
    // read by async saves and PlaceholderAPI requests.
    val data: MutableMap<UUID, Score> = ConcurrentHashMap()

    /**
     * True once the stored data has been read. Until then (or forever, if reading failed)
     * saving is refused so a partial in-memory map can never overwrite the stored history.
     */
    @Volatile
    var loaded = false
        private set

    init {
        require(minScore >= 0) { "Minimum score must be greater than or equal to 0" }

        load()
    }

    /**
     * Asynchronously reads the leaderboard.
     */
    private fun load() {
        Task.create(IEP.instance)
            .async()
            .execute {
                try {
                    Storage.init(this)
                    Storage.load(this)
                    loaded = true
                } catch (t: Throwable) {
                    IEP.logging.stack("Failed to load leaderboard $name; it will not be saved this session " +
                            "so the stored scores are not overwritten", t)
                }
            }
            .run()
    }

    /**
     * @return A point-in-time copy of all scores, safe to serialize off the main thread.
     */
    fun snapshot(): Map<UUID, Score> = HashMap(data)

    /**
     * Asynchronously saves the leaderboard.
     */
    fun save() {
        if (!loaded) {
            IEP.logging.warn("Not saving leaderboard $name because it was not (successfully) loaded")
            return
        }

        if (IEP.stopping) {
            Storage.save(this)
            return
        }

        Task.create(IEP.instance)
            .async()
            .execute { Storage.save(this) }
            .run()
    }

    /**
     * Updates the leaderboard.
     * If this player has a score with a higher score and lower time, it will not be updated.
     * @param uuid The player's UUID
     * @param score The player's score
     */
    fun update(uuid: UUID, score: Score) {
        data.compute(uuid) { _, existing -> if (isBetter(score, existing)) score else existing }
    }

    /**
     * Resets the score of the specified player.
     *
     * @param uuid The player's UUID
     */
    fun reset(uuid: UUID) {
        data.remove(uuid)
    }

    /**
     * Returns the score instance of the specified player.
     * @param uuid The player's UUID
     * @return The score instance of this player, else an empty score
     */
    fun getScore(uuid: UUID): Score {
        return data.getOrDefault(uuid, EMPTY_SCORE)
    }

    /**
     * Returns the score instance at the specified rank.
     * This rank is guaranteed to be above the minimum score for this leaderboard.
     * @param rank The rank
     * @return The score instance at this rank, else an empty score
     */
    fun getRank(rank: Int): Score {
        val scores = sort.sort(data)
            .map { it.value }
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }

        if (rank < 1 || rank > scores.size) return EMPTY_SCORE

        return scores[rank - 1]
    }

    /**
     * Returns all scores on the leaderboard. Not guaranteed to be sorted.
     * @return All scores on the leaderboard that are above the minimum score.
     */
    fun getAllScores() = data
        .filter { it.value.score >= minScore }
        .toSortedMap()

    companion object {
        private val EMPTY_SCORE = Score("?", 0.0, 0, 0)

        /**
         * @return True if [candidate] should replace [existing]: a higher score, or the same
         * score in an equal or shorter time.
         */
        fun isBetter(candidate: Score, existing: Score?): Boolean {
            if (existing == null) return true
            if (existing.score > candidate.score) return false
            if (existing.score == candidate.score && existing.time < candidate.time) return false
            return true
        }

        private val FILE_TYPE: Type = object : TypeToken<Map<String, Score>>() {}.type

        /**
         * Parses a leaderboard file's contents. Entries with an invalid UUID or missing name are
         * skipped; malformed JSON throws.
         */
        fun parse(json: String): Map<UUID, Score> {
            if (json.isBlank()) return emptyMap()

            val raw: Map<String, Score?> = IEP.GSON.fromJson(json, FILE_TYPE) ?: return emptyMap()
            val result = mutableMapOf<UUID, Score>()

            for ((key, score) in raw) {
                // Gson bypasses Kotlin's null checks, so a missing name arrives as null.
                if (score == null || (score.name as String?) == null) continue

                val uuid = try {
                    UUID.fromString(key)
                } catch (_: IllegalArgumentException) {
                    continue
                }

                result[uuid] = score
            }

            return result
        }
    }

    /**
     * Represents the sorting method of the leaderboard.
     */
    enum class Sort {

        SCORE {
            override fun sort(scores: Map<UUID, Score>): List<Map.Entry<UUID, Score>> {
                return scores.entries.sortedWith(compareBy({ -it.value.score }, { -it.value.time }))
            }
        },
        TIME {
            override fun sort(scores: Map<UUID, Score>): List<Map.Entry<UUID, Score>> {
                return scores.entries.sortedWith(compareBy { it.value.time })
            }
        };

        abstract fun sort(scores: Map<UUID, Score>): List<Map.Entry<UUID, Score>>

    }
}