package dev.efnilite.iep

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.ResetReason
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.menu.LeaderboardMenu
import dev.efnilite.iep.menu.PlayMenu
import dev.efnilite.iep.menu.SettingsMenu
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.ip.foundation.command.ViCommand
import dev.efnilite.ip.foundation.schematic.Schematic
import dev.efnilite.ip.foundation.util.Cooldowns
import dev.efnilite.ip.foundation.util.Strings
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.io.File
import java.util.*

object Command : ViCommand() {

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player || args.isEmpty()) {
            with(sender) {
                send("")
                send("<dark_gray>= <gradient:#4800FF:#B200FF><bold>Infinite Elytra Parkour</bold></gradient> <dark_gray>=")
                send("")

                if (sender.hasTPermission("iep.play")) {
                    send("<#7700FF>/iep play <dark_gray>- <gray>Opens the play menu")
                }
                if (sender.hasTPermission("iep.leaderboard")) {
                    send("<#7700FF>/iep leaderboards <dark_gray>- <gray>Opens the leaderboards menu")
                }
                if (sender.hasTPermission("iep.setting")) {
                    send("<#7700FF>/iep settings <dark_gray>- <gray>Opens the settings menu")
                }
                if (sender.hasTPermission("iep.leave")) {
                    send("<#7700FF>/iep leave <dark_gray>- <gray>Leaves the game")
                }
                if (sender.hasTPermission("iep.setting.seed")) {
                    send("<#7700FF>/iep seed <seed> <dark_gray>- <gray>Set the current seed")
                }
                send("")

                if (sender.isOp) {
                    send("<#7700FF>/iep schematic <pos1> <pos2> <dark_gray>- <gray>Saves the area between the positions")
                    send("<gray>⋅ Example: /iep schematic 0,0,0 -10,-10,-10")
                    send("<#7700FF>/iep reset <player/uuid> [mode] <dark_gray>- <gray>Resets player scores for all or a specific mode")
                    send("<gray>⋅ Example: /iep reset Efnilite min speed")
                    send("")
                }
            }
            return true
        }

        when (args[0].lowercase()) {
            "play" -> {
                if (!sender.hasTPermission("iep.play")) {
                    return true
                }

                PlayMenu.open(sender)
            }
            "leaderboards" -> {
                if (!sender.hasTPermission("iep.leaderboard")) {
                    return true
                }

                LeaderboardMenu.open(sender)
            }
            "settings" -> {
                val ep = sender.asElytraPlayer() ?: return true

                if (!ep.hasPermission("iep.setting")) {
                    return true
                }

                SettingsMenu.open(ep)
            }
            "leave" -> {
                val ep = sender.asElytraPlayer() ?: return true

                if (!ep.hasPermission("iep.leave")) {
                    return true
                }

                ep.leave()
            }
            "seed" -> {
                if (!Cooldowns.canPerform(sender, "iep set seed", 1000)) {
                    return true
                }

                val iep = sender.asElytraPlayer() ?: return true

                try {
                    val seed = args[1].toInt()

                    if (seed < 0) throw NumberFormatException()

                    iep.getGenerator().set { settings -> Settings(settings, seed = seed) }
                    iep.getGenerator().reset(ResetReason.RESET, s = seed)

                    iep.send(Locales.getString(sender, "settings.seed.set").replace("%a", args[1]))
                } catch (ex: NumberFormatException) {
                    iep.send(Locales.getString(sender, "settings.seed.invalid").replace("%a", args[1]))
                }
            }
            "schematic" -> {
                if (!sender.isOp) return true

                try {
                    val pos1Nums = args[1].split(",").map { it.toInt() }
                    val pos2Nums = args[2].split(",").map { it.toInt() }

                    val pos1 = Vector(pos1Nums[0], pos1Nums[1], pos1Nums[2]).toLocation(sender.world)
                    val pos2 = Vector(pos2Nums[0], pos2Nums[1], pos2Nums[2]).toLocation(sender.world)

                    val uuid = UUID.randomUUID()
                    val file = File(IEP.dataFolder, "schematics/$uuid")

                    sender.send("<gray>Saving your schematic as $uuid")

                    Schematic.save(file, pos1, pos2, IEP.instance)
                } catch (ex: NumberFormatException) {
                    sender.send("<red>Invalid position format.")
                } catch (ex: IndexOutOfBoundsException) {
                    sender.send("<red>You need two positions to save the schematic.")
                }
            }
            "reset" -> {
                if (!sender.isOp) return true

                val uuid = try {
                    UUID.fromString(args[1])
                } catch (ex: IllegalArgumentException) {
                    Bukkit.getOfflinePlayer(args[1]).uniqueId
                }

                if (uuid == null) {
                    sender.send("<red>Unknown player.")
                    return true
                }

                if (args.size >= 3) {
                    val mode = IEP.getMode(args.drop(2).joinToString(" ").lowercase())

                    if (mode == null) {
                        sender.send("<red>Unknown mode.")
                        return true
                    }

                    mode.leaderboard.reset(uuid)
                    sender.send("<gray>Reset $uuid's scores for ${mode.name}.")
                    return true
                }

                for (mode in IEP.getModes()) {
                    mode.leaderboard.reset(uuid)
                }
                sender.send("<gray>Reset $uuid's scores.")
            }
            "debug-reset-invulnerability" -> {
                sender.isInvulnerable = false
                sender.send("<gray>Invulnerability reset.")
            }
            else -> {
                sender.send("<red>Invalid command.")
            }
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val list = mutableListOf<String>()

            if (sender.hasTPermission("iep.play")) list += "play"
            if (sender.hasTPermission("iep.leaderboard")) list += "leaderboards"
            if (sender.hasTPermission("iep.setting")) list += "settings"
            if (sender.hasTPermission("iep.leave")) list += "leave"
            if (sender.isOp) list.add("schematic")

            return list
        }

        return when (args[0].lowercase()) {
            "schematic" -> {
                if (!sender.isOp || sender !is Player) return emptyList()

                return when (args.size) {
                    2 -> {
                        val x = sender.location.blockX
                        val y = sender.location.blockY
                        val z = sender.location.blockZ

                        return listOf("$x,$y,$z")
                    }
                    3 -> {
                        val x = sender.location.blockX
                        val y = sender.location.blockY
                        val z = sender.location.blockZ

                        return listOf("$x,$y,$z")
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun CommandSender.send(message: String) = sendMessage(Strings.colour(message))

    private fun CommandSender.hasTPermission(permission: String): Boolean {
        if (Config.CONFIG.getBoolean("permissions")) {
            return hasPermission(permission)
        }

        return true
    }
}
