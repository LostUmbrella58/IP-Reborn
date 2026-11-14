package dev.efnilite.ip.migration;

import dev.efnilite.ip.IP;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Imports the former IPPlus and IEP data folders without modifying the originals.
 * Each source is imported once: after a successful import a marker file is written to the target folder,
 * so files an admin deletes afterwards are not copied back on the next startup. Existing files are never overwritten.
 */
public final class LegacyDataMigrator {

    /**
     * Name of the marker file written to a target folder after its source has been imported.
     */
    static final String MARKER = ".legacy-import-done";

    private LegacyDataMigrator() {
    }

    public static void migrate(IP plugin) {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        if (plugins == null) {
            return;
        }

        int copied = copyMissing(plugin, plugins.resolve("IPPlus"), plugin.getDataFolder().toPath().resolve("plus"));
        copied += copyMissing(plugin, plugins.resolve("IEP"), plugin.getDataFolder().toPath().resolve("elytra"));

        if (copied > 0) {
            plugin.getLogger().info("Imported " + copied + " legacy IPPlus/IEP data files; the original folders were left untouched.");
        }
    }

    private static int copyMissing(IP plugin, Path source, Path target) {
        return copyMissing(source, target, plugin.getLogger()::warning);
    }

    /**
     * Copies all files from source missing in target, once.
     *
     * @param source The legacy folder.
     * @param target The folder to import into.
     * @param warn   Logger for warnings.
     * @return The amount of copied files.
     */
    static int copyMissing(Path source, Path target, Consumer<String> warn) {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        Path marker = target.resolve(MARKER);
        if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            return 0; // already imported
        }

        AtomicInteger copied = new AtomicInteger();
        AtomicBoolean failed = new AtomicBoolean();
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(path -> !Files.isSymbolicLink(path)).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination);
                    } else if (Files.notExists(destination, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                        copied.incrementAndGet();
                    }
                } catch (IOException exception) {
                    failed.set(true);
                    warn.accept("Could not import legacy file " + path + ": " + exception.getMessage());
                }
            });
        } catch (IOException | UncheckedIOException exception) {
            warn.accept("Could not inspect legacy data folder " + source + ": " + exception.getMessage());
            return copied.get();
        }

        if (failed.get()) {
            return copied.get(); // retry the missing files on the next startup
        }

        try {
            Files.createDirectories(target);
            Files.writeString(marker, "Imported from " + source.toAbsolutePath() + System.lineSeparator()
                    + "Delete this file to import missing files from that folder again." + System.lineSeparator());
        } catch (IOException exception) {
            warn.accept("Could not write legacy import marker " + marker + ": " + exception.getMessage());
        }
        return copied.get();
    }
}
