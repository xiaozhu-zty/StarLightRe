package com.waterful.project.career.command

import com.waterful.project.career.manager.DebugManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ListeningCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }
        val enable = args.getOrNull(0)?.lowercase() == "true"
        DebugManager.setListening(sender.uniqueId, enable)
        sender.sendMessage(if (enable) "§a✦ 侦听模式已开启 — 所有职业判定将输出结果" else "§7侦听模式已关闭")
        return true
    }
}
