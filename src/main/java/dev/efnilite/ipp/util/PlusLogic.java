package dev.efnilite.ipp.util;

import dev.efnilite.ip.leaderboard.Score;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Pure helper logic for the IP+ modes. Kept free of server state so it can be unit tested.
 */
public final class PlusLogic {

    private static final Set<String> REWRITTEN_SUBCOMMANDS = Set.of("create", "lobbies", "invite");

    private PlusLogic() {
    }

    /**
     * @param used The slots that are currently taken.
     * @return The lowest slot index (starting at 0) that is not in use.
     */
    public static int lowestFreeSlot(Collection<Integer> used) {
        int slot = 0;
        while (used.contains(slot)) {
            slot++;
        }
        return slot;
    }

    /**
     * @param goal     The goal score.
     * @param segments The maximum amount of bar segments.
     * @return The amount of score per bar segment. Always at least 1.
     */
    public static int barInterval(int goal, int segments) {
        if (goal <= 0 || segments <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(goal / (double) segments));
    }

    /**
     * @param value    The current value.
     * @param interval The interval per segment, see {@link #barInterval(int, int)}.
     * @param length   The amount of available gradient colours.
     * @return A gradient index clamped to {@code [0, length - 1]}.
     */
    public static int gradientIndex(int value, int interval, int length) {
        return Math.max(0, Math.min(value / Math.max(1, interval), length - 1));
    }

    /**
     * Whether a newly achieved time should replace the previous leaderboard entry.
     *
     * @param newTime  The new time in {@code mm:ss:SSS} format.
     * @param previous The previous score, or null if there is none.
     * @return True if there is no (valid) previous time or the new time is strictly faster.
     */
    public static boolean isBetterTime(String newTime, @Nullable Score previous) {
        if (previous == null) {
            return true;
        }

        int previousMillis;
        try {
            previousMillis = previous.getTimeMillis();
        } catch (RuntimeException ex) { // malformed previous entry, replace it
            return true;
        }

        try {
            return new Score("", newTime, "", 0).getTimeMillis() < previousMillis;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Rewrites the former IPPlus {@code /parkour create|lobbies|invite} commands to {@code /ipp ...}.
     *
     * @param message The full command message, including the leading slash.
     * @return The rewritten message, or null if this message should not be rewritten.
     */
    public static @Nullable String rewriteParkourCommand(String message) {
        if (message == null || !message.startsWith("/")) {
            return null;
        }

        String[] parts = message.substring(1).trim().split("\\s+", 3);
        if (parts.length < 2 || !(parts[0].equalsIgnoreCase("parkour") || parts[0].equalsIgnoreCase("ip:parkour"))) {
            return null;
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (!REWRITTEN_SUBCOMMANDS.contains(sub)) {
            return null;
        }

        return parts.length == 3 ? "/ipp %s %s".formatted(sub, parts[2]) : "/ipp " + sub;
    }
}
