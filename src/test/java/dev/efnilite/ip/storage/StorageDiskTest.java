package dev.efnilite.ip.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.efnilite.ip.leaderboard.Score;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StorageDiskTest {

    private final Gson gson = new Gson();

    @Test
    void writeJsonReplacesFileWithoutLeavingTempFiles(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("leaderboards").resolve("default.json");
        UUID uuid = UUID.randomUUID();

        StorageDisk.LeaderboardContainer first = new StorageDisk.LeaderboardContainer();
        first.serialized.put(uuid, new Score("a", "00:01:000", "0.5", 1).toString());
        StorageDisk.writeJson(gson, first, target);

        StorageDisk.LeaderboardContainer second = new StorageDisk.LeaderboardContainer();
        second.serialized.put(uuid, new Score("a", "00:02:000", "0.5", 2).toString());
        StorageDisk.writeJson(gson, second, target);

        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            Map<UUID, Score> scores = StorageDisk.parseScores(gson, reader, (u, v, ex) -> fail("unexpected malformed entry"));
            assertEquals(2, scores.get(uuid).score());
        }

        try (Stream<Path> files = Files.list(target.getParent())) {
            assertEquals(List.of(target.getFileName()), files.map(Path::getFileName).toList());
        }
    }

    @Test
    void truncatedFileFailsInsteadOfReturningEmpty() {
        String truncated = "{\"serialized\":{\"" + UUID.randomUUID() + "\":\"a,00:01:0";

        assertThrows(JsonParseException.class, () -> StorageDisk.parseScores(gson, new StringReader(truncated), (u, v, ex) -> {}));
        assertThrows(JsonParseException.class, () -> StorageDisk.parseScores(gson, new StringReader(""), (u, v, ex) -> {}));
    }

    @Test
    void malformedEntriesAreSkipped() {
        UUID good = UUID.randomUUID();
        UUID bad = UUID.randomUUID();
        String json = "{\"serialized\":{\"%s\":\"a,00:01:000,0.5,3\",\"%s\":\"broken\"}}".formatted(good, bad);

        List<UUID> skipped = new ArrayList<>();
        Map<UUID, Score> scores = StorageDisk.parseScores(gson, new StringReader(json), (u, v, ex) -> skipped.add(u));

        assertEquals(Map.of(good, new Score("a", "00:01:000", "0.5", 3)), scores);
        assertEquals(List.of(bad), skipped);
    }
}
