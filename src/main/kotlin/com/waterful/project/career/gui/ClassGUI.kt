package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object ClassGUI {

    const val TITLE_PREFIX = "职业面板："

    fun open(player: Player, careerClass: CareerClass) {
        val cp = CareerManager.getPlayer(player) ?: return
        val branches = Branch.fromCareerClass(careerClass)

        val title = Component.text("$TITLE_PREFIX${careerClass.displayName}")
        val inv = Bukkit.createInventory(null, 54, title)
        fillBackground(inv)

        // Row 0: Class header
        inv.setItem(0, IconFactory.classHeaderIcon(careerClass))
        inv.setItem(4, IconFactory.skillPointDisplay(cp.skillPoints))
        inv.setItem(8, IconFactory.backButton())

        // Branches: slot 20, 22, 24, 30
        val branchSlots = listOf(20, 22, 24, 30)
        branches.forEachIndexed { index, branch ->
            val isUnlocked = CareerManager.hasBranch(cp, branch)
            val level = cp.getBranchLevel(branch)
            val canUnlock = !isUnlocked && CareerManager.canUnlockBranch(cp, branch)

            inv.setItem(branchSlots[index], IconFactory.branchIcon(branch, level, isUnlocked, canUnlock))
        }

        // Bottom
        inv.setItem(53, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, careerClass: CareerClass): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val branches = Branch.fromCareerClass(careerClass)
        val branchSlots = mapOf(20 to 0, 22 to 1, 24 to 2, 30 to 3)

        when (slot) {
            8 -> { CareerGUI.open(player); return true }  // Back to main
            53 -> { player.closeInventory(); return true }

            in branchSlots -> {
                val branchIndex = branchSlots[slot] ?: return true
                val branch = branches.getOrNull(branchIndex) ?: return true

                if (CareerManager.hasBranch(cp, branch)) {
                    // Open branch detail
                    player.closeInventory()
                    BranchGUI.open(player, branch)
                } else {
                    // Try to unlock
                    if (CareerManager.unlockBranch(player, branch)) {
                        player.closeInventory()
                        ClassGUI.open(player, careerClass) // Refresh
                    }
                }
            }
        }
        return true
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        val emptySlots = listOf(1, 2, 3, 5, 6, 7,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 21, 23, 25, 26,
            27, 28, 29, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52)
        emptySlots.forEach { if (inv.getItem(it) == null) inv.setItem(it, filler) }
    }
}
