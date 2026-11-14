package dev.efnilite.ip.reward;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.hook.VaultHook;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * A class for handling reward commands, etc.
 */
// todo add cross-server support
// todo mode-specific way of saving one-time-rewards
public record Reward(@NotNull String string) {

    /**
     * Returns the name of the mode a reward string is meant for.
     *
     * @param reward      The reward string.
     * @param modeNames   The names of all registered modes.
     * @param defaultMode The name of the default mode, used when the reward has no mode prefix.
     * @return The lowercase name of the mode this reward is for.
     */
    static @NotNull String targetMode(@NotNull String reward, @NotNull Collection<String> modeNames, @NotNull String defaultMode) {
        return modeNames.stream()
                .map(String::toLowerCase)
                .filter(name -> reward.contains("%s:".formatted(name)))
                .findFirst()
                .orElse(defaultMode.toLowerCase());
    }

    /**
     * @param mode The mode.
     * @return True if this reward is executed in the given mode.
     */
    public boolean appliesTo(@NotNull Mode mode) {
        return !string.isEmpty() && targetMode(string, Registry.getModes().stream().map(Mode::getName).toList(), Modes.DEFAULT.getName())
                .equalsIgnoreCase(mode.getName());
    }

    /**
     * Parses and executes this reward
     *
     * @param player The player to which to give this reward to
     * @param mode   The mode the player is playing.
     * @return True if the reward applies to this mode and was executed (or scheduled for leaving).
     */
    public boolean execute(@NotNull ParkourPlayer player, @NotNull Mode mode) {
        if (!appliesTo(mode)) {
            return false;
        }

        String string = this.string;
        String modePrefix = "%s:".formatted(mode.getName().toLowerCase());

        if (string.contains(modePrefix)) {
            string = string.replaceFirst(modePrefix, "");
        }

        // check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", player.getName());
        }

        // check for extra data
        if (string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            // keep the mode prefix, so the reward still applies to this mode when it is executed on leave
            player.previousData.onLeave.add(new Reward(mode == Modes.DEFAULT ? string : modePrefix + string));
            return true;
        }

        // check for things to perform
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            player.send(string);
        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            try {
                VaultHook.deposit(player.player, Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                IP.logging().stack("Error while trying to process Vault reward", "check your rewards file for incorrect numbers", ex);
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }

        return true;
    }
}