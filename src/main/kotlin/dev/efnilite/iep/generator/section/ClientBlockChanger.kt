package dev.efnilite.iep.generator.section

import dev.efnilite.iep.IEP
import dev.efnilite.iep.style.Style
import dev.efnilite.iep.world.World
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

/**
 * Pipes pending client-side block updates to the player without leaking server memory.
 *
 * Storage: [Vector] positions rather than [org.bukkit.block.Block]. Block lookup forces
 * a chunk load and pins the chunk in Paper's holder; we'd be loading thousands of chunks
 * per minute as the player flies. Vectors are 24 bytes and pin nothing.
 *
 * Render: at SEND TIME we resolve `v.toLocation(world).block.state`, set its type to the
 * style colour, and ship a batched MultiBlockChange via [Player.sendBlockChanges]. This
 * is exactly the upstream-IEP render path — only the storage tier changed.
 *
 * Concurrency: `queue()` runs from an async continuation; `check`/`flush`/`clear` from
 * the main thread. All four synchronize on `this`; network/state work happens outside.
 */
class ClientBlockChanger {

    private val toChange: MutableMap<Int, MutableSet<Vector>> = mutableMapOf()

    fun check(player: Player, style: Style) {
        val forwardLimit = forwardLimit(player)

        val toSend = mutableListOf<Pair<Int, Set<Vector>>>()
        synchronized(this) {
            val drop = mutableListOf<Int>()
            for ((cx, _) in toChange) {
                if (cx <= forwardLimit) drop += cx
            }
            for (cx in drop) {
                val vectors = toChange.remove(cx) ?: continue
                toSend += cx to vectors.toSet()
            }
        }

        if (toSend.isEmpty()) return
        renderBatches(player, toSend, style)
    }

    fun queue(new: Map<Int, Set<Vector>>) {
        queueIf(new) { true }
    }

    /**
     * Queues [new] only if [condition] holds. The condition is evaluated under the same lock
     * as [clear], so a reset that bumps its generation counter before calling [clear] can
     * never be undone by a late async completion.
     */
    fun queueIf(new: Map<Int, Set<Vector>>, condition: () -> Boolean) {
        synchronized(this) {
            if (!condition()) {
                IEP.log("Dropped stale chunks ${new.keys.toTypedArray().contentToString()}")
                return
            }

            IEP.log("Queued chunks ${new.keys.toTypedArray().contentToString()}")

            new.forEach { (x, vectors) ->
                toChange.getOrPut(x) { mutableSetOf() }.addAll(vectors)
            }
        }
    }

    /**
     * Drains and sends everything currently queued. Used by [Generator.applyAscendingBoost]
     * right before the firework push so the boosted-glide path is fully rendered before
     * the player flies through it. Far-ahead chunks the client doesn't have yet will
     * see their block-change packets dropped, but that's already the worst-case for
     * normal `check()` too — and on boost we'd rather over-send than leave the player
     * flying into emptiness.
     */
    fun flush(player: Player, style: Style) {
        val toSend = mutableListOf<Pair<Int, Set<Vector>>>()
        synchronized(this) {
            val drop = ArrayList(toChange.keys)
            for (cx in drop) {
                val vectors = toChange.remove(cx) ?: continue
                toSend += cx to vectors.toSet()
            }
        }
        if (toSend.isEmpty()) return
        renderBatches(player, toSend, style)
    }

    fun clear() {
        synchronized(this) {
            toChange.clear()
        }
    }

    /**
     * Sends a fixed-material spoof for the given positions (used by ObstacleGenerator
     * to draw / un-draw obstacle blocks without dirtying the server world).
     */
    fun sendNow(player: Player, vectors: Collection<Vector>, material: Material) {
        if (vectors.isEmpty()) return
        val world = World.world
        val states = ArrayList<BlockState>(vectors.size)
        for (v in vectors) {
            val state = v.toLocation(world).block.state
            state.type = material
            states += state
        }
        player.sendBlockChanges(states)
    }

    companion object {

        /**
         * @return The furthest chunk X (inclusive) that should be rendered for [player].
         */
        fun forwardLimit(player: Player): Int {
            val playerCx = player.location.blockX shr 4

            // clientViewDistance returns -1 if Paper hasn't yet received the client's VD
            // packet (common in the first second after a fresh join). Fall back to the
            // server's per-world value so forwardLimit doesn't collapse to "behind player".
            val rawVd = player.clientViewDistance
            val vd = if (rawVd > 0) rawVd else max(4, player.world.viewDistance)
            // Render up to ~min(vd, 8) chunks ahead. Tight on min so we don't burn packets
            // far past where the player can see; generous enough that boost-time velocity
            // doesn't outrun the render window.
            return playerCx + min(vd, 8)
        }
    }

    /**
     * One MultiBlockChange packet per chunk-X batch. `Block.getState()` forces the chunk
     * loaded server-side (no-op if already loaded — by send time the player is gliding
     * through it). `state.type = style.next()` per-block preserves the upstream behaviour
     * where the style cycle advances per position, which is what produces the visible
     * spiral pattern on IncrementalStyle.
     */
    private fun renderBatches(player: Player, batches: List<Pair<Int, Set<Vector>>>, style: Style) {
        val world = World.world
        for ((cx, vectors) in batches) {
            if (vectors.isEmpty()) continue
            IEP.log("Displaying ${vectors.size} positions at chunkX=$cx")
            val states = ArrayList<BlockState>(vectors.size)
            for (v in vectors) {
                val state = v.toLocation(world).block.state
                state.type = style.next()
                states += state
            }
            player.sendBlockChanges(states)
        }
    }
}
