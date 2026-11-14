package dev.efnilite.ip.foundation.particle;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Does particle stuff
 *
 * @author Efnilite
 */
public class Particles {

    /**
     * Draws particles.
     * Uses default Bukkit World methods to spawn particles using {@link ParticleData}
     *
     * @param at   The location of the particles
     * @param data The particle data
     */
    public static <T> void draw(Location at, @NotNull ParticleData<T> data) {
        World world = at.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        world.spawnParticle(
                data.getType(),
                at,
                data.getSize(),
                data.getOffsetX(),
                data.getOffsetY(),
                data.getOffsetZ(),
                data.getSpeed(),
                safe
        );
    }

    public static <T> void draw(Location at, @NotNull ParticleData<T> data, Player player) {
        World world = at.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        }

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        player.spawnParticle(
                data.getType(),
                at,
                data.getSize(),
                data.getOffsetX(),
                data.getOffsetY(),
                data.getOffsetZ(),
                data.getSpeed(),
                safe
        );
    }

    /**
     * Draws a particle line between 2 points
     *
     * @param one             The location from where the tower shoots (always shoot variable)
     * @param two             The location of the entity
     * @param data            The particle data
     * @param distanceBetween The distance between particles in blocks
     */
    public static <T> void line(Location one, Location two, ParticleData<T> data, double distanceBetween) {
        World world = one.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#line)");
        }
        double dist = one.distance(two);
        Vector p1 = one.toVector();
        Vector p2 = two.toVector();
        Vector vec = p2.clone().subtract(p1).normalize().multiply(distanceBetween);

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(),
                data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
        world.spawnParticle(data.getType(), p2.getX(), p2.getY(), p2.getZ(),
                data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);

        double length = 0;
        for (; length < dist; p1.add(vec)) {
            world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(),
                    data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
            length += distanceBetween;
        }
    }

    /**
     * Creates a box of particles
     * Calculates the min point and then adds all the dimensions to each other point to get the locations of all the points
     *
     * @param box             The box which the particles will go around
     * @param world           The world
     * @param data            The particle data
     * @param distanceBetween The distance between particles
     */
    public static <T> void box(BoundingBox box, @NotNull World world, ParticleData<T> data, Player player, double distanceBetween) {
        Location point1 = box.getMin().toLocation(world);
        Location point2, point3, point4, point5, point6, point7, point8;

        if (box.getWidthX() == 1 && box.getWidthZ() == 1) {
            point2 = point1.clone().add(box.getWidthX(), 0, 0);
            point3 = point2.clone().add(0, 0, box.getWidthZ());
            point4 = point1.clone().add(0, 0, box.getWidthZ());
            point5 = point1.clone().add(0, box.getHeight(), 0);
            point6 = point2.clone().add(0, box.getHeight(), 0);
            point7 = point3.clone().add(0, box.getHeight(), 0);
            point8 = point4.clone().add(0, box.getHeight(), 0);
        } else {
            point2 = point1.clone().add(box.getWidthX() + 1, 0, 0);
            point3 = point2.clone().add(0, 0, box.getWidthZ() + 1);
            point4 = point1.clone().add(0, 0, box.getWidthZ() + 1);
            point5 = point1.clone().add(0, box.getHeight() + 1, 0);
            point6 = point2.clone().add(0, box.getHeight() + 1, 0);
            point7 = point3.clone().add(0, box.getHeight() + 1, 0);
            point8 = point4.clone().add(0, box.getHeight() + 1, 0);
        }

        line(point1, point2, data, player, distanceBetween);
        line(point2, point3, data, player, distanceBetween);
        line(point3, point4, data, player, distanceBetween);
        line(point4, point1, data, player, distanceBetween);

        line(point5, point6, data, player, distanceBetween);
        line(point6, point7, data, player, distanceBetween);
        line(point7, point8, data, player, distanceBetween);
        line(point5, point8, data, player, distanceBetween);

        line(point1, point5, data, player, distanceBetween);
        line(point2, point6, data, player, distanceBetween);
        line(point3, point7, data, player, distanceBetween);
        line(point4, point8, data, player, distanceBetween);
    }

    /**
     * Creates a box of particles.
     * Calculates the min point and then adds all the dimensions to each other point to get the locations of all the points
     *
     * @param box             The box which the particles will go around
     * @param world           The world
     * @param data            The particle data
     * @param distanceBetween The distance between particles
     */
    public static <T> void box(BoundingBox box, @NotNull World world, ParticleData<T> data, double distanceBetween) {
        Location point1 = box.getMin().toLocation(world);
        Location point2, point3, point4, point5, point6, point7, point8;

        if (box.getWidthX() == 1.0 && box.getWidthZ() == 1.0) {
            point2 = point1.clone().add(box.getWidthX(), 0.0, 0.0);
            point3 = point2.clone().add(0.0, 0.0, box.getWidthZ());
            point4 = point1.clone().add(0.0, 0.0, box.getWidthZ());
            point5 = point1.clone().add(0.0, box.getHeight(), 0.0);
            point6 = point2.clone().add(0.0, box.getHeight(), 0.0);
            point7 = point3.clone().add(0.0, box.getHeight(), 0.0);
            point8 = point4.clone().add(0.0, box.getHeight(), 0.0);
        } else {
            point2 = point1.clone().add(box.getWidthX() + 1.0, 0.0, 0.0);
            point3 = point2.clone().add(0.0, 0.0, box.getWidthZ() + 1.0);
            point4 = point1.clone().add(0.0, 0.0, box.getWidthZ() + 1.0);
            point5 = point1.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point6 = point2.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point7 = point3.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point8 = point4.clone().add(0.0, box.getHeight() + 1.0, 0.0);
        }

        line(point1, point2, data, distanceBetween);
        line(point2, point3, data, distanceBetween);
        line(point3, point4, data, distanceBetween);
        line(point4, point1, data, distanceBetween);

        line(point5, point6, data, distanceBetween);
        line(point6, point7, data, distanceBetween);
        line(point7, point8, data, distanceBetween);
        line(point5, point8, data, distanceBetween);

        line(point1, point5, data, distanceBetween);
        line(point2, point6, data, distanceBetween);
        line(point3, point7, data, distanceBetween);
        line(point4, point8, data, distanceBetween);
    }

    /**
     * {@link #line(Location, Location, ParticleData, double)} but for players
     */
    public static <T> void line(Location one, Location two, ParticleData<T> data, Player player, double distanceBetween) {
        World world = one.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#line)");
        }
        double dist = one.distance(two);
        Vector p1 = one.toVector();
        Vector p2 = two.toVector();
        Vector vec = p2.clone().subtract(p1).normalize().multiply(distanceBetween);

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        player.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(),
                data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
        player.spawnParticle(data.getType(), p2.getX(), p2.getY(), p2.getZ(),
                data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);

        double length = 0;
        for (; length < dist; p1.add(vec)) {
            player.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(),
                    data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
            length += distanceBetween;
        }
    }

    /**
     * Creates a circle
     *
     * @param location The center
     * @param data     The particle data
     * @param radius   The radius of the circle
     * @param amount   The amount of particles
     */
    public static <T> void circle(Location location, ParticleData<T> data, double radius, double amount) {
        World world = location.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#circle)");
        }

        double increment = (2 * Math.PI) / amount; // calc degree per amount, 2 x pi x r = circumference
        double y = location.getY();

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            double angle = i * increment;
            double x = location.getX() + (radius * Math.cos(angle));
            double z = location.getZ() + (radius * Math.sin(angle));
            world.spawnParticle(data.getType(), x, y, z,
                    data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
        }
    }

    public static <T> void circle(Location location, ParticleData<T> data, @NotNull Player player, double radius, double amount) {
        double increment = (2 * Math.PI) / amount; // calc degree per amount, 2 x pi x r = circumference
        double y = location.getY();

        Object safe = safeData(data);
        if (safe == UNSUPPORTED) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            double angle = i * increment;
            double x = location.getX() + (radius * Math.cos(angle));
            double z = location.getZ() + (radius * Math.sin(angle));
            player.spawnParticle(data.getType(), x, y, z,
                    data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), safe);
        }
    }

    // =========================
    // Internal: safe data maker
    // =========================

    /**
     * Returned by {@link #safeData(ParticleData)} when no data can be made for the particle type.
     * Callers skip spawning in that case.
     */
    private static final Object UNSUPPORTED = new Object();
    private static final Set<Particle> WARNED = ConcurrentHashMap.newKeySet();

    private static <T> Object safeData(@NotNull ParticleData<T> data) {
        Particle type = data.getType();
        Class<?> required = type.getDataType();
        Object provided = data.getData();

        // no data needed
        if (required == Void.class) {
            return null;
        }

        // provided with the right type
        if (provided != null && required.isInstance(provided)) {
            return provided;
        }

        // fallbacks based on the required type
        if (required == Particle.Spell.class) {
            return new Particle.Spell(Color.fromRGB(0x33CCFF), 1.0f);
        }

        if (required == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.fromRGB(0x33CCFF), 1.0f);
        }

        if (required == Particle.DustTransition.class) {
            return new Particle.DustTransition(
                    Color.fromRGB(0x33CCFF),
                    Color.fromRGB(0xFFFFFF),
                    1.0f
            );
        }

        // ENTITY_EFFECT, TINTED_LEAVES, FLASH, ...
        if (required == Color.class) {
            return Color.WHITE;
        }

        // SCULK_CHARGE (roll angle)
        if (required == Float.class) {
            return 0.0f;
        }

        // SHRIEK (delay)
        if (required == Integer.class) {
            return 0;
        }

        // BLOCK / FALLING_DUST
        if (required == BlockData.class) {
            return Material.LIGHT.createBlockData();
        }

        // ITEM
        if (required == ItemStack.class) {
            return new ItemStack(Material.LIGHT);
        }

        // VIBRATION, TRAIL etc. need a target location, so there is no sensible default: skip and warn once
        if (WARNED.add(type)) {
            Bukkit.getLogger().warning("[IP] Particle " + type.name() + " requires " + required.getSimpleName()
                    + " data, which can't be provided. These particles will not be shown. Pick another particle type.");
        }

        return UNSUPPORTED;
    }
}
