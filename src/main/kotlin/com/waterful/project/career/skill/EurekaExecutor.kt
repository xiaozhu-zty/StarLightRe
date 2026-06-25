package com.waterful.project.career.skill

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillRegistry
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.EurekaInstance
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object EurekaExecutor {

    /**
     * Execute a eureka for a player
     */
    fun executeEureka(player: Player, eureka: EurekaInstance, branch: Branch): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val now = System.currentTimeMillis()
        val effectId = eureka.eurekaDef.effectId

        // Check cooldown
        val cooldownSeconds = eureka.eurekaDef.cooldownSeconds
        if (cooldownSeconds > 0 && cp.isOnCooldown(effectId, cooldownSeconds, now)) {
            val remaining = cp.getRemainingCooldown(effectId, cooldownSeconds, now)
            player.sendMessage(
                Component.text("顿悟冷却中！剩余 ${remaining} 秒", NamedTextColor.RED)
            )
            return false
        }

        // Get effect from registry
        val effect = SkillRegistry.getEureka(effectId)
        if (effect == null) {
            player.sendMessage(
                Component.text("✦ 顿悟触发：${eureka.eurekaDef.name}", NamedTextColor.LIGHT_PURPLE)
            )
        } else {
            try {
                effect.execute(player, 1)
            } catch (e: Exception) {
                player.sendMessage(Component.text("顿悟执行出错，请联系管理员。", NamedTextColor.RED))
                e.printStackTrace()
                return false
            }
        }

        // Set cooldown
        if (cooldownSeconds > 0) {
            cp.setCooldown(effectId, now)
        }

        return true
    }
}
