package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.event.ParkourSpectateEvent;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.foundation.util.Task;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Class for spectators of a Session.
 *
 * @author Efnilite
 */
public class ParkourSpectator extends ParkourUser {

    private final BukkitTask closestChecker;
    private boolean madeNonCollidable = false;
    /**
     * The closest player.
     */
    @NotNull
    public ParkourPlayer closest;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        this.closest = session.getPlayers().get(0);

        new ParkourSpectateEvent(this).call();

        Task.create(IP.getPlugin())
            .delay(1)
            .execute(() -> {
                // the spectator may have left within this tick; don't put them back in spectator mode
                if (!player.isOnline() || ParkourUser.getUser(player) != this) {
                    return;
                }

                teleport(closest.getLocation());

                sendTranslated("play.spectator.join");

                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                if (ParkourUser.isBedrockPlayer(player)) {  // bedrock has no spectator mode, so just make the player invisible
                    player.setInvisible(true);
                    if (player.isCollidable()) {
                        player.setCollidable(false);
                        madeNonCollidable = true;
                    }
                }
            })
            .run();

        // runs on the main thread: it reads player locations and session collections that are
        // only safe to touch from the server thread
        closestChecker = Task.create(IP.getPlugin())
            .delay(1)
            .repeat(10)
            .execute(() -> {
                if (session.getPlayers().isEmpty()) {
                    return;
                }

                closest = session.getPlayers().stream()
                        .min(Comparator.comparing(other -> other.getLocation().distanceSquared(player.getLocation()))) // x or x^2 doesn't matter in getting smallest
                        .orElse(closest);
            })
            .run();
    }

    /**
     * Updates the spectator's action bar, scoreboard and checks distance.
     */
    public void update() {
        // Paper 26: Bungee Chat's TextComponent.fromLegacyText is gone. Deserialize the raw
        // locale string straight through MiniMessage so we keep MiniMessage tag support.
        // (Legacy "&"-color strings still survive via Strings.colour → legacy serializer pipeline,
        // but the action bar path doesn't need that compatibility layer.)
        player.sendActionBar(MiniMessage.miniMessage().deserialize(Locales.getString(player, "play.spectator.action_bar")));
        player.setGameMode(GameMode.SPECTATOR);
        updateScoreboard(session.generator);

        // spectator is still being teleported to world
        if (closest.getLocation().getWorld() != player.getLocation().getWorld()) {
            return;
        }

        if (closest.getLocation().distanceSquared(player.getLocation()) < 100 * 100) { // avoid sqrt
            return;
        }

        teleport(closest.getLocation());
        if (player.getGameMode() != GameMode.SPECTATOR) { // if player isn't in spectator or is a bedrock player
            return;
        }

        // if player is a spectator
        player.setSpectatorTarget(null);
        player.setSpectatorTarget(player.getSpectatorTarget());
    }

    /**
     * Stops the closest checker runnable.
     */
    @Override
    public void unregister() {
        closestChecker.cancel();
        session.removeSpectators(this);
        player.setInvisible(false);
        if (madeNonCollidable) {
            player.setCollidable(true);
            madeNonCollidable = false;
        }
    }
}