package com.waterful.project.career.skill

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillRegistry
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.SkillInstance
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object SkillExecutor {

    /**
     * Execute an active skill for a player
     */
    fun executeSkill(player: Player, skill: SkillInstance): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val now = System.currentTimeMillis()
        val effectId = skill.skillDef.effectId
        val level = skill.currentLevel

        if (level == 0) {
            player.sendMessage(Component.text("该技能尚未解锁！", NamedTextColor.RED))
            return false
        }

        // Check cooldown
        val cooldownSeconds = getCooldownSeconds(level)
        if (cp.isOnCooldown(effectId, cooldownSeconds, now)) {
            val remaining = cp.getRemainingCooldown(effectId, cooldownSeconds, now)
            player.sendMessage(
                Component.text("技能冷却中！剩余 ${remaining} 秒", NamedTextColor.RED)
            )
            return false
        }

        // Get effect from registry
        val effect = SkillRegistry.getSkill(effectId)
        if (effect == null) {
            // Use default message effect as fallback
            player.sendMessage(
                Component.text("✦ 技能触发：${skill.skillDef.name} (Lv.$level)", NamedTextColor.YELLOW)
            )
        } else {
            try {
                effect.execute(player, level)
            } catch (e: Exception) {
                player.sendMessage(Component.text("技能执行出错，请联系管理员。", NamedTextColor.RED))
                e.printStackTrace()
                return false
            }
        }

        // Set cooldown
        cp.setCooldown(effectId, now)

        return true
    }

    /**
     * Execute a passive skill (called when skill is leveled up or on respawn)
     */
    fun executePassiveSkill(player: Player, skill: SkillInstance) {
        val effect = SkillRegistry.getSkill(skill.skillDef.effectId) ?: return
        try {
            effect.execute(player, skill.currentLevel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Apply a passive effect to a target player (used by resonance system)
     */
    fun applyPassiveEffectToTarget(branch: Branch, level: Int, target: Player) {
        val effectId = "${branch.name.lowercase()}_skill_1"  // skill index 1 = passive
        val effect = SkillRegistry.getSkill(effectId) ?: return
        try {
            effect.execute(target, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get cooldown seconds for a skill level
     */
    private fun getCooldownSeconds(level: Int): Int = when (level) {
        1 -> 30
        2 -> 20
        3 -> 10
        else -> 60
    }
}
