package com.waterful.project.career.command

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.station.StationManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClearCDCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("此命令只能由玩家执行"); return true }
        val cp = CareerManager.getPlayer(sender) ?: run { sender.sendMessage("§c数据未加载"); return true }
        cp.cooldowns.clear()
        sender.sendMessage("§a✦ 所有技能冷却已清除！")

        // Also reset station placement cooldown
        val station = StationManager.getStationData(sender.uniqueId)
        if (station != null) {
            station.stamp = System.currentTimeMillis() - 100000000L
            sender.sendMessage("§a✦ 驻扎篝火放置冷却已清除！")
        }
        return true
    }
}
