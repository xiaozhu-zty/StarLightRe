package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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

        inv.setItem(4, createEurekaTitle(branch, cp))

        val eurekaSlots = listOf(11, 13, 15)
        eurekaOptions.forEachIndexed { index, eureka ->
            val isChosen = cp.chosenEurekas[branch]?.eurekaDef?.id == eureka.id
            val cost = if (cp.isSpecialEureka(eureka.name)) 0 else 1
            val canAfford = cost == 0 || SkillPointManager.hasEnough(cp, cost)
            inv.setItem(eurekaSlots[index], IconFactory.eurekaOptionIcon(eureka, isChosen, canAfford, cp, cost))
        }

        inv.setItem(22, IconFactory.backButton())
        inv.setItem(26, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch): Boolean {
        when (slot) {
            11 -> { chooseEureka(player, branch, 0); return true }
            13 -> { chooseEureka(player, branch, 1); return true }
            15 -> { chooseEureka(player, branch, 2); return true }
            22 -> { player.closeInventory(); BranchGUI.open(player, branch); return true }
            26 -> { player.closeInventory(); return true }
        }
        return true
    }

    private fun chooseEureka(player: Player, branch: Branch, index: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        val eurekaOption = CareerManager.getEurekaOptions(branch).getOrNull(index) ?: return

        val cost = if (cp.isSpecialEureka(eurekaOption.name)) 0 else 1
        if (cost > 0 && !SkillPointManager.hasEnough(cp, cost)) {
            player.sendMessage("§c技能点不足！")
            return
        }

        if (CareerManager.chooseEureka(player, branch, index)) {
            player.closeInventory()
            BranchGUI.open(player, branch)
        }
    }

    private fun createEurekaTitle(branch: Branch, cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.NETHER_STAR)
        val specialEureka = CareerManager.getEurekaOptions(branch).any { cp.isSpecialEureka(it.name) }
        item.editMeta {
            it.displayName(Component.text("选择顿悟 — ${branch.displayName}", NamedTextColor.LIGHT_PURPLE))
            val lore = mutableListOf(
                Component.text("从3个顿悟中选择1个解锁", NamedTextColor.GRAY),
            )
            if (specialEureka) {
                lore.add(Component.text("特殊顿悟：同职业有另一分支时赠送3技能点！", NamedTextColor.GOLD, TextDecoration.BOLD))
            } else {
                lore.add(Component.text("消耗：1技能点", NamedTextColor.GRAY))
            }
            lore.add(Component.text("选择后不可更改（除非遗忘分支）", NamedTextColor.RED))
            it.lore(lore)
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
