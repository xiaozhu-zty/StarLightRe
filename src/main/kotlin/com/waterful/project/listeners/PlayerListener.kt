package com.waterful.project.listeners

import com.waterful.project.career.gui.CareerGUI
import com.waterful.project.career.manager.AntiFarmManager
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.ResonanceManager
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.SkillType
import com.waterful.project.career.skill.EurekaEffectHandler
import com.waterful.project.career.skill.SkillExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class PlayerListener : Listener {

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

    // ===== Hotkey: Shift+1~9 → bound skills / eurekas / GUIs =====

    @EventHandler
    fun onPlayerItemHeld(event: org.bukkit.event.player.PlayerItemHeldEvent) {
        val player = event.player
        if (!player.isSneaking) return
        event.isCancelled = true

        val numKey = event.newSlot + 1 // 1-9
        val cp = CareerManager.getPlayer(player) ?: return

        // Check custom binding
        val bindStr = cp.hotkeyBinds[numKey - 1]
        if (bindStr != null) {
            // Eureka binding: "eureka:branchName"
            if (bindStr.startsWith("eureka:")) {
                val branchName = bindStr.removePrefix("eureka:")
                val branch = com.waterful.project.career.model.Branch.fromName(branchName)
                if (branch != null && cp.chosenEurekas.containsKey(branch)) {
                    val eureka = cp.chosenEurekas[branch]!!
                    if (EurekaEffectHandler.execute(eureka.eurekaDef.id, player)) return
                    player.sendMessage("§5✦ 顿悟触发：${eureka.eurekaDef.name}")
                }
                return
            }
            // Skill binding: "branchName:skillIndex"
            val parts = bindStr.split(":", limit = 2)
            val branchName = parts.getOrNull(0)
            val skillIndex = parts.getOrNull(1)?.toIntOrNull()
            if (branchName != null && skillIndex != null) {
                val branch = com.waterful.project.career.model.Branch.fromName(branchName)
                if (branch != null) {
                    val skill = cp.getSkill(branch, skillIndex)
                    if (skill != null && skill.currentLevel > 0) {
                        SkillExecutor.executeSkill(player, skill)
                        return
                    }
                }
            }
        }

        // Fallback: default GUI hotkeys
        when (numKey) {
            1 -> CareerGUI.open(player)
            2 -> com.waterful.project.career.gui.BindGUI.open(player)
            3 -> com.waterful.project.career.gui.DelCareerGUI.open(player)
        }
    }

    private fun showHotkeyHint(player: org.bukkit.entity.Player) {
        player.sendMessage(
            Component.text("热键：Shift+F 生涯面板 | Shift+2 绑定面板 | Shift+1~9 自定义技能/顿悟",
                NamedTextColor.GRAY)
        )
    }
}
