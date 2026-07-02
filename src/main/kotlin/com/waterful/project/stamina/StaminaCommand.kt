package com.waterful.project.stamina

import com.waterful.project.station.StationManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * /tl   — query current stamina level
 * /tlbl — query stamina consumption magnification
 * /tlhf — query stamina recovery rate (from stations etc.)
 */
object StaminaCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("此指令只能由玩家执行")
            return true
        }
        when (label.lowercase()) {
            "tlbl" -> StaminaManager.showMagnification(sender)
            "tl" -> StaminaManager.showStamina(sender)
            "tlhf" -> {
                val rate = StationManager.getRecoveryRate(sender)
                val resting = if (StaminaManager.isResting(sender)) 0.5 else 0.0
                val total = rate + resting
                sender.sendMessage("§a☀ 当前体力恢复速率: §e${"%.1f".format(total)}/s")
                if (rate > 0) sender.sendMessage("§7  ┗ 信标光环: §b+${"%.1f".format(rate)}/s")
                if (resting > 0) sender.sendMessage("§7  ┗ 原地休息: §b+${"%.1f".format(resting)}/s")
            }
        }
        return true
    }
}
