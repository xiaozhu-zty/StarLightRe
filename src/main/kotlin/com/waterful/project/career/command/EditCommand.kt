package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.stamina.StaminaManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * /edit <player> <type> <number> — admin command to set any player variable.
 *
 * Supported types:
 *   stamina / 体力       — stamina (0 ~ 2000)
 *   skillpoints / sp / 技能点 — career skill points
 */
class EditCommand : CommandExecutor, TabCompleter {

    private val types = listOf("stamina", "体力", "skillpoints", "sp", "技能点")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage("§c用法：/edit <玩家名> <类型> <数值>")
            sender.sendMessage("§c类型: stamina/体力, skillpoints/sp/技能点")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) {
            sender.sendMessage("§c玩家 ${args[0]} 不在线或不存在")
            return true
        }

        val type = args[1].lowercase()
        val value = args[2].toDoubleOrNull()
        if (value == null) {
            sender.sendMessage("§c数值无效: ${args[2]}")
            return true
        }

        when (type) {
            "stamina", "体力" -> {
                val stamina = StaminaManager.getStamina(target)
                stamina.set(value)
                sender.sendMessage("§a✦ 已将 §e${target.name} §a的体力设为 §e${"%.1f".format(value)}")
                target.sendMessage("§a☀ 你的体力已被设为 §e${"%.1f".format(value)}")
            }

            "skillpoints", "sp", "技能点" -> {
                val sp = value.toInt()
                val cp = CareerManager.getPlayer(target)
                if (cp == null) {
                    sender.sendMessage("§c玩家 ${target.name} 数据未加载")
                    return true
                }
                cp.skillPoints = sp.coerceAtLeast(0)
                sender.sendMessage("§a✦ 已将 §e${target.name} §a的技能点设为 §e$sp")
                target.sendMessage("§a✦ 你的技能点已被设为 §e$sp")
            }

            else -> {
                sender.sendMessage("§c未知类型: $type")
                sender.sendMessage("§c可用: ${types.joinToString(", ")}")
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers().map { it.name }
            2 -> types.filter { it.startsWith(args[1].lowercase()) }
            3 -> listOf("<数值>")
            else -> emptyList()
        }
    }
}
