package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Registers stuff.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Registry {

    private static final LinkedList<Mode> modes = new LinkedList<>();
    private static final LinkedList<Style> styles = new LinkedList<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        modes.add(mode);

        IP.log("Registered mode %s".formatted(mode.getName()));
    }

    public static void register(@NotNull Style style) {
        // replace a style with the same name (e.g. on /ip reload) instead of adding a stale duplicate
        int existing = indexOfStyle(styles, style.getName());
        if (existing >= 0) {
            styles.set(existing, style);
        } else {
            styles.add(style);
        }

        IP.log("Registered style %s".formatted(style.getName()));
    }

    static int indexOfStyle(List<Style> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return styles.stream()
                .filter(style -> style.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static List<Style> getStyles() {
        return styles;
    }

    public static List<Mode> getModes() {
        return modes;
    }
}