package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Hotkey binding GUI — 9 slots for Shift+1 through Shift+9.
 * Click a slot to cycle through available active skills / unbind.
 * Default: 1=Career, 2=Bind, 3=Reset, 4-9=unbound.
 */
object BindGUI {

    const val TITLE = "热键绑定 Shift+1~9"

    /** Default GUI bindings (slot -> description) */
    private val defaultBindings = mapOf(
        0 to "§a生涯面板 §7(默认)",
        1 to "§a绑定面板 §7(默认)",
        2 to "§a重置管理 §7(默认)"
    )

    fun open(player: Player) {
        val cp = CareerManager.getPlayer(player) ?: return
        val inv = Bukkit.createInventory(null, 27, Component.text(TITLE))

        // Collect all available active skills only (eurekas are passive, not bindable)
        val availableItems = mutableListOf<Pair<String, String>>()
        for ((branch, skills) in cp.unlockedBranches) {
            for ((index, skill) in skills.withIndex()) {
                if (skill.skillDef.skillType != SkillType.ACTIVE || skill.currentLevel < 1) continue
                availableItems.add(
                    "§a技能 §f${skill.skillDef.name} §7(${branch.displayName})" to "${branch.name}:$index"
                )
            }
        }

        for (i in 0..8) {
            val slot = 9 + i
            val bindStr = cp.hotkeyBinds[i]

            val icon = when {
                bindStr != null -> {
                    val parts = bindStr.split(":", limit = 2)
                    val branch = parts.getOrNull(0)?.let { Branch.fromName(it) }
                    val idx = parts.getOrNull(1)?.toIntOrNull()
                    val displayName = branch?.let { b -> idx?.let { cp.getSkill(b, it)?.skillDef?.name } } ?: "?"
                    boundSlotIcon(i, displayName)
                }
                i < 3 -> defaultSlotIcon(i, defaultBindings[i]!!)
                else -> unbindSlotIcon(i, availableItems.isNotEmpty())
            }
            inv.setItem(slot, icon)
        }

        // Info + Mode toggle
        inv.setItem(23, infoItem(cp.scrollMode))
        inv.setItem(25, modeToggleIcon(cp.scrollMode))

        // Close
        inv.setItem(26, closeButton())

        fillBackground(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        if (slot == 26) { player.closeInventory(); return true }

        // Mode toggle
        if (slot == 25) {
            val cp = CareerManager.getPlayer(player) ?: return true
            cp.scrollMode = !cp.scrollMode
            player.sendMessage("§6⚡ 切换为：${if (cp.scrollMode) "卷轴模式" else "Shift模式"}")
            player.closeInventory()
            open(player)
            return true
        }

        val bindIndex = slot - 9
        if (bindIndex !in 0..8) return true

        val cp = CareerManager.getPlayer(player) ?: return true

        // Check if player has any bindable content
        val hasContent = cp.unlockedBranches.any { (_, skills) ->
            skills.any { it.skillDef.skillType == SkillType.ACTIVE && it.currentLevel > 0 }
        } || cp.chosenEurekas.isNotEmpty()

        if (!hasContent) {
            player.sendMessage("§c没有可绑定的技能或顿悟！请先解锁并升级主动技能。")
            return true
        }

        // Open multi-level bind GUI
        player.closeInventory()
        BindClassGUI.open(player, bindIndex)
        return true
    }

    // ===== Icon Builders =====

    private fun defaultSlotIcon(index: Int, desc: String): ItemStack {
        val item = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        item.editMeta {
            it.displayName(Component.text("Shift+${index + 1}", NamedTextColor.GREEN))
            it.lore(listOf(
                Component.text(desc, NamedTextColor.GRAY),
                Component.text(""),
                Component.text("点击切换到主动技能", NamedTextColor.GOLD)
            ))
        }
        return item
    }

    private fun boundSlotIcon(index: Int, skillName: String): ItemStack {
        val item = ItemStack(Material.BLAZE_ROD)
        item.editMeta {
            it.displayName(Component.text("Shift+${index + 1}: $skillName", NamedTextColor.AQUA))
            it.lore(listOf(
                Component.text("已绑定", NamedTextColor.GREEN),
                Component.text(""),
                Component.text("点击切换到下一个技能/取消", NamedTextColor.GOLD)
            ))
        }
        return item
    }

    private fun unbindSlotIcon(index: Int, hasSkills: Boolean): ItemStack {
        val item = ItemStack(Material.GRAY_DYE)
        item.editMeta {
            it.displayName(Component.text("Shift+${index + 1}", NamedTextColor.GRAY))
            it.lore(listOf(
                Component.text("未绑定", NamedTextColor.DARK_GRAY),
                Component.text(""),
                if (hasSkills)
                    Component.text("点击绑定第一个可用技能", NamedTextColor.YELLOW)
                else
                    Component.text("没有可用的主动技能", NamedTextColor.RED)
            ))
        }
        return item
    }

    private fun modeToggleIcon(scrollMode: Boolean): ItemStack {
        val item = ItemStack(if (scrollMode) Material.ENCHANTED_BOOK else Material.LEVER)
        item.editMeta {
            it.displayName(Component.text("释放模式", NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("当前：${if (scrollMode) "§d卷轴模式" else "§eShift模式"}", NamedTextColor.GRAY),
                Component.text("", NamedTextColor.WHITE),
                if (scrollMode)
                    Component.text("副手卷轴 + 切换槽位 + F = 释放", NamedTextColor.LIGHT_PURPLE)
                else
                    Component.text("下蹲 + 1~9 = 释放", NamedTextColor.YELLOW),
                Component.text("", NamedTextColor.WHITE),
                Component.text("点击切换模式", NamedTextColor.GOLD)
            ))
        }
        return item
    }

    private fun infoItem(scrollMode: Boolean): ItemStack {
        val item = ItemStack(Material.KNOWLEDGE_BOOK)
        item.editMeta {
            it.displayName(Component.text("使用说明 · ${if (scrollMode) "卷轴模式" else "Shift模式"}", NamedTextColor.GOLD))
            val desc = mutableListOf(
                Component.text("当前模式：${if (scrollMode) "§d卷轴模式" else "§eShift模式"}", NamedTextColor.GRAY),
                Component.text(""),
            )
            if (scrollMode) {
                desc.addAll(listOf(
                    Component.text("① 将技能卷轴放入副手", NamedTextColor.GRAY),
                    Component.text("② 切换到目标数字槽位", NamedTextColor.GRAY),
                    Component.text("③ 按 F 键释放技能", NamedTextColor.GRAY),
                    Component.text("", NamedTextColor.WHITE),
                    Component.text("Shift+F → 生涯面板", NamedTextColor.DARK_GRAY),
                    Component.text("普通按键 → 正常切换物品栏", NamedTextColor.DARK_GRAY)
                ))
            } else {
                desc.addAll(listOf(
                    Component.text("下蹲 + 数字键 → 释放技能", NamedTextColor.GRAY),
                    Component.text("", NamedTextColor.WHITE),
                    Component.text("Shift+1 生涯 | Shift+2 绑定", NamedTextColor.DARK_GRAY),
                    Component.text("Shift+3 重置 | Shift+4~9 自由", NamedTextColor.DARK_GRAY)
                ))
            }
            desc.addAll(listOf(
                Component.text(""),
                Component.text("点击下方按钮切换模式", NamedTextColor.GOLD)
            ))
            it.lore(desc)
        }
        return item
    }

    private fun closeButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.editMeta { it.displayName(Component.text("关闭", NamedTextColor.RED)) }
        return item
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        for (i in 0 until 27) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, filler)
        }
    }
}
