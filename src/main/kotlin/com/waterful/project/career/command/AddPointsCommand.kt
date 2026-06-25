package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillPointManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddPointsCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§c用法：/addpoints <数量>")
            return true
        }

        val amount = args[0].toIntOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage("§c请输入一个有效的正整数")
            return true
        }

        val cp = CareerManager.getPlayer(sender)
        if (cp == null) {
            sender.sendMessage("§c数据未加载，请重新登录")
            return true
        }

        SkillPointManager.addPoints(cp, amount)
        sender.sendMessage("§a✦ 已添加 §e${amount} §a技能点（当前：§e${cp.skillPoints}§a）")
        return true
    }
}
