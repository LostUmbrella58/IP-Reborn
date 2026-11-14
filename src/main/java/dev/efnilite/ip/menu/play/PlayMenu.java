package dev.efnilite.ip.menu.play;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.foundation.inventory.Menu;
import org.bukkit.entity.Player;

/**
 * The menu where players can join modes
 */
public class PlayMenu extends DynamicMenu {

    public PlayMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.single.item")
                        .click(event -> Menus.SINGLE.open(event.getPlayer())),
                ParkourOption.SINGLE::mayPerform);

        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "other.iep")
                        .click(event -> event.getPlayer().performCommand("iep play")),
                // elytra parkour is bundled in this jar now, so there is no separate "IEP" plugin to look for
                player -> IP.getPlugin() != null && IP.getPlugin().isElytraEnabled());

        registerMainItem(1, 6, (player, user) -> Locales.getItem(player, "play.spectator.item")
                        .click(event -> Menus.SPECTATOR.open(event.getPlayer())),
                ParkourOption.SPECTATOR::mayPerform);

        registerMainItem(2, 0, (player, user) -> Locales.getItem(player, "other.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    public void open(Player player) {
        display(player, new Menu(3, Locales.getString(player, "play.name"))
                .distributeRowsEvenly());
    }
}