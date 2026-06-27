package com.waterful.project.listeners

import com.waterful.project.career.CareerItems
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
        CareerManager.validateHotkeyBinds(cp) // Clean stale bindings

        // Give career scroll if not present
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin
        if (plugin != null) {
            val hasScroll = player.inventory.any { it != null && CareerItems.isScroll(plugin, it) }
            if (!hasScroll) {
                player.inventory.addItem(CareerItems.createScroll(plugin))
            }
        }

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

    // ===== Hotkey: F (Swap) → Career Panel (Shift) or Release (Scroll Mode) =====

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val p = event.player
        val cp = CareerManager.getPlayer(p) ?: return

        // Scroll mode: offhand scroll + F = release bound skill for current slot
        if (cp.scrollMode) {
            val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin
            if (plugin != null) {
                val offhand = p.inventory.itemInOffHand
                if (offhand.type != org.bukkit.Material.AIR && CareerItems.isScroll(plugin, offhand)) {
                    event.isCancelled = true
                    val slot = p.inventory.heldItemSlot // 0-8
                    val bindStr = cp.hotkeyBinds[slot]
                    if (bindStr != null) {
                        executeBinding(p, cp, bindStr)
                    }
                    return
                }
            }
        }

        // Default: Shift+F → Career Panel
        if (p.isSneaking) {
            event.isCancelled = true
            CareerGUI.open(p)
        }
    }

    private fun executeBinding(p: org.bukkit.entity.Player, cp: com.waterful.project.career.model.CareerPlayer, bindStr: String) {
        if (bindStr.startsWith("eureka:")) {
            val branch = com.waterful.project.career.model.Branch.fromName(bindStr.removePrefix("eureka:"))
            if (branch != null && cp.chosenEurekas.containsKey(branch)) {
                EurekaEffectHandler.execute(cp.chosenEurekas[branch]!!.eurekaDef.id, p)
            }
            return
        }
        val parts = bindStr.split(":", limit = 2)
        val branchName = parts.getOrNull(0) ?: return
        val skillIndex = parts.getOrNull(1)?.toIntOrNull() ?: return
        val branch = com.waterful.project.career.model.Branch.fromName(branchName) ?: return
        val skill = cp.getSkill(branch, skillIndex) ?: return
        if (skill.currentLevel > 0) SkillExecutor.executeSkill(p, skill)
    }

    // ===== Career Scroll: right-click → open CareerGUI =====

    @EventHandler
    fun onScrollUse(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val item = player.inventory.itemInMainHand
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin ?: return
        if (!CareerItems.isScroll(plugin, item)) return
        event.isCancelled = true
        CareerGUI.open(player)
    }

    // ===== Hotkey: Shift+1~9 → bound skills / GUIs (Shift mode only) =====

    @EventHandler
    fun onPlayerItemHeld(event: org.bukkit.event.player.PlayerItemHeldEvent) {
        val player = event.player
        if (!player.isSneaking) return
        val cp = CareerManager.getPlayer(player) ?: return

        // Scroll mode: don't intercept hotbar switching
        if (cp.scrollMode) return

        event.isCancelled = true
        val numKey = event.newSlot + 1 // 1-9

        // Check custom binding
        val bindStr = cp.hotkeyBinds[numKey - 1]
        if (bindStr != null) {
            executeBinding(player, cp, bindStr)
            return
        }

        // Fallback: default GUI hotkeys
        when (numKey) {
            1 -> CareerGUI.open(player)
            2 -> com.waterful.project.career.gui.BindGUI.open(player)
            3 -> com.waterful.project.career.gui.DelCareerGUI.open(player)
        }
    }

    private fun showHotkeyHint(player: org.bukkit.entity.Player) {
        val cp = CareerManager.getPlayer(player)
        val hint = if (cp?.scrollMode == true)
            "§7卷轴模式：副手卷轴 + 切换槽位 + F = 释放 | Shift+F = 生涯"
        else
            "§7Shift模式：Shift+F 生涯 | Shift+2 绑定 | Shift+1~9 释放"
        player.sendMessage(hint)
    }
}
