package com.waterful.project.career.command

import com.waterful.project.career.gui.ResonanceGUI
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.ResonanceMode
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ResonanceCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }

        val cp = CareerManager.getPlayer(sender) ?: run {
            sender.sendMessage("§c数据未加载")
            return true
        }

        if (args.isEmpty()) {
            // Open resonance GUI for the first resonance-capable branch
            val capableBranch = cp.unlockedBranches.keys.firstOrNull {
                cp.getResonanceLevel(it) > 0
            }

            if (capableBranch != null) {
                ResonanceGUI.open(sender, capableBranch)
            } else {
                sender.sendMessage("§c你还没有可共鸣的分支！请先在有被动技能的分支中提升等级。")
                sender.sendMessage("§7使用 /career 打开生涯面板，进入分支面板设置共鸣。")
            }
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§7用法: /resonance <分支名> <模式>")
            sender.sendMessage("§7模式: disabled, global, friendly, internal, solo_echo")
            return true
        }

        val branch = Branch.fromName(args[0].uppercase())
        if (branch == null) {
            sender.sendMessage("§c无效的分支名称！可用的分支：${cp.unlockedBranches.keys.joinToString { it.displayName }}")
            return true
        }

        val mode = ResonanceMode.fromName(args[1].uppercase())
        if (mode == null) {
            sender.sendMessage("§c无效的共鸣模式！可用模式: disabled, global, friendly, internal, solo_echo")
            return true
        }

        CareerManager.setResonanceMode(sender, branch, mode)
        return true
    }
}
