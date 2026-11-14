import dev.efnilite.iep.storage.AtomicFiles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AtomicFilesTest {

    @TempDir
    lateinit var dir: File

    @Test
    fun testWriteCreatesAndReplaces() {
        val file = File(dir, "nested/data.json")

        AtomicFiles.write(file, "first, longer content")
        assertEquals("first, longer content", file.readText())

        AtomicFiles.write(file, "second")
        assertEquals("second", file.readText())

        // no temp files left behind
        assertEquals(listOf("data.json"), file.parentFile.list()!!.toList())
    }

    @Test
    fun testBackupCorrupt() {
        val file = File(dir, "board.json")
        file.writeText("{broken")

        val backup = AtomicFiles.backupCorrupt(file)

        assertNotNull(backup)
        assertEquals("{broken", backup!!.readText())
        assertEquals("{broken", file.readText())
        assertNull(AtomicFiles.backupCorrupt(File(dir, "missing.json")))
    }
}
