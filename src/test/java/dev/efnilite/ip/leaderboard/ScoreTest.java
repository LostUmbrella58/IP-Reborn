package dev.efnilite.ip.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {

    @Test
    void formatsShortRuns() {
        assertEquals("00:00:000", Score.formatTime(0));
        assertEquals("01:02:345", Score.formatTime(62_345));
    }

    @Test
    void doesNotWrapAfterAnHour() {
        long millis = 65 * 60_000L + 7_008;

        assertEquals("65:07:008", Score.formatTime(millis));
        assertEquals(millis, new Score("p", Score.formatTime(millis), "0.5", 1).getTimeMillis());
    }

    @Test
    void negativeDurationsAreClamped() {
        assertEquals("00:00:000", Score.formatTime(-5));
    }

    @Test
    void malformedTimeSortsLast() {
        assertEquals(Integer.MAX_VALUE, new Score("p", "?", "?", 0).getTimeMillis());
        assertEquals(Integer.MAX_VALUE, new Score("p", "1:2", "?", 0).getTimeMillis());
    }

    @Test
    void roundTripsThroughString() {
        Score score = new Score("Efnilite", "01:02:003", "0.5", 42);

        assertEquals(score, Score.fromString(score.toString()));
    }
}
