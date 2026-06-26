package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.ConfirmManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object DelCareerGUI {

    const val TITLE = "重置管理 — 点击删除"

    fun open(player: Player) {
        val cp = CareerManager.getPlayer(player) ?: run {
            player.sendMessage("§c数据未加载，请重新登录。")
            return
        }

        val inv = Bukkit.createInventory(null, 54, Component.text(TITLE))
        fillBackground(inv)

        // Row 0: Title + close
        inv.setItem(0, createTitleItem())
        inv.setItem(4, IconFactory.skillPointDisplay(cp.skillPoints))
        inv.setItem(8, IconFactory.closeButton())

        // Row 1: Reset ALL careers button (slot 13)
        inv.setItem(13, createResetAllCareersButton())

        // Row 2-3: Show selected careers with reset option
        val classSlots = listOf(20, 22, 24)
        cp.selectedClasses.forEachIndexed { index, careerClass ->
            if (index < classSlots.size) {
                val branches = cp.unlockedBranches.keys.filter { it.careerClass == careerClass }
                inv.setItem(classSlots[index], createCareerResetButton(careerClass, branches.size))
            }
        }

        // Row 4: Show unlocked branches with individual delete buttons
        val branchSlots = listOf(29, 31, 33, 37, 39, 41)
        val unlockedBranches = cp.unlockedBranches.keys.toList()
        unlockedBranches.forEachIndexed { index, branch ->
            if (index < branchSlots.size) {
                inv.setItem(branchSlots[index], createBranchDeleteButton(cp, branch))
            }
        }

        // Row 5: Reset all branches (keep classes)
        inv.setItem(49, createResetAllBranchesButton(cp))

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false

        when (slot) {
            8, 53 -> { player.closeInventory(); return true }

            // All destructive ops require double-confirm
            13 -> confirm(player, "重置全部职业", { handleResetAllCareers(player, cp) })
            20 -> confirm(player, "重置职业", { handleCareerReset(player, cp, 0) })
            22 -> confirm(player, "重置职业", { handleCareerReset(player, cp, 1) })
            24 -> confirm(player, "重置职业", { handleCareerReset(player, cp, 2) })
            29 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 0) })
            31 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 1) })
            33 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 2) })
            37 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 3) })
            39 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 4) })
            41 -> confirm(player, "删除分支", { handleBranchDelete(player, cp, 5) })
            49 -> confirm(player, "重置所有分支（保留职业）", { handleResetAllBranches(player, cp) })
        }
        return true
    }

    private fun handleCareerReset(player: Player, cp: CareerPlayer, index: Int) {
        val careerClass = cp.selectedClasses.getOrNull(index) ?: return
        val branches = cp.unlockedBranches.keys.filter { it.careerClass == careerClass }.toList()

        // Remove all branches of this career
        branches.forEach { branch ->
            cp.unlockedBranches.remove(branch)
            cp.chosenEurekas.remove(branch)
            cp.resonanceModes.remove(branch)
        }

        // Remove the career class itself
        cp.selectedClasses.remove(careerClass)

        player.sendMessage("§a✦ 已重置职业：§e${careerClass.displayName}§a（移除了 ${branches.size} 个分支）")
        player.sendMessage("§7请使用 Shift+F 打开生涯面板重新选择职业")

        player.closeInventory()
        open(player) // Refresh
    }

    private fun handleBranchDelete(player: Player, cp: CareerPlayer, index: Int) {
        val branch = cp.unlockedBranches.keys.toList().getOrNull(index) ?: return

        // Remove the branch
        cp.unlockedBranches.remove(branch)
        cp.chosenEurekas.remove(branch)
        cp.resonanceModes.remove(branch)
        cp.autoCastSkills.keys.removeAll { it.startsWith(branch.name) }
        cp.cooldowns.keys.removeAll { it.startsWith(branch.name) }

        player.sendMessage("§a✦ 已删除分支：§e${branch.displayName}§a（${branch.careerClass.displayName}）")

        player.closeInventory()
        open(player) // Refresh
    }

    private fun handleResetAllCareers(player: Player, cp: CareerPlayer) {
        // Remove everything
        cp.unlockedBranches.clear()
        cp.chosenEurekas.clear()
        cp.resonanceModes.clear()
        cp.selectedClasses.clear()
        cp.autoCastSkills.clear()
        cp.cooldowns.clear()

        player.sendMessage("§a✦ 已重置所有职业和分支！")
        player.sendMessage("§7请使用 Shift+F 打开生涯面板重新选择职业")

        player.closeInventory()
    }

    private fun handleResetAllBranches(player: Player, cp: CareerPlayer) {
        // Remove branches only, keep career selection
        cp.unlockedBranches.clear()
        cp.chosenEurekas.clear()
        cp.resonanceModes.clear()
        cp.autoCastSkills.clear()
        cp.cooldowns.clear()

        player.sendMessage("§a✦ 已重置所有分支（保留职业选择）！")

        player.closeInventory()
        open(player) // Refresh
    }

    private fun confirm(player: Player, desc: String, action: () -> Unit) {
        ConfirmManager.requestConfirm(player, "删除操作：$desc", action)
    }

    // ===== Icon factories =====

    private fun createTitleItem(): ItemStack {
        val item = ItemStack(Material.TNT_MINECART)
        item.editMeta {
            it.displayName(Component.text("⚠ 重置管理", NamedTextColor.RED))
            it.lore(listOf(
                Component.text("点击下方按钮删除职业或分支", NamedTextColor.GRAY),
                Component.text("此操作不可撤销！", NamedTextColor.RED, TextDecoration.BOLD)
            ))
        }
        return item
    }

    private fun createResetAllCareersButton(): ItemStack {
        val item = ItemStack(Material.LAVA_BUCKET)
        item.editMeta {
            it.displayName(Component.text("⚠ 重置全部职业", NamedTextColor.DARK_RED))
            it.lore(listOf(
                Component.text("删除所有职业选择", NamedTextColor.RED),
                Component.text("删除所有分支、技能、顿悟", NamedTextColor.RED),
                Component.text(""),
                Component.text("点击重置", NamedTextColor.DARK_RED, TextDecoration.BOLD)
            ))
        }
        return item
    }

    private fun createCareerResetButton(careerClass: CareerClass, branchCount: Int): ItemStack {
        val item = ItemStack(careerClass.material)
        item.editMeta {
            it.displayName(Component.text("重置：${careerClass.displayName}", NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("已解锁 $branchCount 个分支", NamedTextColor.GRAY),
                Component.text(""),
                Component.text("点击移除此职业及其所有分支", NamedTextColor.RED),
                Component.text("", NamedTextColor.WHITE),
                Component.text("之后可重新选择新职业", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    private fun createBranchDeleteButton(cp: CareerPlayer, branch: Branch): ItemStack {
        val level = cp.getBranchLevel(branch)
        val hasEureka = cp.chosenEurekas.containsKey(branch)
        val item = ItemStack(if (hasEureka) Material.NETHER_STAR else Material.TNT)
        item.editMeta {
            it.displayName(Component.text("删除：${branch.displayName}", NamedTextColor.RED))
            it.lore(listOf(
                Component.text("所属：${branch.careerClass.displayName}", NamedTextColor.GRAY),
                Component.text("等级：Lv.$level", NamedTextColor.AQUA),
                if (hasEureka) Component.text("已解锁顿悟（将丢失）", NamedTextColor.LIGHT_PURPLE)
                else Component.empty(),
                Component.text(""),
                Component.text("点击删除此分支", NamedTextColor.DARK_RED)
            ))
        }
        return item
    }

    private fun createResetAllBranchesButton(cp: CareerPlayer): ItemStack {
        val totalBranches = cp.unlockedBranches.size
        if (totalBranches == 0) {
            val item = ItemStack(Material.BARRIER)
            item.editMeta {
                it.displayName(Component.text("无已解锁分支", NamedTextColor.GRAY))
                it.lore(listOf(Component.text("没有可删除的分支", NamedTextColor.DARK_GRAY)))
            }
            return item
        }
        val item = ItemStack(Material.WATER_BUCKET)
        item.editMeta {
            it.displayName(Component.text("⚠ 重置所有分支", NamedTextColor.RED))
            it.lore(listOf(
                Component.text("当前已解锁 $totalBranches 个分支", NamedTextColor.GRAY),
                Component.text(""),
                Component.text("删除所有分支、技能、顿悟", NamedTextColor.RED),
                Component.text("保留职业选择", NamedTextColor.GREEN),
                Component.text(""),
                Component.text("点击重置", NamedTextColor.RED)
            ))
        }
        return item
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        val emptySlots = (0 until 54).filter {
            inv.getItem(it) == null || inv.getItem(it)?.type == Material.AIR
        }
        emptySlots.forEach { inv.setItem(it, filler) }
    }
}
