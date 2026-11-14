package dev.efnilite.ip.foundation.command;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class to wrap commands, which makes it a lot easier to produce them.
 *
 * @author Efnilite
 */
public abstract class ViCommand implements CommandExecutor, TabCompleter {

    /**
     * UUID-based cooldown system
     */
    private final Map<UUID, List<CommandCooldown>> cooldowns = new HashMap<>();

    /**
     * Execute a command
     */
    public abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Get what should be suggested
     */
    public abstract List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Checks the cooldown
     *
     * @param sender     The CommandSender which may have a cooldown
     * @param arg        The argument to which this cooldown applies
     * @param cooldownMs The cooldown in ms
     * @return false if the cooldown is not over yet, true if it has been.
     */
    protected boolean cooldown(CommandSender sender, String arg, long cooldownMs) {
        if (!(sender instanceof Player player)) { // ignore console, rcon, command blocks etc. (no UUID)
            return true;
        }

        UUID uuid = player.getUniqueId();

        CommandCooldown cooldown; // the current cooldown
        List<CommandCooldown> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            playerCooldowns = new ArrayList<>();
            playerCooldowns.add(new CommandCooldown(System.currentTimeMillis(), arg));
            cooldowns.put(uuid, playerCooldowns);
            return true;
        }

        // get the appropriate commandcooldown class
        cooldown = playerCooldowns.stream()
                .filter(plCooldown -> plCooldown.arg().equals(arg))
                .findFirst().orElse(null);

        if (cooldown == null) { // cooldown doesnt exist yet
            playerCooldowns.add(new CommandCooldown(System.currentTimeMillis(), arg));
            cooldowns.put(uuid, playerCooldowns);
            return true;
        }

        if (System.currentTimeMillis() - cooldown.lastExecuted() <= cooldownMs) {
            return false;
        }

        playerCooldowns.remove(cooldown); // update cooldown
        playerCooldowns.add(new CommandCooldown(System.currentTimeMillis(), arg));
        cooldowns.put(uuid, playerCooldowns);

        return true;
    }

    /**
     * Gets completions in relation to what the user has already typed
     *
     * @param typed    What the player has typed so far
     * @param possible The possible completions
     * @return the updated possible completions
     */
    protected List<String> completions(String typed, List<String> possible) {
        return possible.stream().filter(option -> option.toLowerCase().contains(typed)).toList();
    }

    /**
     * Gets completions in relation to what the user has already typed
     *
     * @param typed    What the player has typed so far
     * @param possible The possible completions
     * @return the updated possible completions
     */
    protected List<String> completions(String typed, String... possible) {
        return Arrays.stream(possible).filter(option -> option.toLowerCase().contains(typed)).toList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return tabComplete(sender, args);
    }

    /**
     * @param arg          The argument
     * @param lastExecuted When the command with arg was last executed
     */
    private record CommandCooldown(long lastExecuted, String arg) {

    }
}