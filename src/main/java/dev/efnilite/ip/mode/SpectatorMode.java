package dev.efnilite.ip.mode;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.foundation.inventory.item.Item;
import dev.efnilite.ip.foundation.util.Strings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorMode implements Mode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return null;
    }

    @Override
    @Nullable
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public void create(Player player) {
        Menus.SPECTATOR.open(player);
    }

    public void create(Player player, Session session) {
        if (!IP.getPlugin().isEnabled()) {
            return;
        }

        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage(Strings.colour("<red><bold>Joining is currently disabled."));
            return;
        }
        if (!ParkourOption.SPECTATOR.mayPerform(player)) {
            player.sendMessage(Locales.getString(player, "other.no_do"));
            return;
        }

        // respect the session's visibility, also for /ip join <player>: private sessions can't be
        // spectated, invite-only (ID_ONLY) sessions can be joined by name, and public sessions
        // follow the session's own spectator rule
        boolean refused = switch (session.getVisibility()) {
            case PRIVATE -> true;
            case ID_ONLY -> false;
            default -> !session.isAcceptingSpectators();
        };
        if (refused) {
            player.sendMessage(Locales.getString(player, "other.no_do"));
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (session.getPlayers().isEmpty()) {
            return;
        }

        if (user != null) {
            ParkourUser.unregister(user, false, false, false);
            spectator = new ParkourSpectator(player, session, user.previousData);
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }
}
