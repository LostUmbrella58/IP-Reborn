import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class LeaderboardDataTest {

    @Test
    fun testIsBetter() {
        val base = Score("a", 100.0, 5000, 1)

        assertTrue(Leaderboard.isBetter(base, null))
        assertTrue(Leaderboard.isBetter(Score("a", 101.0, 9000, 1), base))
        assertFalse(Leaderboard.isBetter(Score("a", 99.0, 1000, 1), base))
        // same score: faster or equal time wins, slower loses
        assertTrue(Leaderboard.isBetter(Score("a", 100.0, 4000, 1), base))
        assertTrue(Leaderboard.isBetter(Score("a", 100.0, 5000, 1), base))
        assertFalse(Leaderboard.isBetter(Score("a", 100.0, 6000, 1), base))
    }

    @Test
    fun testParseValid() {
        val uuid = UUID.randomUUID()
        val json = """{"$uuid":{"name":"Steve","score":12.5,"time":61000,"seed":42}}"""

        val parsed = Leaderboard.parse(json)

        assertEquals(1, parsed.size)
        assertEquals(Score("Steve", 12.5, 61000, 42), parsed[uuid])
    }

    @Test
    fun testParseSkipsBadEntries() {
        val good = UUID.randomUUID()
        val noName = UUID.randomUUID()
        val json = """{"$good":{"name":"Alex","score":1.0,"time":1,"seed":0},
            |"not-a-uuid":{"name":"Bob","score":2.0,"time":2,"seed":0},
            |"$noName":{"score":3.0,"time":3,"seed":0}}""".trimMargin()

        val parsed = Leaderboard.parse(json)

        assertEquals(setOf(good), parsed.keys)
    }

    @Test
    fun testParseBlankIsEmpty() {
        assertTrue(Leaderboard.parse("").isEmpty())
        assertTrue(Leaderboard.parse("   \n").isEmpty())
    }

    @Test
    fun testParseTruncatedThrows() {
        val uuid = UUID.randomUUID()

        assertThrows<Exception> { Leaderboard.parse("""{"$uuid":{"name":"Steve","sco""") }
    }
}
