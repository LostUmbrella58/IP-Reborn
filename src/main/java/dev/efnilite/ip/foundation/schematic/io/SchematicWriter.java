package dev.efnilite.ip.foundation.schematic.io;

import dev.efnilite.ip.foundation.schematic.Schematic;
import dev.efnilite.ip.foundation.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schematic writing handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicWriter {

    /**
     * Saves blocks to the specified file. Reads the world, so this must be called on the main thread.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     */
    public void save(File file, Location pos1, Location pos2, Plugin plugin) {
        Snapshot snapshot = snapshot(pos1, pos2);

        if (snapshot == null) {
            plugin.getLogger().warning("Not saving schematic %s: the selection only contains air".formatted(file.getName()));
            return;
        }

        write(file, snapshot, plugin);
    }

    /**
     * Reads the blocks between the two positions. Must be called on the main thread.
     *
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return The palette and offset data, or null when the selection only contains air.
     */
    public @Nullable Snapshot snapshot(Location pos1, Location pos2) {
        List<Block> blocks = getBlocks(Locations.min(pos1, pos2), Locations.max(pos1, pos2));

        if (blocks.isEmpty()) {
            return null;
        }

        Map<String, Integer> palette = getPalette(blocks);
        Map<String, Integer> offsetData = getOffsetData(blocks, palette);

        return new Snapshot(palette, offsetData);
    }

    /**
     * Writes a snapshot to file. Does not touch the world, so it is safe to call asynchronously.
     *
     * @param file     The file.
     * @param snapshot The snapshot from {@link #snapshot(Location, Location)}.
     */
    public void write(File file, Snapshot snapshot, Plugin plugin) {
        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(Schematic.VERSION);
            stream.writeObject(snapshot.palette());
            stream.writeObject(snapshot.offsets());
            stream.flush();
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to write schematic %s: %s".formatted(file.getName(), ex));
        }
    }

    /**
     * The block data read from the world.
     *
     * @param palette The palette.
     * @param offsets The offsets mapped to palette ids.
     */
    public record Snapshot(Map<String, Integer> palette, Map<String, Integer> offsets) {

    }


    // returns all blocks between the min location (minL) and max location (maxL)
    private List<Block> getBlocks(Location minL, Location maxL) {
        List<Block> blocks = new ArrayList<>();
        Location location = new Location(minL.getWorld() == null ? maxL.getWorld() : minL.getWorld(), 0, 0, 0);

        for (int x = minL.getBlockX(); x <= maxL.getBlockX(); x++) {
            for (int y = minL.getBlockY(); y <= maxL.getBlockY(); y++) {
                for (int z = minL.getBlockZ(); z <= maxL.getBlockZ(); z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    if (location.getBlock().getType().isAir()) {
                        continue;
                    }

                    blocks.add(location.getBlock());
                }
            }
        }
        return blocks;
    }


    // returns block palette where ids are used instead of the full blockdata to reduce file space
    private Map<String, Integer> getPalette(List<Block> blocks) {
        Map<String, Integer> palette = new HashMap<>();

        blocks.stream()
            .map(block -> block.getBlockData().getAsString())
            .distinct()
            .forEach(datum -> palette.put(datum, palette.size()));

        return palette;
    }

    // returns the offset data, where each block offset (position relative to minimum location)
    // is mapped to the data at that position.
    // the first item in the array is the generic BlockData of that block.
    // the second item in the array is the special data of that block, as encoded by the State instances.
    private Map<String, Integer> getOffsetData(List<Block> blocks, Map<String, Integer> palette) {
        List<Location> locations = blocks.stream().map(Block::getLocation).toList();
        Location min = locations.stream().reduce(Locations::min).orElseThrow();

        Map<String, Integer> offsetData = new HashMap<>();

        for (Location loc : locations) {
            offsetData.put(loc.clone().subtract(min).toVector().toString(),
                    palette.get(loc.clone().getBlock().getBlockData().getAsString()));
        }

        return offsetData;
    }
}
