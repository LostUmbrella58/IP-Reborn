package dev.efnilite.ip.foundation.util;

import org.bukkit.Bukkit;

import java.util.Arrays;

/**
 * Version class, useful for checking whether certain features on legacy can be executed.
 *
 * Paper 26 note: Bukkit.getBukkitVersion() now returns the Paper API version (e.g.
 * "26.1.2-R0.1-SNAPSHOT" or "26.1.2.build.72-stable") rather than the legacy
 * "1.21.x-R0.1-SNAPSHOT" string. We now resolve the MC version through
 * {@link Bukkit#getMinecraftVersion()} (available since 1.20-ish) and fall back to
 * parsing getBukkitVersion() only if that throws.
 *
 * @author Efnilite
 */
public enum Version {

    V1_16(16), V1_17(17), V1_18(18), V1_19(19), V1_20(20), V1_20_5(20, 5),
    V1_21(21), V1_22(22);

    public static Version VERSION;
    public final int major;
    public final int minor;

    Version(int major) {
        this.major = major;
        this.minor = 0;
    }

    Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Returns whether the version is higher or equal to a given version.
     *
     * @param compareTo The version to compare to
     * @return true if the current version is higher or equal to the given version, false if not
     */
    public static boolean isHigherOrEqual(Version compareTo) {
        if (VERSION.major == compareTo.major) {
            return VERSION.minor >= compareTo.minor;
        } else {
            return VERSION.major > compareTo.major;
        }
    }

    /**
     * Get the current Minecraft version as a String which can be displayed to users
     * (e.g. "1.21.11"). Always returns the MC version, never the API/Paper version.
     *
     * @return the pretty version as a String
     */
    public static String getPrettyVersion() {
        // Paper 26: getBukkitVersion() reports the API version ("26.1.2..."), so we
        // prefer getMinecraftVersion() which always returns the MC version. Some older
        // Paper builds lack the method; in that case fall back to the legacy parse,
        // which still works because the old format DID embed "1.X.Y".
        try {
            String mc = Bukkit.getMinecraftVersion();
            if (mc != null && !mc.isEmpty()) {
                return mc;
            }
        } catch (NoSuchMethodError ignored) {
            // pre-1.20.2 — fall through
        }
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /**
     * Returns the current version as an instance of this enum. Falls back to the
     * highest known enum value if the running server is newer than this enum knows
     * about — important on Paper 26 where the API version no longer starts with "1.".
     *
     * @return the version.
     */
    public static Version getVersion() {
        VERSION = resolve(getPrettyVersion());
        return VERSION;
    }

    /**
     * Resolves feature gates without requiring a running server. Calendar versions
     * such as 26.3 (including prereleases and Paper build suffixes) use the newest
     * known feature path; the enum's legacy names are not displayed as MC versions.
     */
    static Version resolve(String pretty) {
        var parts = pretty.split("\\.");

        // Try to parse standard "1.MAJOR[.MINOR]" first. If parts[0] is not "1", we
        // assume the server is newer than the highest known enum entry and clamp.
        int major;
        int minor;
        try {
            if (parts.length >= 2 && parts[0].equals("1")) {
                major = Integer.parseInt(parts[1]);
                minor = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            } else {
                // Format we don't recognize (Paper 26+'s 26.1.2-style or future MC
                // numbering). Clamp to the latest known enum entry — for vilib's
                // purposes (gating feature use) "newer than I know about" is the same
                // as "use the newest path."
                return Version.values()[Version.values().length - 1];
            }
        } catch (NumberFormatException ex) {
            return Version.values()[Version.values().length - 1];
        }

        final int finalMajor = major;
        final int finalMinor = minor;
        var lowerVersions = Arrays.stream(Version.values()).filter(version -> {
            if (version.major == finalMajor) {
                return version.minor <= finalMinor;
            } else {
                return version.major < finalMajor;
            }
        }).toList();

        if (lowerVersions.isEmpty()) {
            // Server is older than every known entry — fall back to the oldest one
            // rather than crashing on get(-1). This path only triggers on
            // pre-1.16 servers, which we never officially supported anyway.
            return Version.values()[0];
        } else {
            return lowerVersions.get(lowerVersions.size() - 1);
        }
    }

    /**
     * Gets the internal version from the Bukkit package.
     * Format: "v1_20_R1"
     *
     * @return the internal version with format "v1_20_R1"
     */
    @Deprecated(forRemoval = true)
    public static String getInternalVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }
}
