package com.waterful.project.career.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Two-step confirmation for destructive operations.
 * First click → show confirm/cancel in chat. Second click → execute or cancel.
 */
object ConfirmManager {

    data class PendingAction(
        val playerId: UUID,
        val description: String,
        val action: () -> Unit,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val pending = ConcurrentHashMap<UUID, PendingAction>()
    private const val TIMEOUT_MS = 30_000L

    /** Register a destructive action requiring confirmation. Returns true if confirmation is needed. */
    fun requestConfirm(player: Player, description: String, action: () -> Unit): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return false

        // Check if there's already a pending confirmation for this player
        val existing = pending[player.uniqueId]
        if (existing != null && existing.description == description) {
            // Second click — execute!
            pending.remove(player.uniqueId)
            action()
            return true
        }

        // First click — show confirmation
        pending[player.uniqueId] = PendingAction(player.uniqueId, description, action)

        player.sendMessage(Component.text(""))
        player.sendMessage(Component.text("⚠ $description", NamedTextColor.GOLD))
        player.sendMessage(Component.text("此操作不可撤销！", NamedTextColor.RED))
        player.sendMessage(
            Component.text("        [✓ 确认]        ", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/confirm yes"))
        )
        player.sendMessage(
            Component.text("        [✗ 取消]        ", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/confirm no"))
        )
        player.sendMessage(Component.text(""))

        // Auto-expire after timeout
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val p = pending.remove(player.uniqueId)
            if (p != null && p == existing) {
                player.sendMessage(Component.text("§7操作已超时取消"))
            }
        }, TIMEOUT_MS / 50)

        return false
    }

    fun confirm(player: Player): Boolean {
        val p = pending.remove(player.uniqueId) ?: return false
        p.action()
        return true
    }

    fun cancel(player: Player): Boolean {
        pending.remove(player.uniqueId)?.let {
            player.sendMessage(Component.text("§7操作已取消"))
        }
        return true
    }

    fun hasPending(player: Player): Boolean = pending.containsKey(player.uniqueId)
}
