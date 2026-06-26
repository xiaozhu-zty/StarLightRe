package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Level 2 bind GUI: select a career class to see its branches.
 */
object BindClassGUI {

    const val TITLE = "选择职业 — 绑定热键"

    fun open(player: Player, targetSlot: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        val inv = Bukkit.createInventory(null, 27, Component.text(TITLE))

        val slots = listOf(10, 11, 12, 13, 14, 15)
        CareerClass.entries.forEachIndexed { i, clazz ->
            val hasBranch = cp.unlockedBranches.keys.any { it.careerClass == clazz }
            inv.setItem(slots[i], classIcon(clazz, hasBranch, targetSlot))
        }

        // Back to bind panel
        inv.setItem(22, backButton())
        fill(inv)
        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory): Boolean {
        if (slot == 22) { player.closeInventory(); BindGUI.open(player); return true }
        val slots = listOf(10, 11, 12, 13, 14, 15)
        val idx = slots.indexOf(slot)
        if (idx < 0) return true
        val clazz = CareerClass.entries.getOrNull(idx) ?: return true
        player.closeInventory()
        BindBranchGUI.open(player, clazz, -1) // targetSlot will be passed through
        return true
    }

    private fun classIcon(clazz: CareerClass, hasBranch: Boolean, slot: Int): ItemStack {
        val item = ItemStack(if (hasBranch) clazz.material else Material.GRAY_DYE)
        item.editMeta {
            it.displayName(Component.text(clazz.displayName, if (hasBranch) NamedTextColor.GOLD else NamedTextColor.GRAY))
            val desc = mutableListOf<Component>()
            if (hasBranch) {
                desc.add(Component.text("已解锁分支：", NamedTextColor.GREEN))
                Branch.fromCareerClass(clazz).forEach { b ->
                    if (CareerManager.getPlayer(Bukkit.getPlayer(clazz.displayName) ?: return@forEach)?.unlockedBranches?.containsKey(b) == true)
                        desc.add(Component.text("  ✓ ${b.displayName}", NamedTextColor.GRAY))
                }
                desc.add(Component.text(""))
                desc.add(Component.text("点击查看可绑定技能", NamedTextColor.GOLD))
            } else {
                desc.add(Component.text("未解锁该职业的分支", NamedTextColor.RED))
            }
            it.lore(desc)
        }
        return item
    }

    private fun backButton(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.editMeta { it.displayName(Component.text("返回绑定面板", NamedTextColor.YELLOW)) }
        return item
    }

    private fun fill(inv: org.bukkit.inventory.Inventory) {
        val f = IconFactory.fillerPane()
        for (i in 0 until 27) { if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, f) }
    }
}
