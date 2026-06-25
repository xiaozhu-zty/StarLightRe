package com.waterful.project.career.gui

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object BranchGUI {

    const val TITLE_PREFIX = "分支面板："

    fun open(player: Player, branch: Branch) {
        val cp = CareerManager.getPlayer(player) ?: return
        val branchLevel = cp.getBranchLevel(branch)
        val skills = cp.getSkills(branch)
        val mode = cp.getResonanceMode(branch)
        val isMaxed = cp.isBranchMaxed(branch)
        val hasEureka = cp.chosenEurekas.containsKey(branch)
        val forgetCost = cp.getForgetCost(branch)
        val canAffordForget = SkillPointManager.hasEnough(cp, forgetCost)
        val canUpgrade = SkillPointManager.hasEnough(cp, 1)

        val title = Component.text("$TITLE_PREFIX${branch.displayName}")
        val inv = Bukkit.createInventory(null, 54, title)
        fillBackground(inv)

        // Row 0: Branch header
        inv.setItem(0, IconFactory.branchHeaderIcon(branch, branchLevel))
        inv.setItem(4, IconFactory.skillPointDisplay(cp.skillPoints))
        inv.setItem(8, IconFactory.backButton())

        // Row 1: Base effect
        inv.setItem(10, IconFactory.baseEffectDisplay(branch))

        // Row 2: Resonance mode selector
        inv.setItem(19, IconFactory.resonanceModeButton(branch, mode))

        // Row 3: 3 skills (slots 29, 30, 31)
        for (i in 0 until 3) {
            val skill = skills.getOrNull(i) ?: continue
            val canUpgradeThis = canUpgrade && !skill.isMaxed
            inv.setItem(29 + i, IconFactory.skillIcon(skill, i, canUpgradeThis))

            // Auto-cast toggle for active skills (slot below skill)
            if (skill.skillDef.skillType == SkillType.ACTIVE && skill.currentLevel > 0) {
                val autoKey = "${branch.name}_skill_${i + 1}_auto"
                val isAuto = cp.autoCastSkills[autoKey] ?: false
                inv.setItem(38 + i, IconFactory.autoCastToggle(skill, isAuto))
            }
        }

        // Row 4: Eureka slot (slot 40)
        val eurekaName = cp.chosenEurekas[branch]?.eurekaDef?.name
        inv.setItem(40, IconFactory.eurekaSlot(branch, isMaxed, hasEureka, eurekaName))

        // Row 5: Forget branch button (slot 47)
        inv.setItem(47, IconFactory.forgetBranchButton(forgetCost, canAffordForget))

        // Bottom
        inv.setItem(53, IconFactory.closeButton())

        player.openInventory(inv)
    }

    fun handleClick(player: Player, slot: Int, inv: org.bukkit.inventory.Inventory, branch: Branch): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false

        when (slot) {
            8 -> {
                // Back to ClassGUI
                player.closeInventory()
                ClassGUI.open(player, branch.careerClass)
                return true
            }
            53 -> { player.closeInventory(); return true }

            // Skill slots (29, 30, 31)
            29 -> { handleSkillUpgrade(player, branch, 0); return true }
            30 -> { handleSkillUpgrade(player, branch, 1); return true }
            31 -> { handleSkillUpgrade(player, branch, 2); return true }

            // Auto-cast toggles (38, 39, 40 for skills 1,2,3)
            38 -> { handleAutoCastToggle(player, cp, branch, 0); return true }
            39 -> { handleAutoCastToggle(player, cp, branch, 1); return true }

            // Eureka slot (40)
            40 -> {
                handleEurekaClick(player, cp, branch)
                return true
            }

            // Resonance mode button (19)
            19 -> {
                player.closeInventory()
                ResonanceGUI.open(player, branch)
                return true
            }

            // Forget branch button (47)
            47 -> {
                handleForgetBranch(player, cp, branch)
                return true
            }
        }
        return true
    }

    private fun handleSkillUpgrade(player: Player, branch: Branch, index: Int) {
        val cp = CareerManager.getPlayer(player) ?: return
        val skill = cp.getSkill(branch, index)

        if (skill == null) {
            player.sendMessage("§c找不到该技能！")
            return
        }

        if (skill.isMaxed) {
            player.sendMessage("§c该技能已满级！")
            return
        }

        if (!SkillPointManager.hasEnough(cp, 1)) {
            player.sendMessage("§c技能点不足！")
            return
        }

        if (CareerManager.levelUpSkill(player, branch, index)) {
            // Refresh the GUI
            player.closeInventory()
            BranchGUI.open(player, branch)
        }
    }

    private fun handleAutoCastToggle(player: Player, cp: CareerPlayer, branch: Branch, skillIndex: Int) {
        val skill = cp.getSkill(branch, skillIndex) ?: return
        if (skill.skillDef.skillType != SkillType.ACTIVE) return
        if (skill.currentLevel == 0) {
            player.sendMessage("§c技能未解锁，无法设置自动释放！")
            return
        }

        val autoKey = "${branch.name}_skill_${skillIndex + 1}_auto"
        val current = cp.autoCastSkills[autoKey] ?: false
        cp.autoCastSkills[autoKey] = !current

        player.sendMessage(if (!current) {
            "§a✦ 已开启 ${skill.skillDef.name} 的自动释放"
        } else {
            "§7已关闭 ${skill.skillDef.name} 的自动释放"
        })

        // Refresh
        player.closeInventory()
        BranchGUI.open(player, branch)
    }

    private fun handleEurekaClick(player: Player, cp: CareerPlayer, branch: Branch) {
        if (cp.chosenEurekas.containsKey(branch)) {
            player.sendMessage("§c该分支已选择过顿悟，遗忘分支后才可重新选择！")
            return
        }

        if (!cp.isBranchMaxed(branch)) {
            player.sendMessage("§c需要该分支所有技能达到满级（Lv.3）！")
            // Show current skill levels
            val skills = cp.getSkills(branch)
            player.sendMessage("§7当前技能等级：${skills.joinToString(", ") { "Lv.${it.currentLevel}" }}")
            return
        }

        if (!SkillPointManager.hasEnough(cp, 1)) {
            player.sendMessage("§c技能点不足！")
            return
        }

        player.closeInventory()
        EurekaGUI.open(player, branch)
    }

    private fun handleForgetBranch(player: Player, cp: CareerPlayer, branch: Branch) {
        val cost = cp.getForgetCost(branch)

        if (!SkillPointManager.hasEnough(cp, cost)) {
            player.sendMessage("§c遗忘该分支需要 $cost 技能点（当前：${cp.skillPoints}）")
            return
        }

        // Use CareerManager to forget
        if (CareerManager.forgetBranch(player, branch)) {
            player.closeInventory()
            ClassGUI.open(player, branch.careerClass)
        }
    }

    private fun fillBackground(inv: org.bukkit.inventory.Inventory) {
        val filler = IconFactory.fillerPane()
        val emptySlots = listOf(1, 2, 3, 5, 6, 7,
            9, 11, 12, 13, 14, 15, 16, 17,
            18, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 32, 33, 34, 35,
            36, 37, 41, 42, 43, 44,
            45, 46, 48, 49, 50, 51, 52)
        emptySlots.forEach { if (inv.getItem(it) == null) inv.setItem(it, filler) }
    }
}
