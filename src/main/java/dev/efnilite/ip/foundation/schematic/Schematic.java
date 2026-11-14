package dev.efnilite.ip.foundation.schematic;

import dev.efnilite.ip.foundation.schematic.io.SchematicPaster;
import dev.efnilite.ip.foundation.schematic.io.SchematicReader;
import dev.efnilite.ip.foundation.schematic.io.SchematicWriter;
import dev.efnilite.ip.foundation.util.Task;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Main schematic handling class.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Schematic {

    /**
     * The version of this library instance.
     */
    public static final int VERSION = 1;
    private final File file;
    private final Map<Vector, BlockData> vectorBlockMap;
    /**
     * Constructor.
     *
     * @param file The file.
     */
    public Schematic(@NotNull File file, @NotNull Plugin plugin) throws IOException, ClassNotFoundException {
        this.file = file;
        this.vectorBlockMap = new SchematicReader().read(file, plugin);
    }

    /**
     * Pastes a schematic.
     *
     * @param location The smallest location.
     */
    public List<Block> paste(Location location) {
        return new SchematicPaster().paste(location, vectorBlockMap);
    }

    /**
     * Pastes a schematic at angles rotation.
     *
     * @param location The smallest location.
     * @param rotation The rotation where y = yaw in rad.
     */
    public List<Block> paste(Location location, double rotation) {
        return new SchematicPaster().paste(location, rotation, vectorBlockMap);
    }

    /**
     * @return The dimensions of this schematic.
     */
    public Vector getDimensions() {
        Set<Vector> offsets = vectorBlockMap.keySet();

        Vector min = offsets.stream().reduce((a, b) -> new Vector(min(a.getX(), b.getX()), min(a.getY(), b.getY()), min(a.getZ(), b.getZ()))).orElseThrow();
        Vector max = offsets.stream().reduce((a, b) -> new Vector(max(a.getX(), b.getX()), max(a.getY(), b.getY()), max(a.getZ(), b.getZ()))).orElseThrow();

        return max.subtract(min);
    }

    /**
     * @return True when this schematic contains unknown {@link BlockData}, false if it does.
     */
    public boolean hasUnknownMaterials() {
        return vectorBlockMap.containsValue(null);
    }

    /**
     * @return The map of vectors mapped to each {@link BlockData}.
     */
    public Map<Vector, BlockData> getVectorBlockMap() {
        return vectorBlockMap;
    }

    /**
     * @return The file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Loads a schematic asynchronously.
     *
     * @param file The file.
     * @return A new {@link Schematic} instance.
     */
    public static Schematic load(File file, Plugin plugin) throws IOException, ClassNotFoundException {
        return new Schematic(file, plugin);
    }

    /**
     * Loads a schematic asynchronously.
     *
     * @param file The file.
     * @return A new {@link Schematic} instance.
     */
    public static Schematic load(String file, Plugin plugin) throws IOException, ClassNotFoundException {
        return new Schematic(new File(file), plugin);
    }

    /**
     * Saves the selection between the two locations to file.
     * Blocks are read on the calling thread (must be the main thread), the file is written asynchronously.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return False when the selection only contains air and nothing was saved, true otherwise.
     */
    public static boolean save(String file, Location pos1, Location pos2, Plugin plugin) {
        return save(new File(file), pos1, pos2, plugin);
    }

    /**
     * Saves the selection between the two locations to file.
     * Blocks are read on the calling thread (must be the main thread), the file is written asynchronously.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return False when the selection only contains air and nothing was saved, true otherwise.
     */
    public static boolean save(File file, Location pos1, Location pos2, Plugin plugin) {
        SchematicWriter writer = new SchematicWriter();
        SchematicWriter.Snapshot snapshot = writer.snapshot(pos1, pos2);

        if (snapshot == null) {
            plugin.getLogger().warning("Not saving schematic %s: the selection only contains air".formatted(file.getName()));
            return false;
        }

        Task.create(plugin).async().execute(() -> writer.write(file, snapshot, plugin)).run();
        return true;
    }
}
