package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.ip.foundation.inventory.Menu
import dev.efnilite.ip.foundation.util.Cooldowns
import dev.efnilite.ip.foundation.util.Task
import dev.efnilite.ip.player.ParkourUser
import org.bukkit.entity.Player

object PlayMenu {

    fun open(player: Player) {
        val menu = Menu(3, Locales.getString(player, "play.title"))
            .item(23, Locales.getItem(player, "go back").click({ player.closeInventory() }))
            .distributeRowsEvenly()

        for (mode in IEP.getModes()) {
            menu.item(9 + menu.items.size, mode.getItem(player)
                .click({
                    if (!Cooldowns.canPerform(player, "ep join", 1000)) {
                        return@click
                    }

                    val join = {
                        val ep = player.asElytraPlayer()

                        if (ep == null) {
                            ElytraPlayer(player).join(mode)
                        } else {
                            // Mode switch: keep the loaded settings so the new mode starts
                            // right away instead of waiting for another storage read.
                            val settings = runCatching { ep.getGenerator().settings }.getOrNull()

                            ep.leave(true)

                            ep.join(mode, settings)
                        }
                    }

                    // The block-parkour plugin is part of this jar now, so the old
                    // "is IP enabled" check was always true. Only leave a block-parkour
                    // session if the player is actually in one, through the API, and
                    // urgently (sync restore) so the elytra join captures the restored state.
                    // Done a tick later: teleporting inside an inventory click is unsafe.
                    if (ParkourUser.isUser(player)) {
                        IEP.log("Player is in block parkour, leaving it before joining mode")

                        Task.create(IEP.instance)
                            .delay(1)
                            .execute {
                                val user = ParkourUser.getUser(player)
                                if (user != null) {
                                    ParkourUser.unregister(user, true, false, true)
                                }
                                join()
                            }
                            .run()
                    } else {
                        join()
                    }
                }))
        }

        menu.open(player)
    }

}