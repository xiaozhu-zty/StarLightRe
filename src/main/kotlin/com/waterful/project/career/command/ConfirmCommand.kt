package com.waterful.project.career.command

import com.waterful.project.career.manager.ConfirmManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ConfirmCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.firstOrNull() == "yes") ConfirmManager.confirm(sender)
        else ConfirmManager.cancel(sender)
        return true
    }
}
