package dev.efnilite.ip.foundation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.efnilite.ip.foundation.command.ViCommand;
import dev.efnilite.ip.foundation.inventory.Menu;
import dev.efnilite.ip.foundation.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ParkourPlugin extends JavaPlugin {

    protected static Gson gson;
    protected static Version version;

    /**
     * @return a Gson instance which has already been set up.
     */
    public static Gson getGson() {
        return gson;
    }

    /**
     * @return The version.
     */
    public static Version getVersion() {
        return version;
    }

    @Override
    public void onEnable() {
        version = Version.getVersion();

        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();

        enable();
    }

    @Override
    public void onDisable() {
        // close open menus first, so nothing can be taken out of them once their listeners are gone
        try {
            Menu.closeAll();
        } catch (Throwable t) {
            getLogger().warning("Failed to close open menus: " + t);
        }

        disable();

        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    /**
     * What happens on enable of the plugin inheriting this library.
     */
    public abstract void enable();

    /**
     * What happens on disable of the plugin inheriting this library.
     * Disabling will automatically close all open menus, cancel all active tasks and unregister all EventWatchers.
     */
    public abstract void disable();

    /**
     * Register a command to this plugin.
     *
     * @param name    The name of the command in plugin.yml
     * @param command The command class
     */
    public void registerCommand(String name, ViCommand command) {
        var cmd = getCommand(name);

        if (cmd == null) return;

        cmd.setExecutor(command);
        cmd.setTabCompleter(command);
    }

    /**
     * Registers a Listener, with this plugin as its owner.
     *
     * @param listener The listener to register.
     * @see dev.efnilite.ip.foundation.event.EventWatcher
     */
    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
