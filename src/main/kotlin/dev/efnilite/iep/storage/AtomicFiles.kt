package dev.efnilite.iep.storage

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Crash-safe file writes for IEP's JSON/YAML data.
 *
 * Content is written to a temp file in the target's directory and then moved over the
 * target, so a reader (or a crash mid-write) only ever sees the old or the new file —
 * never a truncated one. Writes are serialized so two async saves of the same file can't
 * interleave.
 */
object AtomicFiles {

    private val lock = Any()

    @Throws(IOException::class)
    fun write(file: File, content: String) {
        synchronized(lock) {
            val target = file.absoluteFile
            val dir = target.parentFile
            dir.mkdirs()

            val tmp = Files.createTempFile(dir.toPath(), target.name, ".tmp")
            try {
                Files.writeString(tmp, content, StandardCharsets.UTF_8)
                try {
                    Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(tmp)
            }
        }
    }

    /**
     * Copies [file] next to itself as `<name>.corrupt-<millis>` so it can be inspected or
     * restored manually.
     * @return The backup file, or null if nothing was backed up.
     */
    fun backupCorrupt(file: File): File? {
        if (!file.exists()) return null

        val backup = File(file.absoluteFile.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}")
        return try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            backup
        } catch (_: IOException) {
            null
        }
    }
}
