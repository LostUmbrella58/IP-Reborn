package dev.efnilite.ip.foundation.schematic.io;

import dev.efnilite.ip.foundation.util.Colls;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Schematic pasting handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicPaster {

    /**
     * Pastes a schematic with the provided map of offsets and BlockData.
     *
     * @param location      The smallest location.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> Colls.thread(vectorDataMap)
                .mapk((k, v) -> location.clone().add(k).getBlock())
                .get());
    }

    /**
     * Pastes a schematic at angle rotation with the provided map of offsets and BlockData.
     * Rotates {@link Directional}, {@link MultipleFacing}, {@link Rotatable} and {@link Orientable} blocks.
     * The cached offsets and block data are never modified; every paste works on copies.
     *
     * @param location      The smallest location.
     * @param rotation      The rotation where y is the yaw in rad.
     * @param vectorDataMap The map.
     * @return The affected blocks.
     */
    public List<Block> paste(Location location, double rotation, Map<Vector, BlockData> vectorDataMap) {
        return paste(() -> Colls.thread(vectorDataMap)
                .mapkv((vector) -> location.clone().add(rotateOffset(vector, rotation)).getBlock(),
                        (data) -> rotateData(data, rotation))
                .get());
    }

    /**
     * Rotates an offset around the y-axis without modifying the provided vector.
     * Coordinates within {@link Vector#getEpsilon()} of an integer are snapped to it,
     * so a 90 degree rotation never lands a block one off due to floating point error.
     *
     * @param vector   The offset.
     * @param rotation The rotation in rad.
     * @return A new rotated vector.
     */
    static Vector rotateOffset(Vector vector, double rotation) {
        return round(vector.clone().rotateAroundY(rotation));
    }

    /**
     * Rotates a block face around the y-axis using the same rotation as {@link #rotateOffset(Vector, double)}.
     * Faces without a horizontal component (UP, DOWN, SELF) are returned unchanged.
     *
     * @param face         The face.
     * @param rotation     The rotation in rad.
     * @param allowedFaces The faces the result may be.
     * @return The allowed face closest to the rotated face.
     */
    static BlockFace rotateFace(BlockFace face, double rotation, Collection<BlockFace> allowedFaces) {
        if (face.getModX() == 0 && face.getModZ() == 0) {
            return face;
        }

        Vector rotated = rotateOffset(face.getDirection(), rotation);

        return allowedFaces.stream()
                .filter(f -> f.getModX() != 0 || f.getModZ() != 0)
                .min(Comparator.comparingDouble(f -> f.getDirection().angle(rotated)))
                .orElse(face);
    }

    /**
     * @return True when rotation swaps the x and z axes (i.e. it is about 90 or 270 degrees).
     */
    static boolean swapsHorizontalAxes(double rotation) {
        return Math.abs(Math.sin(rotation)) > Math.abs(Math.cos(rotation));
    }

    // returns a rotated copy of data. the provided (cached) data is never modified.
    private BlockData rotateData(BlockData original, double rotation) {
        if (original == null) {
            return null;
        }

        BlockData data = original.clone();

        if (data instanceof Directional directional) {
            directional.setFacing(rotateFace(directional.getFacing(), rotation, directional.getFaces()));
        }

        if (data instanceof Rotatable rotatable) {
            rotatable.setRotation(rotateFace(rotatable.getRotation(), rotation, ROTATABLE_FACES));
        }

        if (data instanceof Orientable orientable && swapsHorizontalAxes(rotation)) {
            Axis axis = orientable.getAxis();
            Axis swapped = axis == Axis.X ? Axis.Z : axis == Axis.Z ? Axis.X : axis;

            if (orientable.getAxes().contains(swapped)) {
                orientable.setAxis(swapped);
            }
        }

        if (data instanceof MultipleFacing multipleFacing) {
            Set<BlockFace> allowed = multipleFacing.getAllowedFaces();
            Set<BlockFace> enabled = new HashSet<>(multipleFacing.getFaces());

            for (BlockFace face : allowed) {
                multipleFacing.setFace(face, false);
            }
            for (BlockFace face : enabled) {
                BlockFace rotated = rotateFace(face, rotation, allowed);

                if (allowed.contains(rotated)) {
                    multipleFacing.setFace(rotated, true);
                }
            }
        }

        return data;
    }

    private static final List<BlockFace> ROTATABLE_FACES = List.of(
            BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST);

    // rounds a vector to avoid small discrepancies like having an offset of E-16 or 2.9999999999999996
    private static Vector round(Vector vector) {
        return new Vector(round(vector.getX()), round(vector.getY()), round(vector.getZ()));
    }

    private static double round(double value) {
        double nearest = Math.rint(value);

        return Math.abs(value - nearest) < Vector.getEpsilon() ? nearest : value;
    }

    private List<Block> paste(Supplier<Map<Block, BlockData>> blocksGetter) {
        try {
            Map<Block, BlockData> blocks = CompletableFuture.supplyAsync(blocksGetter).get();

            blocks.forEach((block, data) -> {
                if (block == null || data == null) {
                    return;
                }

                block.setBlockData(data, false);
            });

            return new ArrayList<>(blocks.keySet());
        } catch (InterruptedException | ExecutionException ex) {
            return Collections.emptyList();
        }
    }
}
