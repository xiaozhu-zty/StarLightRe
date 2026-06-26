package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Level 3 bind GUI: shows all bindable skills + eureka for a branch.
 * Clicking a skill/eureka binds it to the target hotkey slot.
 */
object BindBranchGUI {

    const val TITLE_PREFIX = "绑定："

    /** Store target slot temporarily (keyed by player UUID) */
    private val targetSlots = mutableMapOf<java.util.UUID, Int>()

    fun open(player: Player, careerClass: CareerClass, targetSlot: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        targetSlots[player.uniqueId] = targetSlot

        val branches = Branch.fromCareerClass(careerClass).filter { cp.unlockedBranches.containsKey(it) }
        val title = Component.text("$TITLE_PREFIX${careerClass.displayName}")
        val inv = Bukkit.createInventory(null, 27, title)

        // Show unlocked branches with their bindable skills
        val branchSlots = listOf(10, 12, 14, 16)
        branches.forEachIndexed { i, branch ->
            if (i < branchSlots.size) {
                inv.setItem(branchSlots[i], branchIcon(player, cp, branch, targetSlot))
            }
        }

        inv.setItem(22, backButton(careerClass))
        fill(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        if (slot == 22) {
            player.closeInventory()
            BindClassGUI.open(player, targetSlots[player.uniqueId] ?: 0)
            return true
        }

        val branchSlots = listOf(10, 12, 14, 16)
        val idx = branchSlots.indexOf(slot)
        if (idx < 0) return true

        val cp = CareerManager.getPlayer(player) ?: return true
        val targetSlot = targetSlots[player.uniqueId] ?: return true
        val careerClass = CareerClass.entries.find { clazz ->
            val branches = Branch.fromCareerClass(clazz)
            idx < branches.size && cp.unlockedBranches.containsKey(branches[idx])
        } ?: return true
        val branch = Branch.fromCareerClass(careerClass).filter { cp.unlockedBranches.containsKey(it) }.getOrNull(idx) ?: return true

        // Open skill selection for this branch
        player.closeInventory()
        BindSkillSelectGUI.open(player, branch, targetSlot)
        return true
    }

    private fun branchIcon(player: Player, cp: com.waterful.project.career.model.CareerPlayer, branch: Branch, slot: Int): ItemStack {
        val item = ItemStack(branch.careerClass.material)
        val activeCount = cp.getSkills(branch).count { it.skillDef.skillType == SkillType.ACTIVE && it.currentLevel > 0 }
        val hasEureka = cp.chosenEurekas.containsKey(branch)
        item.editMeta {
            it.displayName(Component.text(branch.displayName, NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("可绑定技能：$activeCount 个", NamedTextColor.GREEN),
                if (hasEureka) Component.text("可绑定顿悟：1 个", NamedTextColor.LIGHT_PURPLE)
                else Component.empty(),
                Component.text(""),
                Component.text("点击查看详情", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    private fun backButton(clazz: CareerClass): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.editMeta { it.displayName(Component.text("返回职业选择", NamedTextColor.YELLOW)) }
        return item
    }

    private fun fill(inv: org.bukkit.inventory.Inventory) {
        val f = IconFactory.fillerPane()
        for (i in 0 until 27) { if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, f) }
    }
}
