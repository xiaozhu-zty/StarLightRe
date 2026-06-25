package com.waterful.project.career.command

import com.waterful.project.career.gui.CareerGUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CareerCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }
        CareerGUI.open(sender)
        return true
    }
}
