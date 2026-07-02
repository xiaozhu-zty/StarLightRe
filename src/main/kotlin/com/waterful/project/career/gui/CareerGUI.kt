package com.waterful.project.career.gui

import com.waterful.project.career.manager.AntiFarmManager
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.ConfirmManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.ResonanceMode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

object CareerGUI {

    const val TITLE = "生涯主面板"

    private val viewingClass = mutableMapOf<UUID, CareerClass>()

    fun cleanup(player: Player) { viewingClass.remove(player.uniqueId) }

    fun open(player: Player) {
        val cp = CareerManager.getPlayer(player) ?: run {
            player.sendMessage("§c数据未加载，请重新登录。"); return
        }

        val inv = Bukkit.createInventory(null, 54, Component.text(TITLE))

        // Default to first selected class
        val viewClass = viewingClass[player.uniqueId]?.takeIf { cp.selectedClasses.contains(it) }
            ?: cp.selectedClasses.firstOrNull()

        // Career class grid (slots 10-12, 19-21)
        val classGrid = listOf(10, 11, 12, 19, 20, 21)
        CareerClass.entries.forEachIndexed { i, clazz ->
            val selected = cp.selectedClasses.contains(clazz)
            val isViewing = (clazz == viewClass)
            inv.setItem(classGrid[i], classIcon(clazz, selected, cp, isViewing))
        }

        // Right side — only after selection complete
        if (cp.isCareerSelected && viewClass != null) {
            viewingClass[player.uniqueId] = viewClass
            val branches = Branch.fromCareerClass(viewClass)
            val branchSlots = listOf(14, 23, 32, 41)
            val resSlots = listOf(15, 24, 33, 42)
            val forgetSlots = listOf(16, 25, 34, 43)

            branches.forEachIndexed { i, branch ->
                inv.setItem(branchSlots[i], branchIcon(branch, cp))
                inv.setItem(resSlots[i], resonateIcon(branch, cp))
                inv.setItem(forgetSlots[i], forgetIcon(branch, cp))
            }

            inv.setItem(37, shortcutIcon(cp))
            inv.setItem(38, resonateTypeIcon(cp))
            inv.setItem(39, resonateInfoIcon(cp))
        }

        inv.setItem(40, scrollIcon(player))
        fillFrame(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val viewClass = viewingClass[player.uniqueId]

        when {
            // Class grid — left = view, right/shift = open detail
            slot in listOf(10, 11, 12, 19, 20, 21) -> {
                val idx = listOf(10, 11, 12, 19, 20, 21).indexOf(slot)
                val clazz = CareerClass.entries.getOrNull(idx) ?: return true
                if (cp.selectedClasses.contains(clazz)) {
                    viewingClass[player.uniqueId] = clazz
                    refresh(player)
                } else if (!cp.isCareerSelected || cp.unlocked) {
                    CareerManager.selectThirdClass(player, clazz)
                    refresh(player)
                }
            }
            // Branch — view detail / unlock
            slot in listOf(14, 23, 32, 41) -> {
                val branch = getBranchForSlot(viewClass, slot, listOf(14, 23, 32, 41)) ?: return true
                if (CareerManager.hasBranch(cp, branch)) {
                    BranchGUI.open(player, branch)
                } else {
                    CareerManager.unlockBranch(player, branch); refresh(player)
                }
            }
            // Resonate — toggle
            slot in listOf(15, 24, 33, 42) -> {
                val branch = getBranchForSlot(viewClass, slot, listOf(15, 24, 33, 42)) ?: return true
                if (!CareerManager.hasBranch(cp, branch)) {
                    player.sendMessage("§c请先解锁该分支！"); return true
                }
                cp.resonantBranch = if (cp.resonantBranch == branch) null else branch
                cp.resonanceModes.putIfAbsent(branch, ResonanceMode.FRIENDLY)
                player.sendMessage("§a共生共鸣分支：${cp.resonantBranch?.displayName ?: "无"}")
                refresh(player)
            }
            // Forget — confirm then execute
            slot in listOf(16, 25, 34, 43) -> {
                val branch = getBranchForSlot(viewClass, slot, listOf(16, 25, 34, 43)) ?: return true
                if (!CareerManager.hasBranch(cp, branch)) { player.sendMessage("§c未解锁该分支！"); return true }
                val cost = cp.getForgetCost(branch)
                ConfirmManager.requestConfirm(player, "遗忘分支「${branch.displayName}」(花费${cost}技能点)") {
                    CareerManager.forgetBranch(player, branch); refresh(player)
                }
            }
            // Shortcut bind
            slot == 37 -> { BindGUI.open(player) }
            // Scroll
            slot == 40 -> giveScroll(player)
        }
        return true
    }

    private fun getBranchForSlot(viewClass: CareerClass?, slot: Int, slots: List<Int>): Branch? {
        viewClass ?: return null
        val idx = slots.indexOf(slot)
        return Branch.fromCareerClass(viewClass).getOrNull(idx)
    }

    private fun refresh(player: Player) { open(player) }

    // ===== Icons =====

    private fun classIcon(clazz: CareerClass, selected: Boolean, cp: CareerPlayer, isViewing: Boolean): ItemStack {
        val item = ItemStack(if (isViewing) Material.LIME_STAINED_GLASS_PANE else if (selected) clazz.material else clazz.material)
        item.editMeta {
            it.displayName(Component.text(clazz.displayName, if (selected) NamedTextColor.GREEN else NamedTextColor.WHITE))
            val desc = mutableListOf<Component>()
            if (isViewing) {
                desc.add(Component.text("✦ 当前查看", NamedTextColor.GOLD))
                desc.add(Component.text("右击打开分支详情", NamedTextColor.GRAY))
            } else if (selected) {
                desc.add(Component.text("已选择 · 点击查看分支", NamedTextColor.GREEN))
            } else if (!cp.isCareerSelected || cp.unlocked) {
                desc.add(Component.text("点击选择此职业", NamedTextColor.YELLOW))
            }
            desc.addAll(IconFactory.wrapLore(clazz.description, NamedTextColor.GRAY))
            desc.add(Component.text(""))
            desc.add(Component.text("下设：${Branch.fromCareerClass(clazz).joinToString("、") { it.displayName }}", NamedTextColor.DARK_GREEN))
            it.lore(desc)
        }
        return item
    }

    private fun branchIcon(branch: Branch, cp: CareerPlayer): ItemStack {
        val has = CareerManager.hasBranch(cp, branch)
        val level = cp.getBranchLevel(branch)
        val item = ItemStack(if (has) branch.careerClass.material else Material.GRAY_DYE)
        item.editMeta {
            it.displayName(Component.text(branch.displayName, if (has) NamedTextColor.GREEN else NamedTextColor.RED))
            val desc = mutableListOf<Component>(
                Component.text("状态：${if (has) "已解锁 Lv.$level" else "未解锁"}", if (has) NamedTextColor.GREEN else NamedTextColor.RED),
                Component.text(""),
                Component.text("基础能力：", NamedTextColor.GOLD)
            )
            desc.addAll(IconFactory.wrapLore(branch.baseEffect, NamedTextColor.GRAY))
            desc.add(Component.text(""))
            if (has) desc.add(Component.text("点击查看技能/顿悟", NamedTextColor.GRAY, TextDecoration.ITALIC))
            else desc.add(Component.text("点击解锁（1技能点）", NamedTextColor.YELLOW))
            it.lore(desc)
        }
        return item
    }

    private fun resonateIcon(branch: Branch, cp: CareerPlayer): ItemStack {
        val isResonant = cp.resonantBranch == branch
        val has = CareerManager.hasBranch(cp, branch)
        val item = ItemStack(when { isResonant -> Material.BELL; has -> Material.GOLD_NUGGET; else -> Material.GRAY_DYE })
        item.editMeta {
            it.displayName(Component.text("共鸣：${branch.displayName}", if (isResonant) NamedTextColor.GOLD else NamedTextColor.GRAY))
            it.lore(listOf(
                if (isResonant) Component.text("✦ 当前共鸣分支", NamedTextColor.GREEN)
                else if (has) Component.text("点击设为共鸣分支", NamedTextColor.YELLOW)
                else Component.text("未解锁", NamedTextColor.RED),
                Component.text("", NamedTextColor.WHITE),
                Component.text("最多共鸣1个分支", NamedTextColor.DARK_GRAY)
            ))
        }
        return item
    }

    private fun forgetIcon(branch: Branch, cp: CareerPlayer): ItemStack {
        val has = CareerManager.hasBranch(cp, branch)
        val cost = if (has) cp.getForgetCost(branch) else 0
        val item = ItemStack(if (has) Material.LAVA_BUCKET else Material.BARRIER)
        item.editMeta {
            it.displayName(Component.text("遗忘：${branch.displayName}", NamedTextColor.GOLD))
            it.lore(if (has) listOf(
                Component.text("点击遗忘（消耗${cost}技能点）", NamedTextColor.RED),
                Component.text("此操作不可逆！", NamedTextColor.DARK_RED)
            ) else listOf(Component.text("未解锁", NamedTextColor.DARK_GRAY)))
        }
        return item
    }

    private fun shortcutIcon(cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta {
            it.displayName(Component.text("快捷释放", NamedTextColor.AQUA))
            it.lore(listOf(Component.text("点击打开绑定面板", NamedTextColor.GRAY)))
        }
        return item
    }

    private fun resonateTypeIcon(cp: CareerPlayer): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta {
            it.displayName(Component.text("共鸣模式", NamedTextColor.YELLOW))
            val branch = cp.resonantBranch
            it.lore(listOf(Component.text("当前：${if (branch != null) cp.getResonanceMode(branch).displayName else "无"}", NamedTextColor.GOLD)))
        }
        return item
    }

    private fun resonateInfoIcon(cp: CareerPlayer): ItemStack {
        val branch = cp.resonantBranch
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta {
            it.displayName(Component.text("共鸣信息", NamedTextColor.YELLOW))
            if (branch != null) {
                val lv = cp.getResonanceLevel(branch)
                val range = cp.getResonanceRange(branch)
                val mode = cp.getResonanceMode(branch)
                it.lore(listOf(
                    Component.text("分支：${branch.displayName}", NamedTextColor.GRAY),
                    Component.text("等级：Lv.$lv  范围：${String.format("%.0f", range)}格", NamedTextColor.GRAY),
                    Component.text("模式：${mode.displayName}", NamedTextColor.GOLD)
                ))
            } else {
                it.lore(listOf(Component.text("未选择共鸣分支", NamedTextColor.DARK_GRAY)))
            }
        }
        return item
    }

    private fun scrollIcon(player: Player): ItemStack {
        val hasScroll = player.inventory.any {
            it != null && run {
                val p = Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin
                p != null && com.waterful.project.career.CareerItems.isScroll(p, it)
            }
        }
        val item = ItemStack(Material.ENCHANTED_BOOK)
        item.editMeta {
            it.displayName(Component.text(if (hasScroll) "技能卷轴 ✓" else "领取技能卷轴", if (hasScroll) NamedTextColor.GREEN else NamedTextColor.GOLD))
            it.lore(listOf(
                if (hasScroll) Component.text("背包中已有", NamedTextColor.GRAY) else Component.text("点击领取", NamedTextColor.YELLOW)
            ))
        }
        return item
    }

    private fun giveScroll(player: Player) {
        val p = Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin ?: return
        player.inventory.addItem(com.waterful.project.career.CareerItems.createScroll(p))
        player.sendMessage("§a✦ 技能卷轴已放入背包")
    }

    private fun fillFrame(inv: org.bukkit.inventory.Inventory) {
        val f = IconFactory.fillerPane()
        for (i in 0 until 54) if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, f)
    }
}
