package com.waterful.project.career.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/** Per-player debug mode: when enabled, all career gate checks output pass/fail messages. */
object DebugManager {
    private val listeningPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun isListening(uuid: UUID): Boolean = listeningPlayers.contains(uuid)

    fun setListening(uuid: UUID, enabled: Boolean) {
        if (enabled) listeningPlayers.add(uuid) else listeningPlayers.remove(uuid)
    }

    /**
     * Roll a percentage chance and output debug message if player is listening.
     * @return true if the roll succeeded (random < percentage)
     */
    fun rollChance(player: Player, percentage: Int, description: String): Boolean {
        val roll = ThreadLocalRandom.current().nextInt(100)
        val success = roll < percentage
        if (isListening(player.uniqueId)) {
            val color = if (success) NamedTextColor.GREEN else NamedTextColor.RED
            val symbol = if (success) "✓" else "✗"
            player.sendMessage(
                Component.text("  🎲 $description: ${percentage}% → 判定值=$roll → $symbol ${if (success) "成功" else "失败"}",
                    color)
            )
        }
        return success
    }

    /** Shorthand for rollChance when percentage is in tenths (e.g. 1 = 10%) */
    fun rollChanceDec(player: Player, tenths: Int, description: String): Boolean =
        rollChance(player, tenths * 10, description)

    /** Output a state-tracking debug message when listening mode is on. */
    fun logState(player: Player, message: String) {
        if (isListening(player.uniqueId)) {
            player.sendMessage(Component.text("  📍 $message", NamedTextColor.BLUE))
        }
    }

    /**
     * Roll a fractional percentage (e.g. 0.5% = 1 in 200).
     * Uses 0-1000 range for precision.
     */
    fun rollChanceFraction(player: Player, numerator: Int, denominator: Int, description: String): Boolean {
        val roll = ThreadLocalRandom.current().nextInt(denominator)
        val success = roll < numerator
        if (isListening(player.uniqueId)) {
            val color = if (success) NamedTextColor.GREEN else NamedTextColor.RED
            val symbol = if (success) "✓" else "✗"
            val pct = String.format("%.1f", numerator * 100.0 / denominator)
            player.sendMessage(
                Component.text("  🎲 $description: ${pct}% → 判定值=$roll/$denominator → $symbol ${if (success) "成功" else "失败"}",
                    color)
            )
        }
        return success
    }
}
