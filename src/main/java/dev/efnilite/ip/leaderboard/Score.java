package dev.efnilite.ip.leaderboard;

/**
 * Represents a record, used to keep track of the score a player may achieve.
 *
 * @param name       The name of the player
 * @param score      The score achieved
 * @param time       The time it took to achieve this score
 * @param difficulty The difficulty of this run
 */
public record Score(String name, String time, String difficulty, int score) {

    /**
     * Gets a {@link Score} instance from a string
     *
     * @param string The string
     * @return a {@link Score} instance based off the provided string
     */
    public static Score fromString(String string) {
        String[] parts = string.split(",");

        return new Score(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
    }

    /**
     * Formats a duration in the detailed {@code mm:ss:SSS} format used for stored scores.
     * Minutes are not capped at 59, so runs longer than an hour don't wrap around (e.g. {@code 65:00:000}).
     *
     * @param millis The duration in milliseconds.
     * @return The formatted time.
     */
    public static String formatTime(long millis) {
        millis = Math.max(0, millis);

        long minutes = millis / 60_000;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;

        return "%02d:%02d:%03d".formatted(minutes, seconds, ms);
    }

    /**
     * @return This score's time in millis. {@link Integer#MAX_VALUE} if the time can't be parsed.
     */
    public int getTimeMillis() {
        try {
            String[] split = time.split(":");

            int m = Integer.parseInt(split[0]);
            int s = Integer.parseInt(split[1]);
            int ms = Integer.parseInt(split[2]);

            return m * 60 * 1000 + s * 1000 + ms;
        } catch (RuntimeException ex) { // malformed or placeholder ("?") time
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s", name, time, difficulty, score);
    }
}
