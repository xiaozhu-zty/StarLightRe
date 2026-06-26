package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.SkillType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * /skillbind <branchName> <skillIndex> [hotkeySlot]
 * Binds an active skill to Shift+Number hotkey.
 * Slots 1-3 are GUI hotkeys; skills use slots 4-9.
 */
class SkillBindCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty() || args[0] == "cancel") {
            sender.sendMessage("§7技能绑定已取消。"); return true
        }
        if (args.size < 2) {
            sender.sendMessage("§c用法：/skillbind <分支名> <技能索引> [热键槽位4-9]"); return true
        }

        val branchName = args[0]
        val skillIndex = args[1].toIntOrNull() ?: run { sender.sendMessage("§c无效的技能索引"); return true }
        val branch = Branch.fromName(branchName) ?: run { sender.sendMessage("§c找不到分支：$branchName"); return true }
        val cp = CareerManager.getPlayer(sender) ?: run { sender.sendMessage("§c数据未加载"); return true }
        val skill = cp.getSkill(branch, skillIndex) ?: run { sender.sendMessage("§c找不到该技能"); return true }

        if (skill.skillDef.skillType != SkillType.ACTIVE) { sender.sendMessage("§c只能绑定主动技能！"); return true }
        if (skill.currentLevel < 1) { sender.sendMessage("§c技能未解锁！"); return true }

        // Find next available slot (4-9) or use specified slot
        val slot = args.getOrNull(2)?.toIntOrNull()
            ?: (4..9).firstOrNull { !cp.autoCastSkills.containsKey("hotkey_$it") }
            ?: run { sender.sendMessage("§c所有热键槽位已满！请先取消一些绑定"); return true }

        if (slot !in 1..9) { sender.sendMessage("§c槽位必须在1-9之间"); return true }

        // Store binding
        cp.autoCastSkills["hotkey_$slot"] = true
        cp.cooldowns["bind_${slot}"] = skillIndex.toLong()
        cp.cooldowns["bind_${slot}_branch"] = branch.name.hashCode().toLong()

        sender.sendMessage("§a✦ 技能 §e${skill.skillDef.name} §a已绑定至 Shift+$slot")
        sender.sendMessage("§7下蹲时按数字键 $slot 即可释放")
        sender.sendMessage("§7默认热键：Shift+1 生涯 | Shift+2 绑定 | Shift+3 重置")
        return true
    }
}
