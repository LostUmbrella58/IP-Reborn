package dev.efnilite.iep.storage

import dev.efnilite.iep.IEP
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.leaderboard.Leaderboard
import java.io.File
import java.io.IOException
import java.util.*

internal object DiskStorage {

    fun init(uuid: UUID) {
        val file = getPlayerFile(uuid)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun init(leaderboard: Leaderboard) {
        val file = getLeaderboardFile(leaderboard.name)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun save(uuid: UUID, settings: Settings) {
        AtomicFiles.write(getPlayerFile(uuid), IEP.GSON.toJson(settings))
    }

    /**
     * @return The stored settings, or null if there are none or the file is unreadable
     * (a corrupt file is backed up and the caller falls back to defaults).
     */
    fun load(uuid: UUID): Settings? {
        val file = getPlayerFile(uuid)
        if (!file.exists()) return null

        return try {
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) null else IEP.GSON.fromJson(text, Settings::class.java)
        } catch (ex: Exception) {
            val backup = AtomicFiles.backupCorrupt(file)
            IEP.logging.error("Settings file ${file.name} is unreadable (${ex.message}); using default settings. " +
                    "Backup: ${backup?.name ?: "none"}")
            null
        }
    }

    fun save(leaderboard: Leaderboard) {
        AtomicFiles.write(getLeaderboardFile(leaderboard.name), IEP.GSON.toJson(leaderboard.snapshot()))
    }

    /**
     * Loads stored scores into [leaderboard]. If the file can't be parsed it is backed up and
     * the leaderboard starts empty; if even the backup fails this throws, which keeps the
     * leaderboard marked as not loaded so it is never saved over the original file.
     */
    fun load(leaderboard: Leaderboard) {
        val file = getLeaderboardFile(leaderboard.name)
        if (!file.exists()) return

        val parsed = try {
            Leaderboard.parse(file.readText(Charsets.UTF_8))
        } catch (ex: Exception) {
            val backup = AtomicFiles.backupCorrupt(file)
                ?: throw IOException("Leaderboard file ${file.name} is corrupt and could not be backed up", ex)

            IEP.logging.error("Leaderboard file ${file.name} is corrupt (${ex.message}). It was backed up to " +
                    "${backup.name}; the leaderboard starts empty. Restore the backup manually if needed.")
            return
        }

        for ((uuid, score) in parsed) {
            leaderboard.update(uuid, score)
        }
    }

    private fun getPlayerFile(uuid: UUID) = File(IEP.dataFolder, "players/$uuid.json")
    private fun getLeaderboardFile(name: String) = File(IEP.dataFolder, "leaderboards/$name.json")
}
