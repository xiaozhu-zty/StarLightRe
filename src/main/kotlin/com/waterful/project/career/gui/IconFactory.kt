package com.waterful.project.career.gui

import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.EurekaDef
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.model.SkillInstance
import com.waterful.project.career.model.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object IconFactory {

    /** Maximum characters per lore line for readability */
    private const val LORE_MAX_CHARS = 28

    /** Wrap long text into multiple Component lines with the given color */
    fun wrapLore(text: String, color: NamedTextColor = NamedTextColor.GRAY): List<Component> {
        if (text.length <= LORE_MAX_CHARS) return listOf(Component.text(text, color))
        val lines = mutableListOf<Component>()
        var remaining = text
        while (remaining.length > LORE_MAX_CHARS) {
            // Try to break at punctuation or space
            val breakAt = (LORE_MAX_CHARS downTo LORE_MAX_CHARS / 2).firstOrNull {
                remaining.getOrNull(it) in setOf('；', '，', '。', '、', ' ', '·', '）', ')', '】')
            } ?: LORE_MAX_CHARS
            lines.add(Component.text(remaining.substring(0, breakAt), color))
            remaining = remaining.substring(breakAt).trimStart()
        }
        if (remaining.isNotEmpty()) {
            lines.add(Component.text(remaining, color))
        }
        return lines
    }

    /** Truncate display name to fit in inventory title bar */
    fun truncateName(name: String, maxLen: Int = 16): String =
        if (name.length <= maxLen) name else name.take(maxLen - 1) + "…"

    // ===== Glass Panes (Filler) =====

    fun fillerPane(): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        item.editMeta { it.displayName(Component.empty()) }
        return item
    }

    fun closeButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.editMeta {
            it.displayName(Component.text("关闭", NamedTextColor.RED))
        }
        return item
    }

    fun backButton(): ItemStack {
        val item = ItemStack(Material.ARROW)
        item.editMeta {
            it.displayName(Component.text("返回", NamedTextColor.GRAY))
        }
        return item
    }

    // ===== Career GUI Icons =====

    fun careerClassIcon(careerClass: CareerClass, isSelected: Boolean, isLocked: Boolean): ItemStack {
        val item = when {
            isLocked -> ItemStack(Material.BARRIER)
            isSelected -> ItemStack(Material.LIME_STAINED_GLASS_PANE)
            else -> ItemStack(careerClass.material)
        }
        item.editMeta {
            if (isLocked) {
                it.displayName(Component.text("???", NamedTextColor.GRAY))
            } else if (isSelected) {
                it.displayName(Component.text(careerClass.displayName, NamedTextColor.GREEN))
                val desc = mutableListOf<Component>(
                    Component.text("已选择", NamedTextColor.GRAY, TextDecoration.ITALIC)
                )
                desc.addAll(wrapLore(careerClass.description, NamedTextColor.DARK_GREEN))
                it.lore(desc)
            } else {
                it.displayName(Component.text(careerClass.displayName, NamedTextColor.YELLOW))
                val desc = mutableListOf<Component>(
                    Component.text("点击选择", NamedTextColor.GOLD)
                )
                desc.addAll(wrapLore(careerClass.description, NamedTextColor.GRAY))
                it.lore(desc)
            }
        }
        return item
    }

    fun playerHead(player: Player, skillPoints: Int): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta { meta ->
            if (meta is SkullMeta) {
                meta.playerProfile = player.playerProfile
                meta.displayName(Component.text("生涯面板", NamedTextColor.GOLD))
                meta.lore(listOf(
                    Component.text("玩家：${player.name}", NamedTextColor.WHITE),
                    Component.text("技能点：$skillPoints", NamedTextColor.AQUA),
                    Component.text("", NamedTextColor.WHITE),
                    Component.text("Shift+F 打开此面板", NamedTextColor.GRAY, TextDecoration.ITALIC)
                ))
            }
        }
        return item
    }

    fun skillPointDisplay(points: Int): ItemStack {
        val item = ItemStack(Material.EXPERIENCE_BOTTLE)
        item.editMeta {
            it.displayName(Component.text("技能点", NamedTextColor.AQUA))
            it.lore(listOf(
                Component.text("当前技能点：$points", NamedTextColor.WHITE),
                Component.text("通过进度系统获取", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    fun selectRemainingClassHint(remaining: Int): ItemStack {
        val item = ItemStack(Material.PAPER)
        item.editMeta {
            it.displayName(Component.text("选择职业", NamedTextColor.YELLOW))
            it.lore(listOf(
                Component.text("请从下方选择 1 个职业", NamedTextColor.GOLD),
                Component.text("还需要选择：${remaining}个", NamedTextColor.RED)
            ))
        }
        return item
    }

    // ===== Class GUI Icons =====

    fun classHeaderIcon(careerClass: CareerClass): ItemStack {
        val item = ItemStack(careerClass.material)
        item.editMeta {
            it.displayName(Component.text(careerClass.displayName, NamedTextColor.GOLD))
            val desc = mutableListOf<Component>()
            desc.addAll(wrapLore(careerClass.description, NamedTextColor.GRAY))
            desc.add(Component.text("下设4个分支", NamedTextColor.DARK_GREEN))
            it.lore(desc)
        }
        return item
    }

    fun branchIcon(branch: Branch, level: Int, isUnlocked: Boolean, canUnlock: Boolean): ItemStack {
        val item = when {
            isUnlocked -> ItemStack(branch.careerClass.material)
            else -> ItemStack(Material.GRAY_DYE)
        }
        item.editMeta {
            if (isUnlocked) {
                it.displayName(Component.text(branch.displayName, NamedTextColor.GREEN))
                val loreList = mutableListOf<Component>()
                loreList.add(Component.text("分支等级：Lv.$level / 10", NamedTextColor.AQUA))
                loreList.add(Component.text("基础效果：", NamedTextColor.GOLD))
                loreList.addAll(wrapLore(branch.baseEffect, NamedTextColor.GRAY))
                loreList.add(Component.text(""))
                loreList.add(Component.text("点击查看详情", NamedTextColor.GRAY, TextDecoration.ITALIC))
                it.lore(loreList)
            } else {
                it.displayName(Component.text(branch.displayName, NamedTextColor.RED))
                val loreList = mutableListOf<Component>()
                loreList.add(Component.text("未解锁", NamedTextColor.RED))
                loreList.add(Component.text("基础效果：", NamedTextColor.GOLD))
                loreList.addAll(wrapLore(branch.baseEffect, NamedTextColor.GRAY))
                if (canUnlock) {
                    loreList.add(Component.text(""))
                    loreList.add(Component.text("点击解锁（消耗1技能点）", NamedTextColor.GREEN))
                } else {
                    loreList.add(Component.text(""))
                    loreList.add(Component.text("无法解锁（技能点不足或已达上限）", NamedTextColor.DARK_RED))
                }
                it.lore(loreList)
            }
        }
        return item
    }

    // ===== Branch GUI Icons =====

    fun branchHeaderIcon(branch: Branch, level: Int): ItemStack {
        val item = ItemStack(branch.careerClass.material)
        item.editMeta {
            it.displayName(Component.text("${branch.displayName} 分支", NamedTextColor.GOLD))
            it.lore(listOf(
                Component.text("等级：Lv.$level / 10", NamedTextColor.AQUA),
                Component.text("所属职业：${branch.careerClass.displayName}", NamedTextColor.GRAY)
            ))
        }
        return item
    }

    fun baseEffectDisplay(branch: Branch): ItemStack {
        val item = ItemStack(Material.POTION)
        item.editMeta {
            it.displayName(Component.text("基础效果", NamedTextColor.GOLD))
            it.lore(wrapLore(branch.baseEffect, NamedTextColor.GREEN))
        }
        return item
    }

    fun skillIcon(skill: SkillInstance, index: Int, canUpgrade: Boolean): ItemStack {
        val prefix = if (skill.skillDef.skillType == SkillType.PASSIVE) "被动" else "主动"
        val keyName = when (index) {
            0 -> ""
            1 -> "Shift+G"
            2 -> "Shift+H"
            else -> ""
        }

        val item = if (skill.skillDef.skillType == SkillType.PASSIVE) {
            ItemStack(Material.BOOK)
        } else {
            ItemStack(Material.BLAZE_ROD)
        }

        item.editMeta {
            it.displayName(Component.text(
                "${skill.skillDef.name} [$prefix]",
                if (skill.skillDef.skillType == SkillType.PASSIVE) NamedTextColor.AQUA else NamedTextColor.GOLD
            ))

            val loreList = mutableListOf<Component>()
            loreList.add(Component.text("等级：Lv.${skill.currentLevel} / 3", NamedTextColor.YELLOW))
            if (skill.skillDef.skillType == SkillType.ACTIVE) {
                loreList.add(Component.text("快捷键：$keyName", NamedTextColor.LIGHT_PURPLE))
                val cd = skill.skillDef.getCooldown(skill.currentLevel)
                loreList.add(Component.text("冷却：${cd}秒", NamedTextColor.GRAY))
            } else {
                loreList.add(Component.text("被动 · 持续生效", NamedTextColor.GREEN))
            }

            // Show current level effect description
            val desc = skill.currentDescription()
            if (desc.isNotBlank()) {
                loreList.add(Component.text(""))
                loreList.add(Component.text("Lv.${skill.currentLevel} 效果：", NamedTextColor.GOLD))
                loreList.addAll(wrapLore(desc, NamedTextColor.GRAY))
            }

            // Preview next level if not maxed
            if (!skill.isMaxed && skill.currentLevel > 0) {
                val nextDesc = skill.skillDef.getDescription(skill.currentLevel + 1)
                if (nextDesc.isNotBlank()) {
                    loreList.add(Component.text(""))
                    loreList.add(Component.text("→ Lv.${skill.currentLevel + 1}：", NamedTextColor.DARK_GREEN))
                    loreList.addAll(wrapLore(nextDesc, NamedTextColor.DARK_GRAY))
                }
            }

            loreList.add(Component.text(""))
            if (!skill.isMaxed && canUpgrade) {
                loreList.add(Component.text("点击升级（消耗1技能点）", NamedTextColor.GREEN))
            } else if (skill.isMaxed) {
                loreList.add(Component.text("✦ 已满级", NamedTextColor.GOLD))
            } else if (!canUpgrade) {
                loreList.add(Component.text("技能点不足", NamedTextColor.RED))
            }

            it.lore(loreList)
        }
        return item
    }

    fun eurekaSlot(branch: Branch, isMaxed: Boolean, isChosen: Boolean, eurekaName: String?): ItemStack {
        val item = when {
            isChosen -> ItemStack(Material.NETHER_STAR)
            isMaxed -> ItemStack(Material.ENDER_EYE)
            else -> ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        }
        item.editMeta {
            if (isChosen) {
                it.displayName(Component.text("顿悟：$eurekaName", NamedTextColor.LIGHT_PURPLE))
                it.lore(listOf(
                    Component.text("已解锁", NamedTextColor.GREEN)
                ))
            } else if (isMaxed) {
                it.displayName(Component.text("选择顿悟", NamedTextColor.LIGHT_PURPLE))
                it.lore(listOf(
                    Component.text("所有技能已满级！", NamedTextColor.GREEN),
                    Component.text("点击从3个顿悟中选择1个", NamedTextColor.GOLD),
                    Component.text("消耗：1技能点", NamedTextColor.GRAY)
                ))
            } else {
                it.displayName(Component.text("顿悟（锁定）", NamedTextColor.DARK_GRAY))
                it.lore(listOf(
                    Component.text("需要所有技能满级后才能解锁", NamedTextColor.RED)
                ))
            }
        }
        return item
    }

    fun resonanceModeButton(branch: Branch, currentMode: ResonanceMode): ItemStack {
        val item = ItemStack(Material.BELL)
        item.editMeta {
            it.displayName(Component.text("共鸣模式", NamedTextColor.LIGHT_PURPLE))
            it.lore(listOf(
                Component.text("当前：${currentMode.displayName}", NamedTextColor.GOLD),
                Component.text(currentMode.description, NamedTextColor.GRAY),
                Component.text(""),
                Component.text("点击切换共鸣模式", NamedTextColor.GRAY, TextDecoration.ITALIC)
            ))
        }
        return item
    }

    fun forgetBranchButton(cost: Int, canAfford: Boolean): ItemStack {
        val item = ItemStack(Material.LAVA_BUCKET)
        item.editMeta {
            it.displayName(Component.text("遗忘分支", NamedTextColor.RED))
            it.lore(listOf(
                Component.text("遗忘需要花费：$cost 技能点", if (canAfford) NamedTextColor.RED else NamedTextColor.DARK_RED),
                Component.text("遗忘后：分支、技能、顿悟全部清零", NamedTextColor.GRAY),
                if (canAfford) Component.text("点击遗忘", NamedTextColor.RED)
                else Component.text("技能点不足，无法遗忘", NamedTextColor.DARK_RED)
            ))
        }
        return item
    }

    fun autoCastToggle(skill: SkillInstance, isEnabled: Boolean): ItemStack {
        val item = if (isEnabled) ItemStack(Material.LIME_DYE) else ItemStack(Material.GRAY_DYE)
        item.editMeta {
            it.displayName(Component.text(
                "自动释放：${skill.skillDef.name}",
                if (isEnabled) NamedTextColor.GREEN else NamedTextColor.GRAY
            ))
            it.lore(listOf(
                Component.text(
                    if (isEnabled) "已开启（冷却结束后自动释放）" else "已关闭（需手动触发）",
                    NamedTextColor.GRAY
                ),
                Component.text("点击切换", NamedTextColor.YELLOW)
            ))
        }
        return item
    }

    // ===== Eureka GUI Icons =====

    fun eurekaOptionIcon(eureka: EurekaDef, isChosen: Boolean, canAfford: Boolean): ItemStack {
        val item = if (isChosen) ItemStack(Material.NETHER_STAR) else ItemStack(Material.ENDER_EYE)
        item.editMeta {
            it.displayName(Component.text(
                truncateName(eureka.name, 20),
                if (isChosen) NamedTextColor.LIGHT_PURPLE else NamedTextColor.DARK_PURPLE
            ))
            val desc = mutableListOf<Component>()
            desc.addAll(wrapLore(eureka.description, NamedTextColor.GRAY))
            desc.add(Component.text(""))
            desc.add(
                if (isChosen) Component.text("已选择", NamedTextColor.GREEN)
                else if (canAfford) Component.text("点击选择（消耗1技能点）", NamedTextColor.GOLD)
                else Component.text("技能点不足", NamedTextColor.RED)
            )
            it.lore(desc)
        }
        return item
    }

    // ===== Resonance Mode GUI Icons =====

    fun resonanceModeIcon(mode: ResonanceMode, isCurrent: Boolean): ItemStack {
        val item = when (mode) {
            ResonanceMode.DISABLED -> ItemStack(Material.BARRIER)
            ResonanceMode.GLOBAL -> ItemStack(Material.GLOWSTONE)
            ResonanceMode.FRIENDLY -> ItemStack(Material.EMERALD)
            ResonanceMode.INTERNAL -> ItemStack(Material.LAPIS_LAZULI)
            ResonanceMode.SOLO_ECHO -> ItemStack(Material.ECHO_SHARD)
        }
        item.editMeta {
            it.displayName(Component.text(
                mode.displayName,
                if (isCurrent) NamedTextColor.GREEN else NamedTextColor.WHITE
            ))
            it.lore(listOf(
                Component.text(mode.description, NamedTextColor.GRAY),
                if (isCurrent) Component.text("当前模式", NamedTextColor.GREEN)
                else Component.text("点击切换至此模式", NamedTextColor.YELLOW)
            ))
        }
        return item
    }

    // ===== Helpers =====

    private fun getSkillCooldown(level: Int): Int = when (level) {
        0 -> 0
        1 -> 30
        2 -> 20
        3 -> 10
        else -> 0
    }

    private fun getSkillManaCost(level: Int): Int = when (level) {
        0 -> 0
        1 -> 1
        2 -> 1
        3 -> 1
        else -> 0
    }
}
