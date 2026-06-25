package com.waterful.project.career.gui

import com.waterful.project.career.manager.AntiFarmManager
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CareerGUI {

    const val TITLE = "生涯主面板"

    fun open(player: Player) {
        val cp = CareerManager.getPlayer(player) ?: run {
            player.sendMessage("§c数据未加载，请重新登录。")
            return
        }

        val inv = Bukkit.createInventory(null, 54, Component.text(TITLE))
        fillBackground(inv)

        // Row 0: Player info
        inv.setItem(0, IconFactory.playerHead(player, cp.skillPoints))
        inv.setItem(1, createPlayerInfoItem(cp, player))
        inv.setItem(2, IconFactory.skillPointDisplay(cp.skillPoints))
        inv.setItem(3, createPenaltyInfoItem(cp))
        inv.setItem(8, IconFactory.closeButton())

        // Row 2 (slots 18-26): 6 career classes
        val classSlots = listOf(18, 19, 20, 21, 22, 23)
        CareerClass.entries.forEachIndexed { index, careerClass ->
            val isSelected = cp.selectedClasses.contains(careerClass)
            val isLocked = !isSelected && cp.isCareerSelected
            // Locked means: player already has 3 classes and this one isn't among them
            val icon = if (isLocked) {
                IconFactory.careerClassIcon(careerClass, false, true)
            } else if (isSelected) {
                IconFactory.careerClassIcon(careerClass, true, false)
            } else {
                // Available for selection
                IconFactory.careerClassIcon(careerClass, false, false)
            }
            inv.setItem(classSlots[index], icon)
        }

        // Row 3: Additional info if still selecting
        if (!cp.isCareerSelected) {
            val selectedCount = cp.selectedClasses.size
            inv.setItem(27, IconFactory.selectRemainingClassHint(CareerPlayer.INITIAL_CLASSES - selectedCount))
            // Show remaining classes for selection
            val available = CareerManager.getAvailableClasses(cp)
            val availableSlots = listOf(29, 30, 31, 32)
            available.take(4).forEachIndexed { index, careerClass ->
                inv.setItem(availableSlots[index], IconFactory.careerClassIcon(careerClass, false, false))
            }
        } else {
            // Show hint to click on class to view branches
            inv.setItem(27, createClassViewHint())
        }

        // Bottom row
        inv.setItem(53, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false

        when (slot) {
            8, 53 -> { player.closeInventory(); return true }

            18 -> handleClassClick(player, cp, CareerClass.ARCHITECT, slot)
            19 -> handleClassClick(player, cp, CareerClass.CHEF, slot)
            20 -> handleClassClick(player, cp, CareerClass.SCHOLAR, slot)
            21 -> handleClassClick(player, cp, CareerClass.FARMER, slot)
            22 -> handleClassClick(player, cp, CareerClass.WORKER, slot)
            23 -> handleClassClick(player, cp, CareerClass.WARRIOR, slot)

            // Remaining class selection slots
            29 -> applyIfAvailable(cp, 0) { selectClass(player, cp, it) }
            30 -> applyIfAvailable(cp, 1) { selectClass(player, cp, it) }
            31 -> applyIfAvailable(cp, 2) { selectClass(player, cp, it) }
            32 -> applyIfAvailable(cp, 3) { selectClass(player, cp, it) }
        }
        return true
    }

    private fun handleClassClick(player: Player, cp: CareerPlayer, careerClass: CareerClass, slot: Int) {
        if (cp.selectedClasses.contains(careerClass)) {
            // Open ClassGUI for this class
            player.closeInventory()
            ClassGUI.open(player, careerClass)
            return
        }

        // If not selected and not yet at max, select it
        if (!cp.isCareerSelected && !cp.selectedClasses.contains(careerClass)) {
            selectClass(player, cp, careerClass)
            player.closeInventory()
            open(player) // Refresh
        }
    }

    private fun selectClass(player: Player, cp: CareerPlayer, careerClass: CareerClass) {
        if (CareerManager.selectThirdClass(player, careerClass)) {
            // Check if career selection is now complete
            if (cp.isCareerSelected) {
                player.sendMessage("§a✦ 职业选择完成！你的职业：${cp.selectedClasses.joinToString("、") { it.displayName }}")
                player.sendMessage("§7使用 Shift+F 或 /career 打开生涯面板")
            }
        }
    }

    private fun applyIfAvailable(cp: CareerPlayer, index: Int, action: (CareerClass) -> Unit) {
        val available = CareerManager.getAvailableClasses(cp)
        if (index < available.size) {
            action(available[index])
        }
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        for (i in 0 until 54) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) {
                // We'll fill non-special slots later
            }
        }
        // Fill specific empty slots
        val emptySlots = listOf(4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17,
            24, 25, 26, 28, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52)
        emptySlots.forEach { if (inv.getItem(it) == null) inv.setItem(it, filler) }
    }

    private fun createPlayerInfoItem(cp: CareerPlayer, player: Player): ItemStack {
        val item = ItemStack(Material.NAME_TAG)
        val penaltyRemaining = AntiFarmManager.getPenaltyRemainingHours(player)
        item.editMeta {
            it.displayName(Component.text("玩家信息", NamedTextColor.WHITE))
            it.lore(listOf(
                Component.text("ID：${cp.name}", NamedTextColor.GRAY),
                Component.text("已选职业：${cp.selectedClasses.size}/3", NamedTextColor.GRAY),
                Component.text("已解锁分支：${cp.unlockedBranches.size}/6", NamedTextColor.GRAY),
                Component.text("技能点：${cp.skillPoints}", NamedTextColor.AQUA),
                if (penaltyRemaining > 0)
                    Component.text("⚠ 自杀惩罚剩余：${penaltyRemaining}小时", NamedTextColor.RED)
                else Component.empty()
            ))
        }
        return item
    }

    private fun createPenaltyInfoItem(cp: CareerPlayer): ItemStack {
        val now = System.currentTimeMillis()
        val item = ItemStack(if (cp.antiFarmData.isSuicidePenaltyActive(now)) Material.REDSTONE else Material.EMERALD)
        item.editMeta {
            if (cp.antiFarmData.isSuicidePenaltyActive(now)) {
                it.displayName(Component.text("⚠ 自杀惩罚生效中", NamedTextColor.RED))
                it.lore(listOf(
                    Component.text("1小时内死亡过多，触发自杀惩罚", NamedTextColor.RED),
                    Component.text("24小时内重生只分配1个职业", NamedTextColor.RED)
                ))
            } else {
                it.displayName(Component.text("状态正常", NamedTextColor.GREEN))
                it.lore(listOf(
                    Component.text("无惩罚状态", NamedTextColor.GREEN)
                ))
            }
        }
        return item
    }

    private fun createClassViewHint(): ItemStack {
        val item = ItemStack(Material.KNOWLEDGE_BOOK)
        item.editMeta {
            it.displayName(Component.text("查看职业分支", NamedTextColor.YELLOW))
            it.lore(listOf(
                Component.text("点击上方已选择的职业图标", NamedTextColor.GRAY),
                Component.text("查看和管理该职业下的分支", NamedTextColor.GRAY)
            ))
        }
        return item
    }
}
