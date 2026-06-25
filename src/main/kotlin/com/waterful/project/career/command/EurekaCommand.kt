package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.skill.EurekaExecutor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EurekaCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }

        val cp = CareerManager.getPlayer(sender) ?: run {
            sender.sendMessage("§c数据未加载")
            return true
        }

        // Find the first unlocked branch with a chosen eureka
        val eurekaBranch = cp.chosenEurekas.entries.firstOrNull()

        if (eurekaBranch == null) {
            sender.sendMessage("§c你尚未解锁任何顿悟！")
            sender.sendMessage("§7需要先满级一个分支的所有技能，然后在分支面板中选择顿悟。")
            return true
        }

        val (branch, eurekaInstance) = eurekaBranch
        EurekaExecutor.executeEureka(sender, eurekaInstance, branch)
        return true
    }
}
