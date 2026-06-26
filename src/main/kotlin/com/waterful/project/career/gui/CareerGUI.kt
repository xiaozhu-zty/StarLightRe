package com.waterful.project.career.gui

import com.waterful.project.career.manager.AntiFarmManager
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
import org.bukkit.inventory.meta.ItemMeta

/**
 * Career main panel — layout matching StarLightCore reference.
 *
 * Shape (6 rows × 9 cols):
 *   F F F F F F F F F
 *   F C C C F B G R F    C=career class icons (2×3 grid)
 *   F C C C F B G R F    B=branches  G=resonate  R=forget
 *   F F F F F B G R F
 *   F H T I F B G R F    H=shortcut bind  T=resonate type  I=resonate info
 *   F F F F F F F F F
 */
object CareerGUI {

    const val TITLE = "生涯主面板"

    fun open(player: Player) {
        val cp = CareerManager.getPlayer(player) ?: run {
            player.sendMessage("§c数据未加载，请重新登录。"); return
        }

        val inv = Bukkit.createInventory(null, 54, Component.text(TITLE))
        fillFrame(inv)

        // --- Row 1-2 (slots 10-12, 19-21): Career class icons ---
        val classGrid = listOf(10, 11, 12, 19, 20, 21)
        CareerClass.entries.forEachIndexed { i, clazz ->
            val selected = cp.selectedClasses.contains(clazz)
            inv.setItem(classGrid[i], careerClassIcon(clazz, selected, cp))
        }

        // --- Right column: branches (slots 14,23,32,41) ---
        val firstClass = cp.selectedClasses.firstOrNull()
        if (firstClass != null) {
            val branches = Branch.fromCareerClass(firstClass)
            val branchSlots = listOf(14, 23, 32, 41)
            branches.forEachIndexed { i, branch ->
                inv.setItem(branchSlots[i], branchButton(branch, cp))
            }
        }

        // --- Resonate (slots 15,24,33,42) ---
        val resonateSlots = listOf(15, 24, 33, 42)
        if (firstClass != null) {
            Branch.fromCareerClass(firstClass).forEachIndexed { i, branch ->
                val mode = cp.getResonanceMode(branch)
                inv.setItem(resonateSlots[i], resonateButton(branch, mode))
            }
        }

        // --- Forget (slots 16,25,34,43) ---
        val forgetSlots = listOf(16, 25, 34, 43)
        if (firstClass != null) {
            Branch.fromCareerClass(firstClass).forEachIndexed { i, branch ->
                val has = CareerManager.hasBranch(cp, branch)
                inv.setItem(forgetSlots[i], forgetButton(branch, cp, has))
            }
        }

        // --- Bottom row: shortcut, resonate type, resonate info ---
        inv.setItem(37, shortcutBindButton(cp))
        inv.setItem(38, resonateTypeButton(cp))
        inv.setItem(39, resonateInfoButton(cp))

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false

        when {
            // Career class grid
            slot in listOf(10, 11, 12, 19, 20, 21) -> {
                val idx = listOf(10, 11, 12, 19, 20, 21).indexOf(slot)
                val clazz = CareerClass.entries.getOrNull(idx) ?: return true
                if (cp.selectedClasses.contains(clazz)) {
                    player.closeInventory(); ClassGUI.open(player, clazz)
                } else if (!cp.isCareerSelected) {
                    CareerManager.selectThirdClass(player, clazz)
                    player.closeInventory(); open(player)
                }
                return true
            }
            // Branch buttons
            slot in listOf(14, 23, 32, 41) -> {
                val idx = listOf(14, 23, 32, 41).indexOf(slot)
                val clazz = cp.selectedClasses.firstOrNull() ?: return true
                val branch = Branch.fromCareerClass(clazz).getOrNull(idx) ?: return true
                if (CareerManager.hasBranch(cp, branch)) {
                    player.closeInventory(); BranchGUI.open(player, branch)
                } else {
                    CareerManager.unlockBranch(player, branch)
                    player.closeInventory(); open(player)
                }
                return true
            }
            // Resonate
            slot in listOf(15, 24, 33, 42) -> {
                val idx = listOf(15, 24, 33, 42).indexOf(slot)
                val clazz = cp.selectedClasses.firstOrNull() ?: return true
                val branch = Branch.fromCareerClass(clazz).getOrNull(idx) ?: return true
                player.closeInventory(); ResonanceGUI.open(player, branch)
                return true
            }
            // Forget branch — requires double-confirm
            slot in listOf(16, 25, 34, 43) -> {
                val idx = listOf(16, 25, 34, 43).indexOf(slot)
                val clazz = cp.selectedClasses.firstOrNull() ?: return true
                val branch = Branch.fromCareerClass(clazz).getOrNull(idx) ?: return true
                val cost = cp.getForgetCost(branch)
                ConfirmManager.requestConfirm(player, "遗忘分支「${branch.displayName}」(花费${cost}技能点)") {
                    CareerManager.forgetBranch(player, branch)
                    player.closeInventory()
                    open(player)
                }
                return true
            }
            // Shortcut bind
            slot == 37 -> { player.closeInventory(); BindGUI.open(player); return true }
            // Resonate type toggle
            slot == 38 -> { player.sendMessage("§7共鸣模式切换请在分支详情中操作"); return true }
            // Resonate info
            slot == 39 -> { player.sendMessage("§7共鸣信息：正在刷新..."); return true }
        }
        return true
    }

    // ===== Icon Builders =====

    private fun careerClassIcon(clazz: CareerClass, selected: Boolean, cp: CareerPlayer): ItemStack {
        val item = ItemStack(if (selected) Material.LIME_STAINED_GLASS_PANE else clazz.material)
        item.editMeta {
            it.displayName(Component.text(clazz.displayName, if (selected) NamedTextColor.GREEN else NamedTextColor.WHITE))
            val desc = mutableListOf<Component>()
            if (selected) desc.add(Component.text("已选择", NamedTextColor.GREEN))
            desc.addAll(IconFactory.wrapLore(clazz.description, NamedTextColor.GRAY))
            desc.add(Component.text(""))
            desc.add(Component.text("下设分支：${Branch.fromCareerClass(clazz).joinToString("、") { it.displayName }}", NamedTextColor.DARK_GREEN))
            if (selected) desc.add(Component.text("点击查看详情", NamedTextColor.GRAY, TextDecoration.ITALIC))
            else if (!cp.isCareerSelected) desc.add(Component.text("点击选择此职业", NamedTextColor.GOLD))
            it.lore(desc)
        }
        return item
    }

    private fun branchButton(branch: Branch, cp: CareerPlayer): ItemStack {
        val has = CareerManager.hasBranch(cp, branch)
        val level = cp.getBranchLevel(branch)
        val item = ItemStack(if (has) branch.careerClass.material else Material.GRAY_DYE)
        item.editMeta {
            it.displayName(Component.text(branch.displayName, if (has) NamedTextColor.GREEN else NamedTextColor.RED))
            val desc = mutableListOf<Component>()
            desc.add(Component.text("状态：${if (has) "已解锁 Lv.$level" else "未解锁"}", if (has) NamedTextColor.GREEN else NamedTextColor.RED))
            desc.add(Component.text(""))
            desc.add(Component.text("基础能力：", NamedTextColor.GOLD))
            desc.addAll(IconFactory.wrapLore(branch.baseEffect, NamedTextColor.GRAY))
            desc.add(Component.text(""))
            if (has) desc.add(Component.text("左击查看技能/顿悟详情", NamedTextColor.GRAY, TextDecoration.ITALIC))
            else desc.add(Component.text("点击解锁（消耗1技能点）", NamedTextColor.GREEN))
            it.lore(desc)
        }
        return item
    }

    private fun resonateButton(branch: Branch, mode: com.waterful.project.career.model.ResonanceMode): ItemStack {
        val item = ItemStack(Material.BELL)
        item.editMeta {
            it.displayName(Component.text("共鸣：${branch.displayName}", NamedTextColor.YELLOW))
            it.lore(listOf(
                Component.text("当前模式：${mode.displayName}", NamedTextColor.GOLD),
                Component.text(mode.description, NamedTextColor.GRAY),
                Component.text(""),
                Component.text("点击切换共鸣模式", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    private fun forgetButton(branch: Branch, cp: CareerPlayer, has: Boolean): ItemStack {
        val item = ItemStack(if (has) Material.LAVA_BUCKET else Material.BARRIER)
        val cost = if (has) cp.getForgetCost(branch) else 0
        item.editMeta {
            it.displayName(Component.text("遗忘：${branch.displayName}", NamedTextColor.GOLD))
            if (has) {
                it.lore(listOf(
                    Component.text("点击永久遗忘此分支", NamedTextColor.RED),
                    Component.text("消耗：$cost 技能点", NamedTextColor.GRAY),
                    Component.text("注意：此操作不可逆！", NamedTextColor.DARK_RED)
                ))
            } else {
                it.lore(listOf(Component.text("未解锁，无法遗忘", NamedTextColor.DARK_GRAY)))
            }
        }
        return item
    }

    private fun shortcutBindButton(cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta {
            it.displayName(Component.text("快捷释放", NamedTextColor.AQUA))
            it.lore(listOf(
                Component.text("将技能/顿悟绑定到键盘", NamedTextColor.GRAY),
                Component.text("通过职业信物释放它们", NamedTextColor.GRAY),
                Component.text(""),
                Component.text("点击打开绑定菜单", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    private fun resonateTypeButton(cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta {
            it.displayName(Component.text("共鸣模式", NamedTextColor.YELLOW))
            it.lore(listOf(
                Component.text("点击切换共鸣模式", NamedTextColor.GRAY),
                Component.text("模式列表：关闭/全局/友善/内部/独奏", NamedTextColor.GRAY)
            ))
        }
        return item
    }

    private fun resonateInfoButton(cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val resonating = cp.resonanceModes.filter { it.value != com.waterful.project.career.model.ResonanceMode.DISABLED }
        item.editMeta {
            it.displayName(Component.text("共鸣信息", NamedTextColor.YELLOW))
            val desc = mutableListOf<Component>()
            resonating.forEach { (br, mode) ->
                desc.add(Component.text("${br.displayName}: ${mode.displayName} Lv.${cp.getResonanceLevel(br)}", NamedTextColor.GRAY))
            }
            if (resonating.isEmpty()) desc.add(Component.text("当前无共鸣分支", NamedTextColor.DARK_GRAY))
            it.lore(desc)
        }
        return item
    }

    private fun fillFrame(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        for (i in 0 until 54) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) {
                inv.setItem(i, filler)
            }
        }
    }
}
