package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Level 4 bind GUI: lists all bindable skills + eureka for a branch.
 * Click an item to instantly bind it to the target hotkey slot.
 */
object BindSkillSelectGUI {

    const val TITLE_PREFIX = "绑定技能："

    fun open(player: Player, branch: Branch, targetSlot: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        val inv = Bukkit.createInventory(null, 27, Component.text("$TITLE_PREFIX${branch.displayName}"))
        var slot = 10

        // Active skills
        for ((index, skill) in cp.getSkills(branch).withIndex()) {
            if (skill.skillDef.skillType != SkillType.ACTIVE || skill.currentLevel < 1) continue
            inv.setItem(slot, skillBindIcon(skill.skillDef.name, index, branch, targetSlot))
            slot++
        }

        // Show ACTIVE eurekas
        cp.chosenEurekas[branch]?.let { eureka ->
            if (eureka.eurekaDef.skillType == SkillType.ACTIVE) {
                inv.setItem(slot, eurekaBindIcon(eureka.eurekaDef.name, branch, targetSlot))
                slot++
            }
        }

        if (slot == 10) {
            inv.setItem(13, emptyIcon())
        }

        inv.setItem(22, backButton(branch))
        fill(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch): Boolean {
        if (slot == 22) { player.closeInventory(); BindBranchGUI.open(player, branch.careerClass, -1); return true }

        val cp = CareerManager.getPlayer(player) ?: return true

        // Find the binding data stored in item meta via lore hack
        // We use the slot position to determine which skill/eureka
        var skillOffset = 0
        for ((index, skill) in cp.getSkills(branch).withIndex()) {
            if (skill.skillDef.skillType != SkillType.ACTIVE || skill.currentLevel < 1) continue
            if (10 + skillOffset == slot) {
                // Bind this skill
                val bindIndex = BindSkillSelectTarget.targetSlot
                if (bindIndex < 0) { player.sendMessage("§c绑定槽位信息丢失"); return true }
                cp.hotkeyBinds[bindIndex] = "${branch.name}:$index"
                player.sendMessage("§a✦ Shift+${bindIndex + 1} 绑定：${skill.skillDef.name}")
                player.closeInventory()
                BindGUI.open(player)
                return true
            }
            skillOffset++
        }

        // ACTIVE eureka binding
        cp.chosenEurekas[branch]?.let { eureka ->
            if (eureka.eurekaDef.skillType == SkillType.ACTIVE && 10 + skillOffset == slot) {
                val bindIndex = BindSkillSelectTarget.targetSlot
                if (bindIndex < 0) { player.sendMessage("§c绑定槽位信息丢失"); return true }
                cp.hotkeyBinds[bindIndex] = "eureka:${branch.name}"
                player.sendMessage("§a✦ Shift+${bindIndex + 1} 绑定：§d${eureka.eurekaDef.name}")
                player.closeInventory()
                BindGUI.open(player)
                return true
            }
        }

        return true
    }

    private fun skillBindIcon(name: String, index: Int, branch: Branch, slot: Int): ItemStack {
        val item = ItemStack(Material.BLAZE_ROD)
        item.editMeta {
            it.displayName(Component.text("技能：$name", NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("分支：${branch.displayName}", NamedTextColor.GRAY),
                Component.text("类型：主动技能", NamedTextColor.AQUA),
                Component.text(""),
                Component.text("点击绑定至 Shift+${slot + 1}", NamedTextColor.GREEN, TextDecoration.BOLD)
            ))
        }
        return item
    }

    private fun eurekaBindIcon(name: String, branch: Branch, slot: Int): ItemStack {
        val item = ItemStack(Material.NETHER_STAR)
        item.editMeta {
            it.displayName(Component.text("顿悟：$name", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("分支：${branch.displayName}", NamedTextColor.GRAY),
                Component.text("类型：顿悟", NamedTextColor.DARK_PURPLE),
                Component.text(""),
                Component.text("点击绑定至 Shift+${slot + 1}", NamedTextColor.GREEN, TextDecoration.BOLD)
            ))
        }
        return item
    }

    private fun emptyIcon(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.editMeta {
            it.displayName(Component.text("没有可绑定的技能/顿悟", NamedTextColor.RED))
            it.lore(listOf(Component.text("请先升级主动技能或解锁顿悟", NamedTextColor.GRAY)))
        }
        return item
    }

    private fun backButton(branch: Branch): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.editMeta { it.displayName(Component.text("返回分支选择", NamedTextColor.YELLOW)) }
        return item
    }

    private fun fill(inv: org.bukkit.inventory.Inventory) {
        val f = IconFactory.fillerPane()
        for (i in 0 until 27) { if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, f) }
    }
}

/** Holds the target slot index between GUI levels */
object BindSkillSelectTarget {
    var targetSlot: Int = -1
}
