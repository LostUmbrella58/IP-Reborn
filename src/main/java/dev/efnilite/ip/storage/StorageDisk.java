package dev.efnilite.ip.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.leaderboard.Score;
import dev.efnilite.ip.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Local disk (json) storage manager.
 * Files are written to a temporary file first and then moved over the old file, so a crash
 * during a write never leaves a truncated file behind.
 *
 * @since 5.0.0
 */
class StorageDisk {

    public static @Nullable Map<UUID, Score> readScores(@NotNull String mode) {
        File file = getLeaderboardFile(mode);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return parseScores(IP.getGson(), reader, (uuid, value, ex) ->
                    IP.logging().warn("Skipping malformed score of %s in leaderboard %s: %s".formatted(uuid, mode, value)));
        } catch (IOException | RuntimeException ex) {
            IP.logging().stack("Error while trying to read leaderboard file %s, the file has been backed up and the scores in memory are kept".formatted(mode), ex);
            backup(file);
            return null;
        }
    }

    /**
     * Parses a leaderboard file.
     *
     * @param gson      The gson instance.
     * @param reader    The reader.
     * @param malformed Called for every entry that can't be parsed. That entry is skipped.
     * @return The scores.
     * @throws JsonParseException if the file isn't a valid leaderboard file.
     */
    static @NotNull Map<UUID, Score> parseScores(@NotNull Gson gson, @NotNull Reader reader, @NotNull MalformedEntryHandler malformed) {
        LeaderboardContainer read = gson.fromJson(reader, LeaderboardContainer.class);

        if (read == null || read.serialized == null) {
            throw new JsonParseException("Leaderboard file is empty or incomplete");
        }

        Map<UUID, Score> scores = new HashMap<>();

        new LinkedHashMap<>(read.serialized).forEach((uuid, score) -> {
            try {
                scores.put(uuid, Score.fromString(score));
            } catch (RuntimeException ex) {
                malformed.handle(uuid, score, ex);
            }
        });

        return scores;
    }

    interface MalformedEntryHandler {
        void handle(UUID uuid, String value, Exception ex);
    }

    public static boolean writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        LeaderboardContainer container = new LeaderboardContainer();
        scores.forEach((uuid, score) -> container.serialized.put(uuid, score.toString()));

        File file = getLeaderboardFile(mode);

        try {
            writeJson(IP.getGson(), container, file.toPath());
            return true;
        } catch (IOException | RuntimeException ex) {
            IP.logging().stack("Error while trying to write to leaderboard file %s".formatted(mode), ex);
            return false;
        }
    }

    private static File getLeaderboardFile(String mode) {
        return IP.getInFolder("leaderboards/%s.json".formatted(mode.toLowerCase()));
    }

    public static class LeaderboardContainer {
        @Expose
        public final Map<UUID, String> serialized = new LinkedHashMap<>();
    }

    public static void readPlayer(@NotNull ParkourPlayer player) {
        File file = getPlayerFile(player);

        if (!file.exists()) {
            player.setSettings(new HashMap<>());
            return;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            ParkourPlayer from = IP.getGson().fromJson(reader, ParkourPlayer.class);

            if (from == null) {
                throw new JsonParseException("Player file is empty");
            }

            Map<String, Object> settings = new HashMap<>();

            settings.put("style", from.style);
            settings.put("blockLead", from.blockLead);
            settings.put("useParticles", from.particles);
            settings.put("useSpecial", from.useSpecialBlocks);
            settings.put("showFallMsg", from.showFallMessage);
            settings.put("showScoreboard", from.showScoreboard);
            settings.put("selectedTime", from.selectedTime);
            settings.put("collectedRewards", from.collectedRewards);
            settings.put("locale", from._locale);
            settings.put("schematicDifficulty", from.schematicDifficulty);
            settings.put("sound", from.sound);

            player.setSettings(settings);
        } catch (IOException | RuntimeException ex) {
            IP.logging().stack("Error while trying to read disk data of %s, the file has been backed up and default settings are used".formatted(player.getName()), ex);
            backup(file);
            player.setSettings(new HashMap<>());
        }
    }

    public static void writePlayer(@NotNull ParkourPlayer player) {
        File file = getPlayerFile(player);

        try {
            writeJson(IP.getGson(), player, file.toPath());
        } catch (IOException | RuntimeException ex) {
            IP.logging().stack("Error while trying to write disk data of %s to file %s".formatted(player.getName(), file), ex);
        }
    }

    /**
     * Writes an object as json to a temporary file and then moves it over the target,
     * so the target either contains the old or the new contents, never a partial write.
     *
     * @param gson   The gson instance.
     * @param object The object to serialize.
     * @param target The file to write to.
     * @throws IOException if writing fails. The target is left untouched in that case.
     */
    static void writeJson(@NotNull Gson gson, @NotNull Object object, @NotNull Path target) throws IOException {
        Path folder = target.toAbsolutePath().getParent();
        Files.createDirectories(folder);

        Path temp = Files.createTempFile(folder, target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                gson.toJson(object, writer);
            }

            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailed) { // atomic move unsupported or refused (e.g. on some Windows setups)
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // copies an unreadable file next to itself so it won't be lost when it gets overwritten later
    private static final java.util.Set<String> BACKED_UP = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void backup(File file) {
        if (!file.exists()) {
            return;
        }

        // a periodic re-read of the same unreadable file shouldn't create a new copy every time
        if (!BACKED_UP.add("%s|%d|%d".formatted(file.getAbsolutePath(), file.lastModified(), file.length()))) {
            return;
        }

        Path backup = file.toPath().resolveSibling("%s.corrupt-%d".formatted(file.getName(), System.currentTimeMillis()));
        try {
            Files.copy(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            IP.logging().warn("Backed up unreadable file %s to %s".formatted(file, backup.getFileName()));
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to back up file %s".formatted(file), ex);
        }
    }

    private static File getPlayerFile(ParkourPlayer player) {
        return IP.getInFolder("players/%s.json".formatted(player.getUUID()));
    }
}
