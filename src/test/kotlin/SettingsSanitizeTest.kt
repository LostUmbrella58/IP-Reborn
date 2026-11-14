import com.google.gson.Gson
import dev.efnilite.iep.generator.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsSanitizeTest {

    private val defaults = Settings("en", "cherry", 5, 0, -1, true, true, false, mutableSetOf())

    @Test
    fun testMissingFieldsFallBackToDefaults() {
        // Gson leaves absent fields null/0 even though Kotlin declares them non-null.
        val loaded = Gson().fromJson("""{"time":6000,"info":true}""", Settings::class.java)

        val result = Settings.sanitized(loaded, defaults)

        assertEquals("en", result.locale)
        assertEquals("cherry", result.style)
        assertEquals(5, result.radius) // 0 is out of range
        assertEquals(6000, result.time)
        assertTrue(result.info)
        assertTrue(result.rewards.isEmpty())
    }

    @Test
    fun testValidSettingsAreKept() {
        val loaded = Settings("nl", "ocean", 3, 18000, 1234, false, false, true, mutableSetOf(5000))

        assertEquals(loaded, Settings.sanitized(loaded, defaults))
    }

    @Test
    fun testOutOfRangeValuesAreReplaced() {
        val loaded = Settings("nl", "ocean", 99, 30000, -5, false, false, true, mutableSetOf())

        val result = Settings.sanitized(loaded, defaults)

        assertEquals(5, result.radius)
        assertEquals(0, result.time)
        assertEquals(-1, result.seed)
    }
}
