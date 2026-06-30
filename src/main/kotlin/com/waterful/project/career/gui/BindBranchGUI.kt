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
import java.util.UUID

/**
 * Level 3 bind GUI: shows unlocked branches for a career class.
 * Click a branch to see bindable skills.
 */
object BindBranchGUI {

    const val TITLE_PREFIX = "绑定："

    /** Store (careerClass, targetSlot) per player */
    private val ctx = mutableMapOf<UUID, Pair<CareerClass, Int>>()

    fun open(player: Player, careerClass: CareerClass, targetSlot: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        ctx[player.uniqueId] = careerClass to targetSlot

        val branches = Branch.fromCareerClass(careerClass).filter { cp.unlockedBranches.containsKey(it) }
        val title = Component.text("$TITLE_PREFIX${careerClass.displayName}")
        val inv = Bukkit.createInventory(null, 27, title)

        val slots = listOf(10, 12, 14, 16)
        branches.forEachIndexed { i, branch ->
            if (i < slots.size) inv.setItem(slots[i], branchIcon(cp, branch))
        }

        inv.setItem(22, backButton())
        fill(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        if (slot == 22) {
            val (_, ts) = ctx[player.uniqueId] ?: return true
            BindClassGUI.open(player, ts)
            return true
        }

        val slots = listOf(10, 12, 14, 16)
        val idx = slots.indexOf(slot)
        if (idx < 0) return true

        val cp = CareerManager.getPlayer(player) ?: return true
        val (careerClass, targetSlot) = ctx[player.uniqueId] ?: return true

        val branches = Branch.fromCareerClass(careerClass).filter { cp.unlockedBranches.containsKey(it) }
        val branch = branches.getOrNull(idx) ?: return true

        BindSkillSelectGUI.open(player, branch, targetSlot)
        return true
    }

    private fun branchIcon(cp: com.waterful.project.career.model.CareerPlayer, branch: Branch): ItemStack {
        val item = ItemStack(branch.careerClass.material)
        val activeCount = cp.getSkills(branch).count { it.skillDef.skillType == SkillType.ACTIVE && it.currentLevel > 0 && it.skillDef.bindable }
        val eurekaCount = cp.chosenEurekas[branch]?.let {
            if (it.eurekaDef.skillType == SkillType.ACTIVE) 1 else 0
        } ?: 0
        item.editMeta {
            it.displayName(Component.text(branch.displayName, NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("可绑定技能：$activeCount 个", NamedTextColor.GREEN),
                if (eurekaCount > 0) Component.text("可绑定顿悟：$eurekaCount 个", NamedTextColor.LIGHT_PURPLE)
                else Component.empty(),
                Component.text(""),
                Component.text("点击查看详情", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    private fun backButton(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.editMeta { it.displayName(Component.text("返回职业选择", NamedTextColor.YELLOW)) }
        return item
    }

    private fun fill(inv: org.bukkit.inventory.Inventory) {
        val f = IconFactory.fillerPane()
        for (i in 0 until 27) { if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, f) }
    }
}
