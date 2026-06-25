package com.waterful.project.career.command

import com.waterful.project.career.gui.CareerGUI
import com.waterful.project.career.gui.ClassGUI
import com.waterful.project.career.manager.CareerManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClassCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }

        val cp = CareerManager.getPlayer(sender) ?: run {
            sender.sendMessage("§c数据未加载")
            return true
        }

        if (!cp.isCareerSelected) {
            // Open career main panel for class selection
            CareerGUI.open(sender)
            return true
        }

        // If a specific class name is provided, try to open that class panel
        if (args.isNotEmpty()) {
            val className = args.joinToString("_").uppercase()
            val careerClass = com.waterful.project.career.model.CareerClass.fromName(className)
            if (careerClass != null && cp.selectedClasses.contains(careerClass)) {
                ClassGUI.open(sender, careerClass)
                return true
            }
            sender.sendMessage("§c无效的职业名称，或你尚未选择该职业。可用职业：${cp.selectedClasses.joinToString { it.displayName }}")
            return true
        }

        // Show the first selected class
        cp.selectedClasses.firstOrNull()?.let {
            ClassGUI.open(sender, it)
        } ?: CareerGUI.open(sender)
        return true
    }
}
