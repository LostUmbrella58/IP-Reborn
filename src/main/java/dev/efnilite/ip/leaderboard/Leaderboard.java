package dev.efnilite.ip.leaderboard;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.foundation.util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class for handling leaderboards.
 * <p>
 * Thread-safety: the score map is copy-on-write. Every change builds a new, sorted, unmodifiable map
 * and swaps it in, so readers (menus, placeholders, async writes) always iterate a consistent snapshot.
 * All storage I/O for a leaderboard is serialized, so a synchronous write (e.g. on shutdown) waits for
 * an in-flight async read/write instead of racing it.
 */
public class Leaderboard {

    /**
     * The mode that this leaderboard belongs to
     */
    public final String mode;

    /**
     * The way in which items will be sorted.
     */
    public final Sort sort;

    /**
     * Sorted, unmodifiable snapshot of all scores for this mode. Replaced on every change.
     */
    private volatile Map<UUID, Score> scores = Collections.emptyMap();

    /**
     * Guards changes to {@link #scores}, {@link #unsaved} and {@link #removed}.
     */
    private final Object lock = new Object();

    /**
     * Serializes storage I/O of this leaderboard.
     */
    private final Object ioLock = new Object();

    /**
     * Scores that have been put since the last successful write.
     */
    private final Map<UUID, Score> unsaved = new HashMap<>();

    /**
     * Scores that have been removed since the last successful write.
     */
    private final Set<UUID> removed = new HashSet<>();

    public Leaderboard(@NotNull String mode, Sort sort) {
        this(mode, sort, true);
    }

    /**
     * @param connect False to skip storage initialisation and the periodic task (used in tests).
     */
    Leaderboard(@NotNull String mode, Sort sort, boolean connect) {
        this.mode = mode.toLowerCase();
        this.sort = sort;

        if (!connect) {
            return;
        }

        Storage.init(this.mode);

        // read all data
        read(true);

        var interval = Config.CONFIG.getInt("storage-update-interval");

        // read/write all data every x seconds after x seconds to allow time for reading/writing
        Task.create(IP.getPlugin())
                .delay(interval * 20)
                .repeat(interval * 20)
                .async()
                .execute(Config.CONFIG.getBoolean("joining") ? () -> {
                    IP.log("Periodic saving of leaderboard data of %s".formatted(this.mode));

                    writeNow();
                } : () -> {
                    IP.log("Periodic reading of leaderboard data of %s".formatted(this.mode));

                    readNow();
                })
                .run();
    }

    /**
     * Writes all scores to the leaderboard file associated with this leaderboard.
     * When async is false, this blocks until any in-flight async read/write of this leaderboard is done.
     */
    public void write(boolean async) {
        IP.log("Saving leaderboard data of %s".formatted(mode));

        run(this::writeNow, async);
    }

    /**
     * Reads all scores from the leaderboard file
     */
    public void read(boolean async) {
        IP.log("Reading leaderboard data of %s".formatted(mode));

        run(this::readNow, async);
    }

    private void writeNow() {
        synchronized (ioLock) {
            WriteSnapshot snapshot = snapshot();

            if (Storage.writeScores(mode, snapshot.scores(), snapshot.removed(), snapshot.unsaved())) {
                markWritten(snapshot);
            }
        }
    }

    private void readNow() {
        synchronized (ioLock) {
            Map<UUID, Score> loaded = Storage.readScores(mode);

            if (loaded == null) {
                IP.logging().warn("Could not read leaderboard data of %s, keeping the scores currently in memory".formatted(mode));
                return;
            }

            applyRead(loaded);
        }
    }

    /**
     * Replaces the in-memory scores with the loaded ones, keeping local changes that have not been written yet.
     *
     * @param loaded The scores read from storage.
     */
    void applyRead(@NotNull Map<UUID, Score> loaded) {
        synchronized (lock) {
            Map<UUID, Score> merged = new HashMap<>(loaded);
            removed.forEach(merged::remove);
            merged.putAll(unsaved);

            scores = sortCopy(merged, sort);
        }
    }

    /**
     * @return A consistent snapshot of what should be written.
     */
    WriteSnapshot snapshot() {
        synchronized (lock) {
            return new WriteSnapshot(scores, Set.copyOf(removed), Map.copyOf(unsaved));
        }
    }

    /**
     * Marks the changes in the snapshot as written, keeping changes made after the snapshot was taken.
     */
    void markWritten(@NotNull WriteSnapshot snapshot) {
        synchronized (lock) {
            removed.removeAll(snapshot.removed());
            snapshot.unsaved().forEach(unsaved::remove);
        }
    }

    record WriteSnapshot(Map<UUID, Score> scores, Set<UUID> removed, Map<UUID, Score> unsaved) {
    }

    private void run(Runnable runnable, boolean async) {
        if (async) {
            Task.create(IP.getPlugin()).async().execute(runnable).run();
        } else {
            runnable.run();
        }
    }

    /**
     * Returns sorted copy of the score map.
     * @param sort The sorting method.
     * @return A sorted map of scores.
     */
    public Map<UUID, Score> sort(Sort sort) {
        return new LinkedHashMap<>(sortCopy(scores, sort));
    }

    private static Map<UUID, Score> sortCopy(Map<UUID, Score> source, Sort sort) {
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();

        source.entrySet().stream()
                .sorted((one, two) -> {
                    switch (sort) {
                        case SCORE -> {
                            int scoreComparison = Integer.compare(two.getValue().score(), one.getValue().score());

                            if (scoreComparison != 0) {
                                return scoreComparison;
                            } else {
                                return Integer.compare(one.getValue().getTimeMillis(), two.getValue().getTimeMillis());
                            }
                        }
                        case TIME -> {
                            return Integer.compare(one.getValue().getTimeMillis(), two.getValue().getTimeMillis());
                        }
                        case DIFFICULTY -> {
                            return Double.compare(parseDifficulty(two.getValue()), parseDifficulty(one.getValue()));
                        }
                        default -> throw new IllegalArgumentException("Invalid sort method");
                    }
                })
                .forEachOrdered(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return Collections.unmodifiableMap(sorted);
    }

    private static double parseDifficulty(Score score) {
        try {
            return Double.parseDouble(score.difficulty());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    /**
     * Registers a new score, overriding the old one
     *
     * @param uuid  The player's uuid
     * @param score The {@link Score} instance associated with a player's run
     * @return the previous score, if there was one
     */
    @Nullable
    public Score put(@NotNull UUID uuid, @NotNull Score score) {
        synchronized (lock) {
            Map<UUID, Score> copy = new HashMap<>(scores);
            Score previous = copy.put(uuid, score);

            unsaved.put(uuid, score);
            removed.remove(uuid);

            scores = sortCopy(copy, sort);

            return previous;
        }
    }

    /**
     * Resets the score of a player by deleting it from the internal map.
     * The deletion is propagated to storage on the next write.
     *
     * @param uuid The UUID
     * @return the previous value if one was found
     */
    @Nullable
    public Score remove(@NotNull UUID uuid) {
        synchronized (lock) {
            Map<UUID, Score> copy = new LinkedHashMap<>(scores);
            Score previous = copy.remove(uuid);

            unsaved.remove(uuid);
            removed.add(uuid);

            scores = Collections.unmodifiableMap(copy);

            return previous;
        }
    }

    /**
     * Resets all registered scores for this mode
     */
    public void resetAll() {
        synchronized (lock) {
            removed.addAll(scores.keySet());
            unsaved.clear();

            scores = Collections.emptyMap();
        }
    }

    /**
     * @return An unmodifiable, sorted snapshot of all scores.
     */
    @NotNull
    public Map<UUID, Score> getScores() {
        return scores;
    }

    /**
     * @param uuid The {@link UUID} to get.
     * @return The {@link Score} associated with the player. If null, returns a {@link Score} instance with "?".
     */
    @NotNull
    public Score get(@NotNull UUID uuid) {
        return scores.getOrDefault(uuid, new Score("?", "?", "?", 0));
    }

    /**
     * @param uuid The uuid
     * @return The rank. Starts from 1. Returns 0 if no ranking is found.
     */
    public int getRank(@NotNull UUID uuid) {
        return new ArrayList<>(scores.keySet()).indexOf(uuid) + 1;
    }

    /**
     * Gets the score at a specified rank.
     * Ranks start at 1.
     *
     * @param rank The rank
     * @return the {@link Score} instance, null if one isn't found
     */
    @Nullable
    public Score getScoreAtRank(int rank) {
        List<Score> values = new ArrayList<>(scores.values());

        if (rank < 1 || values.size() < rank) {
            return null;
        }

        return values.get(rank - 1);
    }

    public enum Sort {
        SCORE, TIME, DIFFICULTY
    }
}
