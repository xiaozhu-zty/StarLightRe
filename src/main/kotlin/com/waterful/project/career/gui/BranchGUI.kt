package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Branch detail panel — layout matching StarLightCore reference.
 *
 * Shape (6 rows × 9 cols):
 *   F F F F F F F F F
 *   F I F S S S F P F    I=branch info  S=skills  P=eureka guide
 *   F M F X X X F E F    M=skill points  X=level details  E=eureka
 *   F F F X X X F E F
 *   F B F X X X F E F    B=back button
 *   F F F F F F F F F
 */
object BranchGUI {

    const val TITLE_PREFIX = "分支面板："

    fun open(player: Player, branch: Branch) {
        val cp = CareerManager.getPlayer(player) ?: return
        val level = cp.getBranchLevel(branch)
        val skills = cp.getSkills(branch)
        val isMaxed = cp.isBranchMaxed(branch)
        val hasEureka = cp.chosenEurekas.containsKey(branch)
        val canUpgrade = SkillPointManager.hasEnough(cp, 1)

        val title = Component.text("$TITLE_PREFIX${branch.displayName}")
        val inv = Bukkit.createInventory(null, 54, title)
        fillFrame(inv)

        // Row 1 (slots 10, 12-14, 16):
        inv.setItem(10, branchInfoIcon(branch, cp))           // Branch info
        skills.forEachIndexed { i, skill ->                   // Skill icons
            inv.setItem(12 + i, skillHeaderIcon(skill, i))
        }
        inv.setItem(16, eurekaGuideIcon(branch, isMaxed, hasEureka)) // Eureka guide

        // Row 2 (slots 10, 12-14, 16):
        inv.setItem(19, skillPointIcon(cp.skillPoints))       // Skill points
        skills.forEachIndexed { i, skill ->                   // Skill level 1 details
            inv.setItem(21 + i, skillLevelIcon(skill, 1, level))
        }
        val eurekaList = CareerManager.getEurekaOptions(branch)
        if (eurekaList.isNotEmpty()) {
            inv.setItem(25, eurekaDetailIcon(eurekaList.getOrNull(0), cp, branch))
        }

        // Row 3 (slots 12-14, 16):
        skills.forEachIndexed { i, skill ->                   // Skill level 2 details
            inv.setItem(30 + i, skillLevelIcon(skill, 2, level))
        }
        if (eurekaList.size > 1) {
            inv.setItem(34, eurekaDetailIcon(eurekaList.getOrNull(1), cp, branch))
        }

        // Row 4 (slots 10, 12-14, 16):
        inv.setItem(37, backButton())                          // Back
        skills.forEachIndexed { i, skill ->                   // Skill level 3 details
            inv.setItem(39 + i, skillLevelIcon(skill, 3, level))
        }
        if (eurekaList.size > 2) {
            inv.setItem(43, eurekaDetailIcon(eurekaList.getOrNull(2), cp, branch))
        }

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch, isShift: Boolean = false): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false

        when (slot) {
            // Back
            37 -> { player.closeInventory(); ClassGUI.open(player, branch.careerClass); return true }

            // Skill headers (click to upgrade or bind)
            12 -> handleSkillClick(player, cp, branch, 0, isShift)
            13 -> handleSkillClick(player, cp, branch, 1, isShift)
            14 -> handleSkillClick(player, cp, branch, 2, isShift)

            // Skill level details (click to upgrade)
            21, 30, 39 -> handleSkillLevelClick(player, cp, branch, 0, slot)
            22, 31, 40 -> handleSkillLevelClick(player, cp, branch, 1, slot)
            23, 32, 41 -> handleSkillLevelClick(player, cp, branch, 2, slot)

            // Eureka guide / details
            16 -> handleEurekaGuideClick(player, cp, branch)
            25 -> handleEurekaChooseClick(player, cp, branch, 0)
            34 -> handleEurekaChooseClick(player, cp, branch, 1)
            43 -> handleEurekaChooseClick(player, cp, branch, 2)
        }
        return true
    }

    // ===== Click Handlers =====

    private fun handleSkillClick(player: Player, cp: CareerPlayer, branch: Branch, index: Int, isShift: Boolean) {
        val skill = cp.getSkill(branch, index) ?: return
        if (isShift && skill.skillDef.skillType == SkillType.ACTIVE && skill.currentLevel > 0) {
            // Redirect to BindGUI for hotkey management
            player.closeInventory()
            player.sendMessage("§6请在热键绑定面板 (Shift+2) 中管理快捷键")
            return
        }
        if (skill.isMaxed) { player.sendMessage("§c该技能已满级！"); return }
        if (!SkillPointManager.hasEnough(cp, 1)) { player.sendMessage("§c技能点不足！"); return }
        CareerManager.levelUpSkill(player, branch, index)
        player.closeInventory(); open(player, branch) // Refresh
    }

    private fun handleSkillLevelClick(player: Player, cp: CareerPlayer, branch: Branch, skillIndex: Int, slot: Int) {
        val skill = cp.getSkill(branch, skillIndex) ?: return
        val targetLevel = when (slot) {
            in 21..23 -> 1; in 30..32 -> 2; in 39..41 -> 3; else -> return
        }
        if (skill.currentLevel >= targetLevel) { player.sendMessage("§7已解锁该等级效果"); return }
        // Show what's needed to reach this level
        player.sendMessage("§e升级到 Lv.$targetLevel 需要 ${targetLevel - skill.currentLevel} 技能点")
    }

    private fun handleEurekaGuideClick(player: Player, cp: CareerPlayer, branch: Branch) {
        if (cp.chosenEurekas.containsKey(branch)) { player.sendMessage("§c该分支已选择过顿悟！"); return }
        if (!cp.isBranchMaxed(branch)) { player.sendMessage("§c需要所有技能满级才能解锁顿悟！"); return }
        player.closeInventory(); EurekaGUI.open(player, branch)
    }

    private fun handleEurekaChooseClick(player: Player, cp: CareerPlayer, branch: Branch, index: Int) {
        if (cp.chosenEurekas.containsKey(branch)) { player.sendMessage("§c该分支已选择过顿悟！"); return }
        if (!cp.isBranchMaxed(branch)) { player.sendMessage("§c需要所有技能满级才能解锁顿悟！"); return }
        CareerManager.chooseEureka(player, branch, index)
        player.closeInventory(); open(player, branch)
    }

    // ===== Icon Builders =====

    private fun branchInfoIcon(branch: Branch, cp: CareerPlayer): ItemStack {
        val level = cp.getBranchLevel(branch)
        val item = ItemStack(branch.careerClass.material)
        val skills = cp.getSkills(branch)
        item.editMeta {
            it.displayName(Component.text(branch.displayName, NamedTextColor.GOLD))
            val desc = mutableListOf<Component>()
            desc.add(Component.text("状态：${if (level >= 0) "已解锁 Lv.$level" else "未解锁"}", NamedTextColor.GREEN))
            desc.add(Component.text(""))
            desc.add(Component.text("可升级的技能：", NamedTextColor.AQUA))
            skills.forEach { s -> desc.add(Component.text("  |—— ${s.skillDef.name} (Lv.${s.currentLevel}/3)", NamedTextColor.GRAY)) }
            desc.add(Component.text(""))
            desc.add(Component.text("顿悟：${if (cp.chosenEurekas.containsKey(branch)) cp.chosenEurekas[branch]!!.eurekaDef.name else "未解锁"}", NamedTextColor.LIGHT_PURPLE))
            it.lore(desc)
        }
        return item
    }

    private fun skillHeaderIcon(skill: com.waterful.project.career.model.SkillInstance, index: Int): ItemStack {
        val type = if (skill.skillDef.skillType == SkillType.PASSIVE) "被动" else "主动"
        val item = if (skill.skillDef.skillType == SkillType.PASSIVE) ItemStack(Material.BOOK) else ItemStack(Material.BLAZE_ROD)
        item.editMeta {
            it.displayName(Component.text("${skill.skillDef.name} [$type]", if (skill.skillDef.skillType == SkillType.PASSIVE) NamedTextColor.AQUA else NamedTextColor.GOLD))
            val desc = mutableListOf<Component>()
            desc.add(Component.text("等级：Lv.${skill.currentLevel}/3", NamedTextColor.YELLOW))
            desc.add(Component.text(""))
            desc.add(Component.text("点击升级此技能", NamedTextColor.GRAY, TextDecoration.ITALIC))
            if (skill.skillDef.skillType == SkillType.ACTIVE && skill.currentLevel > 0) {
                desc.add(Component.text("Shift+左击绑定快捷键", NamedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC))
            }
            it.lore(desc)
        }
        return item
    }

    private fun skillLevelIcon(skill: com.waterful.project.career.model.SkillInstance, displayLevel: Int, branchLevel: Int): ItemStack {
        val unlocked = skill.currentLevel >= displayLevel
        val item = ItemStack(when {
            unlocked -> Material.LIME_STAINED_GLASS_PANE
            displayLevel == 1 -> Material.YELLOW_STAINED_GLASS_PANE
            displayLevel == 2 -> Material.ORANGE_STAINED_GLASS_PANE
            else -> Material.RED_STAINED_GLASS_PANE
        })
        item.editMeta {
            val roman = when (displayLevel) { 1 -> "I"; 2 -> "II"; 3 -> "III"; else -> "" }
            it.displayName(Component.text(
                "${skill.skillDef.name} $roman",
                if (unlocked) NamedTextColor.GREEN else NamedTextColor.RED
            ))
            val desc = mutableListOf<Component>()
            desc.add(Component.text("状态：${if (unlocked) "已解锁" else "未解锁"}", if (unlocked) NamedTextColor.GREEN else NamedTextColor.RED))
            if (unlocked) {
                desc.add(Component.text(""))
                desc.add(Component.text("效果：", NamedTextColor.GOLD))
                desc.addAll(IconFactory.wrapLore(skill.skillDef.getDescription(displayLevel), NamedTextColor.GRAY))
            } else {
                desc.add(Component.text("需要升级到此等级", NamedTextColor.GRAY))
            }
            it.lore(desc)
        }
        return item
    }

    private fun eurekaGuideIcon(branch: Branch, isMaxed: Boolean, hasEureka: Boolean): ItemStack {
        val item = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        item.editMeta {
            it.displayName(Component.text("顿悟", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("状态：${if (hasEureka) "已解锁" else if (isMaxed) "可解锁" else "锁定"}", if (hasEureka) NamedTextColor.GREEN else if (isMaxed) NamedTextColor.GOLD else NamedTextColor.RED),
                Component.text(""),
                Component.text("所有技能满级后可解锁顿悟", NamedTextColor.GRAY),
                Component.text("每个分支只能解锁一个顿悟", NamedTextColor.RED)
            ))
        }
        return item
    }

    private fun eurekaDetailIcon(eureka: com.waterful.project.career.model.EurekaDef?, cp: CareerPlayer, branch: Branch): ItemStack {
        val item = ItemStack(Material.ENDER_EYE)
        val chosen = cp.chosenEurekas[branch]?.eurekaDef?.id == eureka?.id
        item.editMeta {
            it.displayName(Component.text("顿悟：${eureka?.name ?: "无"}", NamedTextColor.LIGHT_PURPLE))
            val desc = mutableListOf<Component>()
            if (eureka != null) {
                desc.add(Component.text("能力：", NamedTextColor.GOLD))
                desc.addAll(IconFactory.wrapLore(eureka.description, NamedTextColor.GRAY))
                desc.add(Component.text(""))
                if (chosen) desc.add(Component.text("✦ 已选择", NamedTextColor.GREEN))
                else if (cp.isBranchMaxed(branch)) desc.add(Component.text("点击解锁此顿悟", NamedTextColor.GOLD))
                else desc.add(Component.text("需要所有技能满级", NamedTextColor.RED))
                if (eureka.cooldownSeconds > 0) desc.add(Component.text("冷却：${eureka.cooldownSeconds}秒", NamedTextColor.AQUA))
            }
            it.lore(desc)
        }
        return item
    }

    private fun skillPointIcon(points: Int): ItemStack = IconFactory.skillPointDisplay(points)

    private fun backButton(): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta { it.displayName(Component.text("返回职业菜单", NamedTextColor.YELLOW)) }
        return item
    }

    private fun fillFrame(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        for (i in 0 until 54) {
            if (inv.getItem(i) == null || inv.getItem(i)?.type == Material.AIR) inv.setItem(i, filler)
        }
    }
}
