package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class UnlockCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§c此命令仅限OP使用！")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§6/unlock <玩家名> §7— 解除该玩家的6分支3职业限制")
            return true
        }

        val target = org.bukkit.Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("§c玩家 ${args[0]} 不在线")
            return true
        }

        val cp = CareerManager.getPlayer(target)
        if (cp == null) {
            sender.sendMessage("§c玩家数据未加载")
            return true
        }

        cp.unlocked = !cp.unlocked
        val status = if (cp.unlocked) "§a已解除" else "§c已恢复"
        sender.sendMessage("§a✦ ${target.name} 的职业/分支限制：$status")
        target.sendMessage("§5✦ 管理员${if (cp.unlocked) "解除" else "恢复"}了你的职业/分支限制！")
        if (cp.unlocked) {
            target.sendMessage("§7现在你可以解锁超过6个分支和3个职业")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return org.bukkit.Bukkit.getOnlinePlayers().map { it.name }
        }
        return emptyList()
    }
}
