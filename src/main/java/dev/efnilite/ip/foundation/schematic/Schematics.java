package dev.efnilite.ip.foundation.schematic;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe schematic cache. Schematics are loaded asynchronously, so readers on the main thread
 * may see an empty cache until loading has finished.
 */
public class Schematics {

    private static final Map<Plugin, Map<String, Schematic>> cache = new ConcurrentHashMap<>();

    public static void addFromFiles(@NotNull Plugin plugin, @NotNull File... files) throws IOException, ClassNotFoundException {
        Map<String, Schematic> current = cache.computeIfAbsent(plugin, p -> new ConcurrentHashMap<>());

        for (File file : files) {
            current.put(file.getName(), Schematic.load(file, plugin));
        }

        plugin.getLogger().info("Loaded all schematics!");
    }

    /**
     * @return The schematic, or null if it doesn't exist or schematics haven't finished loading yet.
     */
    public static @Nullable Schematic getSchematic(@NotNull Plugin plugin, @NotNull String schematicName) {
        Map<String, Schematic> schematics = cache.get(plugin);

        return schematics == null ? null : schematics.get(schematicName);
    }

    /**
     * @return The names of all loaded schematics. Empty if schematics haven't finished loading yet.
     */
    public static Set<String> getSchematicNames(@NotNull Plugin plugin) {
        Map<String, Schematic> schematics = cache.get(plugin);

        return schematics == null ? Collections.emptySet() : Collections.unmodifiableSet(schematics.keySet());
    }

    /**
     * @return All loaded schematics. Empty if schematics haven't finished loading yet.
     */
    public static Collection<Schematic> getSchematics(@NotNull Plugin plugin) {
        Map<String, Schematic> schematics = cache.get(plugin);

        return schematics == null ? Collections.emptyList() : Collections.unmodifiableCollection(schematics.values());
    }
}
