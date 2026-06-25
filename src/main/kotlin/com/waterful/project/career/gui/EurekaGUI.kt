package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.model.Branch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object EurekaGUI {

    const val TITLE_PREFIX = "选择顿悟："

    fun open(player: Player, branch: Branch) {
        val cp = CareerManager.getPlayer(player) ?: return
        val eurekaOptions = CareerManager.getEurekaOptions(branch)

        val title = Component.text("$TITLE_PREFIX${branch.displayName}")
        val inv = Bukkit.createInventory(null, 27, title)
        fillBackground(inv)

        // Title
        inv.setItem(4, createEurekaTitle(branch))

        // 3 Eureka options at slots 11, 13, 15
        val eurekaSlots = listOf(11, 13, 15)
        eurekaOptions.forEachIndexed { index, eureka ->
            val isChosen = cp.chosenEurekas[branch]?.eurekaDef?.id == eureka.id
            val canAfford = SkillPointManager.hasEnough(cp, 1)
            inv.setItem(eurekaSlots[index], IconFactory.eurekaOptionIcon(eureka, isChosen, canAfford))
        }

        // Bottom
        inv.setItem(22, IconFactory.backButton())
        inv.setItem(26, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch): Boolean {
        when (slot) {
            11 -> { chooseEureka(player, branch, 0); return true }
            13 -> { chooseEureka(player, branch, 1); return true }
            15 -> { chooseEureka(player, branch, 2); return true }
            22 -> {
                player.closeInventory()
                BranchGUI.open(player, branch)
                return true
            }
            26 -> { player.closeInventory(); return true }
        }
        return true
    }

    private fun chooseEureka(player: Player, branch: Branch, index: Int) {
        val cp = CareerManager.getPlayer(player) ?: return

        if (!SkillPointManager.hasEnough(cp, 1)) {
            player.sendMessage("§c技能点不足！")
            return
        }

        if (CareerManager.chooseEureka(player, branch, index)) {
            player.closeInventory()
            BranchGUI.open(player, branch)
        }
    }

    private fun createEurekaTitle(branch: Branch): ItemStack {
        val item = ItemStack(Material.NETHER_STAR)
        item.editMeta {
            it.displayName(Component.text("选择顿悟 — ${branch.displayName}", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("从3个顿悟中选择1个解锁", NamedTextColor.GRAY),
                Component.text("消耗：1技能点", NamedTextColor.GRAY),
                Component.text("选择后不可更改（除非遗忘分支）", NamedTextColor.RED)
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
