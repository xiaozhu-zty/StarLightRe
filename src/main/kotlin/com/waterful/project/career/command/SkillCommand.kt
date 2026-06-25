package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.skill.SkillExecutor
import com.waterful.project.career.model.Branch
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SkillCommand(private val skillIndex: Int) : CommandExecutor {
    // skillIndex: 1 = first active skill (Shift+G), 2 = second active skill (Shift+H)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此命令只能由玩家执行")
            return true
        }

        val cp = CareerManager.getPlayer(sender) ?: run {
            sender.sendMessage("§c数据未加载")
            return true
        }

        if (cp.unlockedBranches.isEmpty()) {
            sender.sendMessage("§c你尚未解锁任何分支！")
            return true
        }

        // Find the first unlocked branch that has this active skill with level > 0
        val activeSkillBranch = cp.unlockedBranches.entries.firstOrNull { (branch, skills) ->
            val skill = skills.getOrNull(skillIndex)  // skillIndex maps directly (1=Shift+G, 2=Shift+H)
            skill != null && skill.skillDef.skillType == com.waterful.project.career.model.SkillType.ACTIVE && skill.currentLevel > 0
        }

        if (activeSkillBranch == null) {
            val keyName = if (skillIndex == 1) "Shift+G" else "Shift+H"
            sender.sendMessage("§c你没有可用的主动技能！请先在有主动技能的分支中提升技能等级。")
            sender.sendMessage("§7快捷键：$keyName 或 /skill${skillIndex}")
            return true
        }

        val (branch, _) = activeSkillBranch
        val skill = cp.getSkill(branch, skillIndex)!!
        SkillExecutor.executeSkill(sender, skill)
        return true
    }
}
