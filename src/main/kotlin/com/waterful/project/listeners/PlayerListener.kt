package com.waterful.project.listeners

import com.waterful.project.career.gui.CareerGUI
import com.waterful.project.career.manager.AntiFarmManager
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.ResonanceManager
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.SkillType
import com.waterful.project.career.skill.SkillExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class PlayerListener : Listener {

    // ===== Career Data Lifecycle =====

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val cp = CareerManager.loadPlayer(player.uniqueId, player.name)

        if (!cp.isCareerSelected) {
            val assigned = AntiFarmManager.assignClassesOnJoin(player)

            if (assigned.isNotEmpty()) {
                val remaining = CareerPlayer.INITIAL_CLASSES - assigned.size
                player.sendMessage(
                    Component.text("✦ 系统已为你分配职业：${assigned.joinToString("、") { it.displayName }}",
                        NamedTextColor.GREEN)
                )
                if (remaining > 0) {
                    player.sendMessage(
                        Component.text("请使用 Shift+F 打开生涯面板，从剩余职业中选择 $remaining 个。",
                            NamedTextColor.YELLOW)
                    )
                }
                showHotkeyHint(player)
            }
        } else {
            player.sendMessage(
                Component.text("✦ 欢迎回来！你的职业：${cp.selectedClasses.joinToString("、") { it.displayName }}",
                    NamedTextColor.AQUA)
            )
            showHotkeyHint(player)
        }

        ResonanceManager.refreshPlayer(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        CareerManager.unloadPlayer(player.uniqueId)
        ResonanceManager.removeAllResonanceFromPlayer(player)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        AntiFarmManager.handleRespawn(event.player)
    }

    // ===== Hotkey: Shift+F → Career Panel =====

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (event.player.isSneaking) {
            event.isCancelled = true
            CareerGUI.open(event.player)
        }
    }

    // ===== Hotkey: Shift+Q → Active Skill 1 =====

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        if (!player.isSneaking) return

        // Only trigger if player has active skills
        val cp = CareerManager.getPlayer(player) ?: return
        if (cp.unlockedBranches.isEmpty()) return

        // Find first active skill at index 1 with level > 0
        val entry = cp.unlockedBranches.entries.firstOrNull { (_, skills) ->
            val skill = skills.getOrNull(1)
            skill != null && skill.skillDef.skillType == SkillType.ACTIVE && skill.currentLevel > 0
        } ?: return

        event.isCancelled = true
        val (branch, _) = entry
        val skill = cp.getSkill(branch, 1)!!
        SkillExecutor.executeSkill(player, skill)
    }

    // ===== Hotkey: Shift+Right Click Air → Active Skill 2 =====

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!player.isSneaking) return
        if (event.action != Action.RIGHT_CLICK_AIR) return

        // Don't intercept when holding a usable item (bow, food, potion, etc.)
        val held = player.inventory.itemInMainHand
        if (held.type.isBlock || held.type.isEdible || held.type == Material.BOW ||
            held.type == Material.CROSSBOW || held.type == Material.TRIDENT ||
            held.type == Material.SHIELD || held.type == Material.POTION ||
            held.type == Material.SPLASH_POTION || held.type == Material.LINGERING_POTION
        ) return

        val cp = CareerManager.getPlayer(player) ?: return
        if (cp.unlockedBranches.isEmpty()) return

        // Find first active skill at index 2 with level > 0
        val entry = cp.unlockedBranches.entries.firstOrNull { (_, skills) ->
            val skill = skills.getOrNull(2)
            skill != null && skill.skillDef.skillType == SkillType.ACTIVE && skill.currentLevel > 0
        } ?: return

        event.isCancelled = true
        val (branch, _) = entry
        val skill = cp.getSkill(branch, 2)!!
        SkillExecutor.executeSkill(player, skill)
    }

    // ===== Helpers =====

    private fun showHotkeyHint(player: org.bukkit.entity.Player) {
        player.sendMessage(
            Component.text("快捷键：Shift+F 生涯面板 | Shift+Q 技能1 | Shift+右键 技能2",
                NamedTextColor.GRAY)
        )
    }
}
