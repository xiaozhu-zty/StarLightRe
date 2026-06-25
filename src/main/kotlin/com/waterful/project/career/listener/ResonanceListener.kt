package com.waterful.project.career.listener

import com.waterful.project.career.manager.ResonanceManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class ResonanceListener : Listener {

    private val lastMoveUpdate = mutableMapOf<java.util.UUID, Long>()
    private val moveThrottleMs = 2000L  // 2 seconds between updates

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Only update if the player changed block position
        if (!event.hasChangedPosition()) return

        val player = event.player
        val now = System.currentTimeMillis()
        val lastUpdate = lastMoveUpdate[player.uniqueId] ?: 0L

        if (now - lastUpdate < moveThrottleMs) return
        lastMoveUpdate[player.uniqueId] = now

        // Refresh resonance effects for this player
        ResonanceManager.refreshPlayer(player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Apply resonance from nearby players
        ResonanceManager.refreshPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Clean up resonance effects
        ResonanceManager.removeAllResonanceFromPlayer(event.player)
        lastMoveUpdate.remove(event.player.uniqueId)
    }
}
