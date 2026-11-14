package dev.efnilite.ip.hologram;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.hologram.HologramConfig.Entry;
import dev.efnilite.ip.hologram.HologramConfig.IconSpec;
import dev.efnilite.ip.hologram.HologramConfig.Page;
import dev.efnilite.ip.hook.PAPIHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * One live hologram. Owns its TextDisplay (the visible lines), zero or more ItemDisplay
 * icons, and one Interaction (the click box for paging).
 *
 * <p><b>Entity lifecycle invariants:</b></p>
 * <ul>
 *   <li>Every spawned entity is tagged with {@link HologramKeys#HOLOGRAM_ID} = our id,
 *       {@link HologramKeys#HOLOGRAM_ROLE} = "text" | "icon" | "interaction", and
 *       (for icons) {@link HologramKeys#HOLOGRAM_INDEX} = the icon's slot.</li>
 *   <li>Every entity is {@code setPersistent(false)} — Mojang will NOT write them to
 *       chunk data, so crash/kill -9 cleans up after itself.</li>
 *   <li>{@link #destroy()} kills everything we hold and clears the references. Safe to
 *       call multiple times.</li>
 *   <li>The host chunk holds an IP plugin ticket while this hologram is live so the
 *       Display entities aren't culled out from under players who walk away briefly.</li>
 * </ul>
 */
public final class TrackedHologram {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Entry config;

    // Mutable state owned by this hologram. All access on the main thread.
    private TextDisplay textDisplay;
    private Interaction interaction;
    private final List<ItemDisplay> icons = new ArrayList<>();
    private int currentPage = 0;
    private boolean destroyed = false;

    public TrackedHologram(Entry config) {
        this.config = config;
    }

    public Entry config() { return config; }
    public boolean isDestroyed() { return destroyed; }

    /**
     * @return True when this hologram was spawned and its main entities still exist in the world.
     * False when they were removed externally (e.g. /kill, world unload).
     */
    public boolean isHealthy() {
        return !destroyed
                && textDisplay != null && textDisplay.isValid()
                && interaction != null && interaction.isValid();
    }
    public int currentPage() { return currentPage; }
    public int pageCount() { return config.pages().size(); }

    /**
     * Spawns the entity set. Must be called on the main thread with the chunk loaded
     * (HologramManager handles the chunk ticket). Returns false if something went wrong.
     */
    public boolean spawn() {
        if (destroyed) throw new IllegalStateException("already destroyed");
        if (textDisplay != null) return true; // already spawned

        Location anchor = config.location().clone();
        if (anchor.getWorld() == null) return false;

        // -- TextDisplay (main line container) --
        textDisplay = anchor.getWorld().spawn(anchor, TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setDefaultBackground(false);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            // Anchor at TOP of visible text rather than the default CENTER. Matches
            // the DecentHolograms layout users have built their hub coordinates around,
            // and makes icon offsets predictable: a positive offset Y is ALWAYS above
            // the text, regardless of how many lines a given page has.
            applyTopAnchorTransform(td, anchor, config.pages().get(currentPage).lines().size());
            tag(td, HologramKeys.ROLE_TEXT, -1);
        });

        // -- Interaction (page nav) --
        Location interactionLoc = anchor.clone().add(0, config.interaction().offsetY(), 0);
        interaction = interactionLoc.getWorld().spawn(interactionLoc, Interaction.class, ie -> {
            ie.setPersistent(false);
            ie.setInteractionWidth(config.interaction().width());
            ie.setInteractionHeight(config.interaction().height());
            ie.setResponsive(true);
            tag(ie, HologramKeys.ROLE_INTERACTION, -1);
        });

        // -- ItemDisplay icons for the current page --
        spawnIconsForPage(currentPage);

        // -- Initial render --
        renderCurrentPage();

        return true;
    }

    /** Re-resolves placeholders in the current page and pushes them to the TextDisplay. */
    public void renderCurrentPage() {
        if (destroyed || textDisplay == null || !textDisplay.isValid()) return;
        Page page = config.pages().get(currentPage);
        Component text = composeText(page);
        textDisplay.text(text);
        // Re-apply top-anchor translation in case the page line count changed (different
        // pages of the same hologram often have different line counts; without this the
        // text would visibly drift up/down on page flip).
        applyTopAnchorTransform(textDisplay, config.location(), page.lines().size());
    }

    /**
     * Flips to the next or previous page. Re-spawns icons (since icon set is per-page),
     * re-renders text. Wraps around at the ends.
     */
    public void flipPage(int delta) {
        if (destroyed || pageCount() <= 1) return;
        currentPage = Math.floorMod(currentPage + delta, pageCount());
        // Remove icons from the old page first, then spawn the new ones.
        despawnIcons();
        spawnIconsForPage(currentPage);
        renderCurrentPage();
    }

    /** Sets the current page absolutely; clamps to bounds. */
    public void setPage(int page) {
        if (destroyed || pageCount() <= 1) return;
        currentPage = Math.floorMod(page, pageCount());
        despawnIcons();
        spawnIconsForPage(currentPage);
        renderCurrentPage();
    }

    /**
     * Kills every entity this hologram owns. Idempotent — safe to call multiple times.
     * Does NOT release the chunk ticket (HologramManager owns that resource).
     */
    public void destroy() {
        destroyed = true;
        if (textDisplay != null) {
            try { textDisplay.remove(); } catch (Throwable ignored) {}
            textDisplay = null;
        }
        if (interaction != null) {
            try { interaction.remove(); } catch (Throwable ignored) {}
            interaction = null;
        }
        despawnIcons();
    }

    private void spawnIconsForPage(int pageIdx) {
        Page page = config.pages().get(pageIdx);
        Location anchor = config.location();
        int i = 0;
        for (IconSpec icon : page.icons()) {
            final int idx = i++;
            Location loc = anchor.clone().add(icon.dx(), icon.dy(), icon.dz());
            ItemDisplay disp = loc.getWorld().spawn(loc, ItemDisplay.class, id -> {
                id.setPersistent(false);
                id.setBillboard(Display.Billboard.CENTER);
                id.setItemStack(buildIconItem(icon));
                Transformation t = id.getTransformation();
                Transformation scaled = new Transformation(
                        t.getTranslation(),
                        t.getLeftRotation(),
                        new Vector3f(icon.scale(), icon.scale(), icon.scale()),
                        t.getRightRotation()
                );
                id.setTransformation(scaled);
                tag(id, HologramKeys.ROLE_ICON, idx);
            });
            icons.add(disp);
        }
    }

    private void despawnIcons() {
        for (ItemDisplay id : icons) {
            try { id.remove(); } catch (Throwable ignored) {}
        }
        icons.clear();
    }

    private ItemStack buildIconItem(IconSpec spec) {
        ItemStack stack = new ItemStack(spec.material());
        if (spec.enchanted()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    /**
     * Mojang's TextDisplay renders multi-line text with the BOTTOM of the FIRST line
     * pinned to the entity location — subsequent lines stack UPWARD. For a 9-line
     * leaderboard that means the visible text TOP is at entity_y + 9 × 0.25 = +2.25,
     * which doesn't match the DecentHolograms convention users have built their hub
     * coordinates around (DH treated the location Y as the visible top).
     *
     * <p>We pull the rendered text DOWN by exactly textHeight via a translation so the
     * visible top sits AT the entity location. An icon offset of `0.6` then always
     * means "0.6 blocks above the title line", regardless of how many lines a given
     * page has — the calculation is page-independent, so the icon doesn't visibly
     * drift when flipping between pages of different sizes.
     *
     * <p>0.25 is the per-line height Mojang uses at the default text scale (line
     * spacing + glyph height, 10 px / 40 px per block). It's not exposed via API.
     */
    private static final float LINE_HEIGHT_BLOCKS = 0.25f;

    private void applyTopAnchorTransform(TextDisplay td, Location anchor, int lineCount) {
        float textHeight = Math.max(1, lineCount) * LINE_HEIGHT_BLOCKS;
        float translationY = -textHeight;

        // The 4-arg Transformation constructor only accepts (Vec3, Quaternionf, Vec3,
        // Quaternionf) OR (Vec3, AxisAngle4f, Vec3, AxisAngle4f) — Paper rejects a mix.
        // Build the rotation as a Quaternionf from a yaw-around-Y AxisAngle.
        Transformation t = td.getTransformation();
        float yawRad = (float) Math.toRadians(anchor.getYaw());
        Quaternionf leftRot = new Quaternionf().fromAxisAngleRad(0f, 1f, 0f, yawRad);
        // Reset translation each call (don't compose) — page flips re-invoke this
        // with potentially different line counts and we want a clean overwrite.
        Vector3f translation = new Vector3f(0f, translationY, 0f);
        Transformation adjusted = new Transformation(
                translation,
                leftRot,
                t.getScale(),
                t.getRightRotation()
        );
        td.setTransformation(adjusted);
    }

    private Component composeText(Page page) {
        Component out = Component.empty();
        boolean first = true;
        for (String raw : page.lines()) {
            String resolved = resolvePlaceholders(raw);
            Component line = MM.deserialize(resolved);
            if (!first) out = out.append(Component.newline());
            out = out.append(line);
            first = false;
        }
        return out;
    }

    private String resolvePlaceholders(String raw) {
        PAPIHook hook = IP.getPlaceholderHook();
        if (hook == null) return raw;
        // PlaceholderAPI's setPlaceholders is null-player-safe but expects an OfflinePlayer.
        // For server-wide leaderboard text we don't have a viewer player at render time —
        // pass null and let PAPI resolve %witp_*% via its registered expansion which
        // doesn't need a player. The IP PAPI expansion already supports this.
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders((Player) null, raw);
    }

    private void tag(Entity entity, String role, int index) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(HologramKeys.HOLOGRAM_ID, PersistentDataType.STRING, config.id());
        pdc.set(HologramKeys.HOLOGRAM_ROLE, PersistentDataType.STRING, role);
        if (index >= 0) {
            pdc.set(HologramKeys.HOLOGRAM_INDEX, PersistentDataType.INTEGER, index);
        }
    }

    /**
     * Recognizes whether an Entity is one of OURS for the given hologram id. Used by
     * the chunk-load sweeper to find and kill orphans from a previous server lifetime.
     */
    public static boolean isOurs(Entity entity, String id) {
        String tagged = entity.getPersistentDataContainer().get(HologramKeys.HOLOGRAM_ID, PersistentDataType.STRING);
        return id.equals(tagged);
    }

    /** Same as {@link #isOurs(Entity, String)} but matches ANY IP hologram id. */
    public static boolean isAnyHologramEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(HologramKeys.HOLOGRAM_ID, PersistentDataType.STRING);
    }

    /**
     * Whether the entity's runtime type can be one of ours (cheap class-check to skip
     * the PDC lookup on the bulk of unrelated entities during chunk sweeps).
     */
    public static boolean isHologramEntityType(EntityType type) {
        return type == EntityType.TEXT_DISPLAY
                || type == EntityType.ITEM_DISPLAY
                || type == EntityType.INTERACTION;
    }
}
