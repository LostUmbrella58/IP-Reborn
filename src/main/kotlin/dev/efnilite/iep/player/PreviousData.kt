package dev.efnilite.iep.player

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.iep.reward.Reward
import dev.efnilite.iep.storage.AtomicFiles
import dev.efnilite.iep.world.World
import dev.efnilite.ip.foundation.inventory.Menu
import dev.efnilite.ip.foundation.inventory.item.Item
import dev.efnilite.ip.foundation.util.Task
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Class for storing a player's previous data.
 *
 * Paper 26 port: PaperLib.teleportAsync replaced with native Player.teleportAsync.
 * Equipment setters use the explicit Java methods (setChestplate, setContents) —
 * Kotlin 2.4 refuses to synthesize a property over a getter/setter pair whose
 * nullability annotations disagree, which is the case in Paper 26's PlayerInventory.
 *
 * Inventory mutations are forced onto the main thread via an `onMain` runner.
 * Paper's `teleportAsync(...).thenRun(...)` continuation thread varies — sometimes
 * the chunk-loader thread, sometimes the main thread — and inventory operations on
 * a non-main thread are silently dropped on modern Paper. That was the cause of
 * the "elytra not given on rejoin" symptom users reported: the first join almost
 * always ran setup on main, but a rejoin happened to hit the async continuation
 * path and the setChestplate call no-op'd.
 *
 * Crash safety: before the player's state is replaced, it is also written to
 * `elytra/restore/<uuid>.yml`. A normal leave deletes that file; if the server dies
 * while the player is in elytra parkour, [restoreLeftover] puts their state back the
 * next time they log in.
 */
data class PreviousData(private val player: Player) {

    val leaveRewards: MutableMap<Mode, MutableSet<Reward>> = mutableMapOf()

    private val foodLevel = player.foodLevel
    private val saturation = player.saturation
    private val flying = player.isFlying
    private val allowFlight = player.allowFlight

    private val gamemode = player.gameMode
    private val position = player.location
    private val inventoryContents: Array<ItemStack?> = player.inventory.contents
    private val effects: Collection<PotionEffect> = player.activePotionEffects

    /**
     * Teleports the player into the IEP world, then sets up their inventory + state.
     * The inventory setup is guaranteed to run on the main thread regardless of where
     * the teleport future completes — see class doc.
     *
     * @param shouldContinue Checked on the main thread once the teleport finishes. If it
     * returns false (the player left in the meantime) nothing is applied.
     * @return A future completed on the main thread: true if the player was set up, false
     * if the teleport failed or setup was skipped; exceptionally if setup threw.
     */
    fun setup(vector: Vector, shouldContinue: () -> Boolean = { true }): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        persist()

        player.teleportAsync(vector.toLocation(World.world)).whenComplete { teleported, ex ->
            onMain {
                if (!shouldContinue()) {
                    // The player left while the teleport was pending, and leave() already
                    // restored them. If the late teleport still moved them into the IEP
                    // world (and they didn't start another mode meanwhile), send them back.
                    if (player.isOnline && player.world == World.world && player.asElytraPlayer() == null) {
                        player.teleportAsync(position)
                    }
                    future.complete(false)
                    return@onMain
                }

                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@onMain
                }
                if (teleported != true) {
                    future.complete(false)
                    return@onMain
                }

                try {
                    applySetup()
                    future.complete(true)
                } catch (t: Throwable) {
                    future.completeExceptionally(t)
                }
            }
        }

        return future
    }

    private fun applySetup() {
        player.gameMode = GameMode.ADVENTURE
        player.fallDistance = 0F

        player.resetPlayerTime()
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        player.foodLevel = 20
        player.saturation = 20F
        player.isFlying = false
        player.allowFlight = false

        // Wipe everything — main inventory, off-hand, AND armor slots. Inventory.clear()
        // does cover armor on Paper 26, but a few setups (e.g. CMI's "vanish kit", some
        // anti-cheat preload, GameStack inventory mirroring) re-populate the chestplate
        // on the next tick. Calling setHelmet/Chestplate/Leggings/Boots(null) explicitly
        // ensures the slot is empty at the exact moment we then setChestplate(elytra) —
        // no race window where something else can put a curse-of-binding item in first.
        player.inventory.clear()
        player.inventory.setHelmet(null)
        player.inventory.setChestplate(null)
        player.inventory.setLeggings(null)
        player.inventory.setBoots(null)

        val elytra = Item(Material.ELYTRA, "").unbreakable().build()
        player.inventory.setChestplate(elytra)

        val items = mutableListOf<ItemStack>()
        items += Locales.getItem(player, "hotbar.play").build()
        items += Locales.getItem(player, "hotbar.settings").build()
        items += Locales.getItem(player, "hotbar.leaderboards").build()
        items += Locales.getItem(player, "hotbar.leave").build()

        Menu.getEvenlyDistributedSlots(items.size).forEachIndexed { index, slot ->
            player.inventory.setItem(slot, items[index])
        }
    }

    /**
     * Resets the player's data.
     * @param switchMode True when the player is only switching modes: their state is
     * restored and leave rewards are paid, but the crash-restore file is kept because the
     * next mode's setup follows immediately.
     */
    fun reset(switchMode: Boolean, urgent: Boolean) {
        if (switchMode) {
            onMain { resetInner(final = false) }
            return
        }
        if (urgent) {
            player.teleport(position)
            onMain { resetInner(final = true) }
            return
        }

        player.teleportAsync(position).thenRun {
            onMain { resetInner(final = true) }
        }
    }

    private fun resetInner(final: Boolean) {
        // The player disconnected while the leave teleport was pending. Changes to this
        // stale Player object would be lost, so keep the crash-restore file and the leave
        // rewards for restoreLeftover on their next login.
        if (!player.isOnline) {
            IEP.log("Skipping restore for offline player ${player.name}; it will be restored on next login")
            return
        }

        player.fallDistance = 0F

        player.gameMode = gamemode
        player.inventory.setContents(inventoryContents)

        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        player.addPotionEffects(effects)

        player.foodLevel = foodLevel
        player.saturation = saturation
        player.allowFlight = allowFlight
        player.isFlying = flying

        player.resetPlayerTime()

        // State is back — the crash-restore copy is no longer needed. Delete it before
        // running rewards so a failing reward can't cause a second restore later.
        if (final) {
            deletePersisted(player.uniqueId)
        }

        // Leave rewards are paid once, on the final leave. Paying them on a mode switch
        // would lose item rewards, because the next mode's setup clears the inventory.
        if (!final) {
            return
        }
        val rewards = leaveRewards.toMap()
        leaveRewards.clear()
        for ((mode, set) in rewards) {
            for (reward in set) {
                try {
                    reward.execute(player, mode)
                } catch (t: Throwable) {
                    IEP.logging.stack("Error while giving leave reward ${reward.string} to ${player.name}", t)
                }
            }
        }
    }

    /**
     * Writes the state captured at construction to the crash-restore file. Done
     * synchronously on purpose: the file must exist before the inventory is replaced, and
     * an async write could land after the delete on leave and resurrect stale data.
     */
    private fun persist() {
        try {
            val yaml = YamlConfiguration()

            yaml.set("world", position.world?.name)
            yaml.set("x", position.x)
            yaml.set("y", position.y)
            yaml.set("z", position.z)
            yaml.set("yaw", position.yaw.toDouble())
            yaml.set("pitch", position.pitch.toDouble())
            yaml.set("gamemode", gamemode.name)
            yaml.set("food", foodLevel)
            yaml.set("saturation", saturation.toDouble())
            yaml.set("allow-flight", allowFlight)
            yaml.set("flying", flying)
            yaml.set("effects", effects.toList())
            yaml.set("size", inventoryContents.size)
            inventoryContents.forEachIndexed { slot, item ->
                if (item != null && !item.type.isAir && item.amount > 0) {
                    yaml.set("inventory.$slot", Base64.getEncoder().encodeToString(item.serializeAsBytes()))
                }
            }

            AtomicFiles.write(restoreFile(player.uniqueId), yaml.saveToString())
        } catch (t: Throwable) {
            IEP.logging.stack("Failed to write crash-restore data for ${player.name}", t)
        }
    }

    /**
     * Routes [r] to the main server thread. Use for any operation that touches the
     * player's inventory or world state — Paper's teleportAsync continuation runs on
     * whatever thread the chunk loader handed control back on, and inventory writes
     * from that thread are silently dropped.
     */
    private fun onMain(r: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            r()
        } else {
            Task.create(IEP.instance).execute(Runnable { r() }).run()
        }
    }

    companion object {

        private fun restoreFile(uuid: UUID): File = IEP.dataFolder.resolve("restore/$uuid.yml")

        private fun deletePersisted(uuid: UUID) {
            val file = restoreFile(uuid)
            if (file.exists() && !file.delete()) {
                IEP.logging.error("Could not delete crash-restore file ${file.path}; delete it manually " +
                        "or the player's pre-parkour state will be restored again on their next login")
            }
        }

        /**
         * Restores state left behind by a crash or failed shutdown while [player] was in
         * elytra parkour. Must run on the main thread, before anything else touches the
         * player's inventory on join.
         * @return True if leftover state was found and restored.
         */
        fun restoreLeftover(player: Player): Boolean {
            val file = restoreFile(player.uniqueId)
            if (!file.exists()) return false

            try {
                val yaml = YamlConfiguration.loadConfiguration(file)
                val gamemode = GameMode.valueOf(yaml.getString("gamemode")
                    ?: throw IllegalStateException("file is empty or unreadable"))

                val contents = arrayOfNulls<ItemStack>(yaml.getInt("size", player.inventory.contents.size))
                yaml.getConfigurationSection("inventory")?.getKeys(false)?.forEach { key ->
                    val slot = key.toIntOrNull() ?: return@forEach
                    val encoded = yaml.getString("inventory.$key") ?: return@forEach
                    if (slot in contents.indices) {
                        contents[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded))
                    }
                }

                player.inventory.setContents(contents)
                player.gameMode = gamemode

                player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
                player.addPotionEffects(yaml.getList("effects")?.filterIsInstance<PotionEffect>() ?: emptyList())

                player.foodLevel = yaml.getInt("food", 20)
                player.saturation = yaml.getDouble("saturation", 5.0).toFloat()
                player.allowFlight = yaml.getBoolean("allow-flight")
                player.isFlying = player.allowFlight && yaml.getBoolean("flying")
                player.fallDistance = 0F
                player.resetPlayerTime()

                val world = yaml.getString("world")?.let { Bukkit.getWorld(it) }
                if (world != null) {
                    player.teleportAsync(Location(world, yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                        yaml.getDouble("yaw").toFloat(), yaml.getDouble("pitch").toFloat()))
                }
            } catch (t: Throwable) {
                IEP.logging.stack("Failed to restore ${player.name}'s state from before elytra parkour; " +
                        "the data is kept in ${file.path}", t)
                return false
            }

            deletePersisted(player.uniqueId)
            IEP.logging.info("Restored ${player.name}'s inventory and state from an interrupted elytra parkour session")
            return true
        }
    }
}
