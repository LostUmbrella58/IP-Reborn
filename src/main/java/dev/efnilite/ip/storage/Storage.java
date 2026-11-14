package dev.efnilite.ip.storage;

import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * I/O handling.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Storage {

    public static void init(String mode) {
        if (Option.SQL) {
            StorageSQL.init(mode);
        }
    }

    public static void close() {
        if (Option.SQL) {
            StorageSQL.close();
        }
    }

    /**
     * Reads scores.
     *
     * @param mode The mode.
     * @return Map with all scores, unsorted. Null if reading failed, in which case the caller should keep its current data.
     */
    public static @Nullable Map<UUID, Score> readScores(@NotNull String mode) {
        if (Option.SQL) {
            return StorageSQL.readScores(mode);
        } else {
            return StorageDisk.readScores(mode);
        }
    }

    /**
     * Writes scores.
     *
     * @param mode    The mode.
     * @param scores  The score map. Not modified.
     * @param removed Players whose scores have been reset and should be deleted from storage.
     * @param changed Scores changed since the last save. MySQL only writes these; disk writes the whole map.
     * @return True if writing succeeded.
     */
    public static boolean writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores, @NotNull Set<UUID> removed,
                                      @NotNull Map<UUID, Score> changed) {
        if (Option.SQL) {
            // MySQL keeps every row, so only the scores changed since the last save need writing
            return StorageSQL.writeScores(mode, changed, removed);
        } else {
            return StorageDisk.writeScores(mode, scores);
        }
    }

    /**
     * Reads player data and applies changes.
     *
     * @param player The player.
     */
    public static void readPlayer(@NotNull ParkourPlayer player) {
        if (Option.SQL) {
            StorageSQL.readPlayer(player);
        } else {
            StorageDisk.readPlayer(player);
        }
    }

    /**
     * Writes player data.
     *
     * @param player The player.
     */
    public static void writePlayer(@NotNull ParkourPlayer player) {
        if (Option.SQL) {
            StorageSQL.writePlayer(player);
        } else {
            StorageDisk.writePlayer(player);
        }
    }
}