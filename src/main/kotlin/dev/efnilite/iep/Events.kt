package dev.efnilite.iep

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.ResetReason
import dev.efnilite.iep.menu.LeaderboardMenu
import dev.efnilite.iep.menu.PlayMenu
import dev.efnilite.iep.menu.SettingsMenu
import dev.efnilite.iep.mode.DefaultMode
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.iep.player.PreviousData
import dev.efnilite.iep.world.World
import dev.efnilite.ip.foundation.event.EventWatcher
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot

object Events : EventWatcher {

    @EventHandler
    fun drop(event: PlayerDropItemEvent) {
        val player = event.player.asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun quit(event: PlayerQuitEvent) {
        val player = event.player.asElytraPlayer() ?: return

        player.leave(urgent = true)
    }

    @EventHandler
    fun change(event: PlayerChangedWorldEvent) {
        val player = event.player.asElytraPlayer() ?: return

        if (event.player.world == World.world) return

        player.leave()
    }

    /**
     * Puts back the state of a player who was in elytra parkour when the server crashed.
     * LOWEST so it runs before join-on-join and other plugins' join handling.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun restore(event: PlayerJoinEvent) {
        PreviousData.restoreLeftover(event.player)
    }

    @EventHandler
    fun join(event: PlayerJoinEvent) {
        if (Config.CONFIG.getBoolean("join-on-join")) {
            ElytraPlayer(event.player).join(DefaultMode)
        }
    }

    @EventHandler
    fun rightRocket(event: BlockPlaceEvent) {
        val player = event.player.asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun inventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        if (event.whoClicked.openInventory.type == InventoryType.CRAFTING) return
        val player = (event.whoClicked as Player).asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun damage(event: EntityDamageEvent) {
        if (event.entity !is Player) return

        val player = (event.entity as Player).asElytraPlayer() ?: return

        event.isCancelled = true

        if (event.cause == EntityDamageEvent.DamageCause.VOID) {
            player.getGenerator().reset(ResetReason.BOUNDS)
        }
    }

    @EventHandler
    fun rightSettings(event: PlayerInteractEvent) {
        if ((event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) ||
            event.hand != EquipmentSlot.HAND) return

        val player = event.player.asElytraPlayer() ?: return

        val play = Locales.getString(player, "hotbar.play.material").lowercase()
        val settings = Locales.getString(player, "hotbar.settings.material").lowercase()
        val leaderboard = Locales.getString(player, "hotbar.leaderboards.material").lowercase()
        val leave = Locales.getString(player, "hotbar.leave.material").lowercase()

        when (event.item?.type?.name?.lowercase()) {
            play -> {
                if (player.hasPermission("iep.play")) {
                    PlayMenu.open(player.player)
                }
            }
            settings -> {
                if (player.hasPermission("iep.setting")) {
                    SettingsMenu.open(player)
                }
            }
            leaderboard -> {
                if (player.hasPermission("iep.leaderboard")) {
                    LeaderboardMenu.open(player.player)
                }
            }
            leave -> {
                if (player.hasPermission("iep.leave")) {
                    player.leave()
                }
            }

            else -> {}
        }
    }
}