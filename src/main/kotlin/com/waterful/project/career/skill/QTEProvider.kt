package com.waterful.project.career.skill

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.roundToInt

/**
 * QTE (Quick Time Event) system — ported from StarLightCore.
 *
 * Displays a scrolling action-bar bar with a success interval.
 * Player responds by sneaking (Shift) or swapping offhand (F key).
 *
 * @author IceBear003 (StarLightCore) — adapted to Pure Bukkit
 */
object QTEProvider : Listener {

    /** Waiting queue: if a player is already QTE-ing, subsequent QTEs are queued */
    private val waitingQTEs = mutableMapOf<UUID, MutableList<() -> Unit>>()

    /** Whether the player has responded to the current QTE */
    private val responseMap = mutableMapOf<UUID, Boolean>()

    /** Active QTE tasks per player — allows cancellation */
    private val activeTasks = mutableMapOf<UUID, BukkitTask>()

    private var plugin: JavaPlugin? = null
    private var registered = false

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin)
            registered = true
        }
    }

    fun sendQTE(
        player: Player,
        difficulty: QTEDifficulty,
        type: QTEType,
        function: Player.(result: QTEResult) -> Unit,
        title: String = "",
        subtitle: String = "在读条合适时机§e下蹲§7或§e交换副手物品"
    ) {
        if (isQTEing(player)) {
            waitingQTEs.putIfAbsent(player.uniqueId, mutableListOf())
            waitingQTEs[player.uniqueId]!! += { sendQTE(player, difficulty, type, function, title, subtitle) }
            return
        }

        player.showTitle(Title.title(
            Component.text(title),
            Component.text(subtitle),
            Title.Times.times(Ticks.duration(10), Ticks.duration(40), Ticks.duration(10))
        ))

        val uuid = player.uniqueId
        responseMap[uuid] = false

        // Bar format: 100 characters, colored segments
        val total = 100
        val period = difficulty.period
        val interval = difficulty.interval

        // Randomize interval start position
        var intervalStart = ((0.2 + Math.random() * 0.6) * total).roundToInt()

        // Total ticks based on type (rounds)
        val ticks = when (type) {
            QTEType.ONE_TIME -> 1 * total * period
            QTEType.TWO_TIMES -> 2 * total * period
            QTEType.THREE_TIMES -> 3 * total * period
        }

        var failTime = 0
        var tot = 0
        var lastTickChance = 1

        val task = Bukkit.getScheduler().runTaskTimer(plugin!!, Runnable {
            // Finish QTE and proceed
            fun finish(result: QTEResult? = null) {
                result?.let {
                    function.invoke(player, result)
                    player.sendActionBar(
                        Component.text(
                            if (it == QTEResult.ACCEPTED) "✔" else "✘",
                            if (it == QTEResult.ACCEPTED) NamedTextColor.GREEN else NamedTextColor.RED
                        )
                    )
                } ?: function.invoke(player, QTEResult.UNABLE)

                responseMap.remove(uuid)
                activeTasks.remove(uuid)?.cancel()

                // Process queued QTEs after 1s delay
                Bukkit.getScheduler().runTaskLater(plugin!!, Runnable {
                    waitingQTEs[player.uniqueId]?.let { qtes ->
                        if (qtes.isNotEmpty()) {
                            qtes.removeAt(0).invoke()
                        }
                    }
                }, 20L)
            }

            // Current round number
            val chance = tot / (total * period) + 1

            // Reset interval position for new round
            if (chance != lastTickChance) {
                lastTickChance = chance
                intervalStart = ((0.1 + Math.random() * 0.6) * total).roundToInt()
            }

            // Player can no longer complete QTE
            if (!player.isOnline || player.isDead) {
                finish()
                return@Runnable
            }

            // Time's up or player abandoned
            tot += difficulty.mag
            if (tot >= ticks || !responseMap.containsKey(uuid)) {
                finish(QTEResult.REJECTED)
                return@Runnable
            }

            // Player responded — check timing
            if (responseMap[uuid]!!) {
                val intervalThisTime = when (chance) {
                    1 -> intervalStart
                    2 -> total * 2 - intervalStart - interval
                    3 -> total * 2 + intervalStart
                    else -> 0
                } * period

                if (tot in intervalThisTime..intervalThisTime + interval * period) {
                    finish(QTEResult.ACCEPTED)
                    return@Runnable
                } else {
                    failTime += 1
                    if (failTime >= type.time) {
                        finish(QTEResult.REJECTED)
                        return@Runnable
                    }
                    responseMap[uuid] = false
                }
            }

            // Render QTE bar on action bar
            if (tot % period == 0) {
                val sb = StringBuilder()
                repeat(100) { i ->
                    val isPassed = when (chance) {
                        1 -> i <= tot / period
                        2 -> i > 2 * total - tot / period
                        3 -> i <= tot / period - total * 2
                        else -> false
                    }
                    val color = when {
                        isPassed -> when (chance) {
                            1 -> '6'; 2 -> 'c'; 3 -> '4'; else -> '7'
                        }
                        i in intervalStart..intervalStart + interval -> 'e'
                        else -> when (chance) {
                            1 -> '7'; 2 -> '6'; 3 -> 'c'; else -> '7'
                        }
                    }
                    sb.append("§$color|")
                }
                val remaining = type.time - failTime
                player.sendActionBar(
                    Component.text("> ").color(NamedTextColor.GRAY)
                        .append(Component.text(sb.toString()))
                        .append(Component.text(" < (容错${remaining}次)").color(NamedTextColor.GRAY))
                )
            }
        }, 0L, 1L)

        activeTasks[uuid] = task
    }

    fun isQTEing(player: Player): Boolean {
        return responseMap.containsKey(player.uniqueId)
    }

    /** Clean up all QTE state for a player (call on quit) */
    fun cleanup(uuid: UUID) {
        activeTasks.remove(uuid)?.cancel()
        responseMap.remove(uuid)
        waitingQTEs.remove(uuid)
    }

    // ---- Response triggers ----

    @EventHandler
    fun onShift(event: PlayerToggleSneakEvent) {
        val player = event.player
        if (!player.isSneaking && isQTEing(player)) {
            responseMap[player.uniqueId] = true
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (isQTEing(player)) {
            responseMap[player.uniqueId] = true
            event.isCancelled = true
        }
    }

    // ---- Data classes ----

    enum class QTEDifficulty(val period: Int, val interval: Int, val mag: Int, val easier: QTEDifficulty?) {
        EASY(1, 30, 1, null),
        HARD(1, 15, 1, EASY),
        CHAOS(1, 10, 1, HARD),
        GLITCH(1, 7, 2, CHAOS),
        BETA(1, 4, 3, GLITCH)
    }

    enum class QTEType(val time: Int) {
        ONE_TIME(1),
        TWO_TIMES(2),
        THREE_TIMES(3)
    }

    enum class QTEResult {
        ACCEPTED,
        REJECTED,
        UNABLE
    }
}
