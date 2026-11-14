package dev.efnilite.ip.foundation.schematic.io;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchematicPasterTest {

    private static final List<BlockFace> HORIZONTAL = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    // the same way ParkourGenerator computes its snapped angles
    private static final double[] ROTATIONS = {
            Math.atan2(1, 0), Math.atan2(-1, 0), Math.atan2(0, -1), Math.atan2(0, 1)
    };

    @Test
    void rotateOffsetDoesNotMutateInput() {
        Vector original = new Vector(3, 1, -5);

        SchematicPaster.rotateOffset(original, Math.PI / 2);

        assertEquals(new Vector(3, 1, -5), original);
    }

    @Test
    void rotateOffsetSnapsToWholeBlocks() {
        for (double rotation : ROTATIONS) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    Vector rotated = SchematicPaster.rotateOffset(new Vector(x, 0, z), rotation);

                    assertEquals(Math.rint(rotated.getX()), rotated.getX(), "x for " + x + "," + z + " at " + rotation);
                    assertEquals(Math.rint(rotated.getZ()), rotated.getZ(), "z for " + x + "," + z + " at " + rotation);
                }
            }
        }
    }

    @Test
    void faceRotationMatchesPositionRotation() {
        for (double rotation : ROTATIONS) {
            for (BlockFace face : HORIZONTAL) {
                BlockFace rotatedFace = SchematicPaster.rotateFace(face, rotation, HORIZONTAL);
                Vector rotatedPosition = SchematicPaster.rotateOffset(face.getDirection(), rotation);

                assertEquals(rotatedPosition, rotatedFace.getDirection(), face + " at " + rotation);
            }
        }
    }

    @Test
    void quarterTurnIsConsistentWithStairsLikeFaces() {
        BlockFace once = SchematicPaster.rotateFace(BlockFace.NORTH, Math.PI / 2, HORIZONTAL);
        BlockFace twice = SchematicPaster.rotateFace(once, Math.PI / 2, HORIZONTAL);

        assertNotEquals(BlockFace.NORTH, once);
        assertEquals(BlockFace.SOUTH, twice);
    }

    @Test
    void verticalFacesAreUnchanged() {
        assertEquals(BlockFace.UP, SchematicPaster.rotateFace(BlockFace.UP, Math.PI / 2, HORIZONTAL));
        assertEquals(BlockFace.DOWN, SchematicPaster.rotateFace(BlockFace.DOWN, Math.PI, HORIZONTAL));
    }

    @Test
    void axisSwapOnlyForQuarterTurns() {
        assertTrue(SchematicPaster.swapsHorizontalAxes(Math.atan2(1, 0)));
        assertTrue(SchematicPaster.swapsHorizontalAxes(Math.atan2(-1, 0)));
        assertFalse(SchematicPaster.swapsHorizontalAxes(Math.atan2(0, -1)));
        assertFalse(SchematicPaster.swapsHorizontalAxes(0));
    }
}
