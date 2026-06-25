package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.ResonanceMode
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object ResonanceGUI {

    const val TITLE_PREFIX = "共鸣模式："

    fun open(player: Player, branch: Branch) {
        val cp = CareerManager.getPlayer(player) ?: return
        val currentMode = cp.getResonanceMode(branch)
        val passiveLevel = cp.getResonanceLevel(branch)

        val title = Component.text("$TITLE_PREFIX${branch.displayName}")
        val inv = Bukkit.createInventory(null, 27, title)
        fillBackground(inv)

        // Mode buttons at slots 10-14
        val modes = ResonanceMode.entries.toList()
        val modeSlots = listOf(10, 11, 12, 13, 14)
        modes.forEachIndexed { index, mode ->
            inv.setItem(modeSlots[index], IconFactory.resonanceModeIcon(mode, mode == currentMode))
        }

        // Range info
        if (passiveLevel > 0 && currentMode != ResonanceMode.DISABLED && currentMode != ResonanceMode.SOLO_ECHO) {
            val range = cp.getResonanceRange(branch)
            inv.setItem(22, createRangeInfo(passiveLevel, currentMode, range))
        } else if (currentMode == ResonanceMode.SOLO_ECHO) {
            inv.setItem(22, createSoloEchoInfo())
        } else {
            inv.setItem(22, createNoResonanceInfo())
        }

        // Bottom
        inv.setItem(18, IconFactory.backButton())
        inv.setItem(26, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch): Boolean {
        val modeMap = mapOf(
            10 to ResonanceMode.DISABLED,
            11 to ResonanceMode.GLOBAL,
            12 to ResonanceMode.FRIENDLY,
            13 to ResonanceMode.INTERNAL,
            14 to ResonanceMode.SOLO_ECHO
        )

        when (slot) {
            in modeMap -> {
                val mode = modeMap[slot]!!
                CareerManager.setResonanceMode(player, branch, mode)
                player.closeInventory()
                BranchGUI.open(player, branch)
                return true
            }
            18 -> {
                player.closeInventory()
                BranchGUI.open(player, branch)
                return true
            }
            26 -> { player.closeInventory(); return true }
        }
        return true
    }

    private fun createRangeInfo(
        passiveLevel: Int,
        mode: ResonanceMode,
        range: Double
    ): org.bukkit.inventory.ItemStack {
        val item = org.bukkit.inventory.ItemStack(Material.COMPASS)
        item.editMeta {
            it.displayName(Component.text("共鸣范围信息", net.kyori.adventure.text.format.NamedTextColor.AQUA))
            it.lore(listOf(
                Component.text("被动技能等级：Lv.$passiveLevel",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY),
                Component.text("共鸣模式：${mode.displayName}",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY),
                Component.text("范围：${String.format("%.1f", range)}格",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN)
            ))
        }
        return item
    }

    private fun createSoloEchoInfo(): org.bukkit.inventory.ItemStack {
        val item = org.bukkit.inventory.ItemStack(Material.ECHO_SHARD)
        item.editMeta {
            it.displayName(Component.text("独奏回响", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("自身共鸣效果翻倍", net.kyori.adventure.text.format.NamedTextColor.GREEN),
                Component.text("不向其他玩家分享效果", net.kyori.adventure.text.format.NamedTextColor.GRAY)
            ))
        }
        return item
    }

    private fun createNoResonanceInfo(): org.bukkit.inventory.ItemStack {
        val item = org.bukkit.inventory.ItemStack(Material.BARRIER)
        item.editMeta {
            it.displayName(Component.text("无共鸣", net.kyori.adventure.text.format.NamedTextColor.GRAY))
            it.lore(listOf(
                Component.text("被动技能未激活或已关闭共鸣", net.kyori.adventure.text.format.NamedTextColor.RED),
                Component.text("提升被动技能等级以解锁共鸣效果", net.kyori.adventure.text.format.NamedTextColor.GRAY)
            ))
        }
        return item
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        for (i in 0 until 27) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) {
                inv.setItem(i, filler)
            }
        }
    }
}
