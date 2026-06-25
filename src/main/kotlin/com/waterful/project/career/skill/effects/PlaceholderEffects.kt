package com.waterful.project.career.skill.effects

import com.waterful.project.career.skill.SkillEffect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object PlaceholderEffects {

    class Heal : SkillEffect {
        override val id = "heal"
        override val displayName = "治疗"

        override fun execute(player: Player, level: Int) {
            val healAmount = level * 4.0
            val newHealth = minOf(player.health + healAmount, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0)
            player.health = newHealth
            player.sendMessage(
                Component.text("✦ 恢复了 ${healAmount.toInt()} 点生命值", NamedTextColor.GREEN)
            )
        }

        override fun getDescription(level: Int): String = "恢复 ${level * 4} 点生命值"
    }

    class Damage : SkillEffect {
        override val id = "damage"
        override val displayName = "伤害"

        override fun execute(player: Player, level: Int) {
            // Get target from player's line of sight
            val target = player.getTargetEntity(5) as? org.bukkit.entity.LivingEntity
            if (target != null) {
                val damage = level * 3.0
                target.damage(damage, player)
                player.sendMessage(
                    Component.text("✦ 对目标造成了 ${damage.toInt()} 点伤害", NamedTextColor.RED)
                )
            } else {
                player.sendMessage(
                    Component.text("没有找到目标！请看向一个实体。", NamedTextColor.GRAY)
                )
            }
        }

        override fun getDescription(level: Int): String = "对5格内目标造成 ${level * 3} 点伤害"
    }

    class PotionApply : SkillEffect {
        override val id = "potion"
        override val displayName = "药水效果"

        override fun execute(player: Player, level: Int) {
            val effectType = when {
                level >= 3 -> PotionEffectType.STRENGTH
                level == 2 -> PotionEffectType.SPEED
                else -> PotionEffectType.HASTE
            }
            player.addPotionEffect(PotionEffect(effectType, 20 * 30, level - 1, true, false, true))
            player.sendMessage(
                Component.text("✦ 获得 ${effectType.name} 效果（Lv.$level，持续30秒）", NamedTextColor.AQUA)
            )
        }

        override fun getDescription(level: Int): String = "获得持续30秒的药水效果"
    }

    class Buff : SkillEffect {
        override val id = "buff"
        override val displayName = "增益"

        override fun execute(player: Player, level: Int) {
            // Apply absorption and regeneration
            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 20 * (10 + level * 10), level - 1, true, false, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * (5 + level * 5), level - 1, true, false, true))
            player.sendMessage(
                Component.text("✦ 获得吸收和生命恢复效果（Lv.$level）", NamedTextColor.GOLD)
            )
        }

        override fun getDescription(level: Int): String = "获得吸收和生命恢复效果"
    }

    class Teleport : SkillEffect {
        override val id = "teleport"
        override val displayName = "传送"

        override fun execute(player: Player, level: Int) {
            val distance = level * 8.0
            val targetLoc = player.location.add(player.location.direction.multiply(distance))
            // Ensure safe landing
            targetLoc.y = player.world.getHighestBlockYAt(targetLoc) + 1.0
            if (targetLoc.y > player.world.maxHeight) targetLoc.y = player.world.maxHeight.toDouble() - 1
            if (targetLoc.y < player.world.minHeight) targetLoc.y = player.world.minHeight.toDouble() + 1

            player.teleport(targetLoc)
            player.sendMessage(
                Component.text("✦ 传送了 ${distance.toInt()} 格", NamedTextColor.LIGHT_PURPLE)
            )
        }

        override fun getDescription(level: Int): String = "向前方传送 ${level * 8} 格"
    }

    class Message : SkillEffect {
        override val id = "message"
        override val displayName = "消息提示"

        override fun execute(player: Player, level: Int) {
            val messages = when (level) {
                1 -> listOf("技能触发！")
                2 -> listOf("技能强化触发！！", "效果提升！")
                3 -> listOf("技能最高级触发！！！", "效果最大化！", "✦ 满级力量 ✦")
                else -> listOf("技能触发")
            }
            messages.forEach { msg ->
                player.sendMessage(
                    Component.text("✦ $msg", NamedTextColor.YELLOW)
                )
            }
        }

        override fun getDescription(level: Int): String = "展示技能触发的消息提示"
    }
}
