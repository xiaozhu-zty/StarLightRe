package com.waterful.project.career.listener

import com.waterful.project.career.manager.AntiFarmManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class AntiFarmListener : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val isSuicide = player.lastDamageCause?.let {
            it.cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.SUICIDE ||
                it.cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID
        } ?: false

        AntiFarmManager.recordDeath(player, isSuicide)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        AntiFarmManager.handleRespawn(event.player)
    }
}
