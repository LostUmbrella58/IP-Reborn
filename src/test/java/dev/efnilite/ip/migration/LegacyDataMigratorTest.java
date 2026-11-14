package dev.efnilite.ip.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataMigratorTest {

    @Test
    void importsOnceAndNeverOverwrites(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("IPPlus");
        Path target = dir.resolve("IP").resolve("plus");
        Files.createDirectories(source.resolve("leaderboards"));
        Files.writeString(source.resolve("config.yml"), "legacy");
        Files.writeString(source.resolve("leaderboards").resolve("speed.json"), "{}");

        Files.createDirectories(target);
        Files.writeString(target.resolve("config.yml"), "existing");

        List<String> warnings = new ArrayList<>();

        assertEquals(1, LegacyDataMigrator.copyMissing(source, target, warnings::add));
        assertEquals("existing", Files.readString(target.resolve("config.yml")));
        assertTrue(Files.exists(target.resolve("leaderboards").resolve("speed.json")));
        assertTrue(Files.exists(target.resolve(LegacyDataMigrator.MARKER)));

        // an admin deletes an imported file: it must not come back on the next startup
        Files.delete(target.resolve("leaderboards").resolve("speed.json"));
        assertEquals(0, LegacyDataMigrator.copyMissing(source, target, warnings::add));
        assertFalse(Files.exists(target.resolve("leaderboards").resolve("speed.json")));

        assertTrue(warnings.isEmpty());
    }

    @Test
    void missingSourceDoesNothing(@TempDir Path dir) {
        Path target = dir.resolve("plus");

        assertEquals(0, LegacyDataMigrator.copyMissing(dir.resolve("IEP"), target, warning -> fail(warning)));
        assertFalse(Files.exists(target.resolve(LegacyDataMigrator.MARKER)));
    }
}
