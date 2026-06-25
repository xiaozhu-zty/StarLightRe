package com.waterful.project.career.manager

import com.waterful.project.career.data.PlayerDataStore
import com.waterful.project.career.model.CareerPlayer
import org.bukkit.entity.Player

object AntiFarmManager {

    private var playerDataStore: PlayerDataStore? = null

    fun init(dataStore: PlayerDataStore) {
        this.playerDataStore = dataStore
    }

    fun recordDeath(player: Player, isSuicide: Boolean) {
        val cp = playerDataStore?.getCachedPlayer(player.uniqueId) ?: return
        val now = System.currentTimeMillis()
        cp.antiFarmData.recordDeath(now, isSuicide)

        // On the 6th death within 1 hour, announce penalty
        if (cp.antiFarmData.isSuicidePenaltyActive(now)) {
            val recentDeaths = cp.antiFarmData.oneHourDeathTimestamps.count {
                now - it <= 60 * 60 * 1000
            }
            if (recentDeaths >= 6 && recentDeaths <= 7) {
                player.sendMessage(
                    net.kyori.adventure.text.Component.text(
                        "§c⚠ 自杀惩罚已触发！你在1小时内死亡超过${com.waterful.project.career.model.AntiFarmData.SUICIDE_PENALTY_THRESHOLD}次。" +
                                "接下来的24小时内重生将只分配1个职业。",
                        net.kyori.adventure.text.format.NamedTextColor.RED
                    )
                )
            }
        }
    }

    fun handleRespawn(player: Player) {
        val cp = playerDataStore?.getCachedPlayer(player.uniqueId) ?: return
        val now = System.currentTimeMillis()

        // Check for natural rebirth
        if (cp.antiFarmData.canNaturalRebirth(now)) {
            applyNaturalRebirth(player, cp, now)
            return
        }

        // Normal respawn
        cp.antiFarmData.onRespawn()
    }

    private fun applyNaturalRebirth(player: Player, cp: CareerPlayer, now: Long) {
        cp.antiFarmData.markNaturalRebirth(now)

        // Reset classes for re-selection
        cp.selectedClasses.clear()

        // Award rebirth skill points
        cp.skillPoints += 3

        player.sendMessage(
            net.kyori.adventure.text.Component.text(
                "§a✦ 自然重生奖励！你获得了3个技能点。请重新选择职业。",
                net.kyori.adventure.text.format.NamedTextColor.GREEN
            )
        )
    }

    fun assignClassesOnJoin(player: Player): List<com.waterful.project.career.model.CareerClass> {
        val cp = playerDataStore?.getCachedPlayer(player.uniqueId) ?: return emptyList()
        val now = System.currentTimeMillis()

        // If player already has classes, don't reassign
        if (cp.selectedClasses.isNotEmpty()) return cp.selectedClasses.toList()

        val classCount = cp.antiFarmData.getRandomClassCount(now)
        val available = com.waterful.project.career.model.CareerClass.entries.toMutableList()
        available.shuffle()

        val assigned = available.take(classCount)
        cp.selectedClasses.addAll(assigned)

        // Award initial skill points for first-time join
        if (cp.antiFarmData.lastNaturalRebirthTime == 0L) {
            cp.skillPoints += 3
            cp.antiFarmData.markNaturalRebirth(now)
            player.sendMessage(
                net.kyori.adventure.text.Component.text(
                    "§a✦ 欢迎来到新大陆！初次选择职业，获得3个技能点作为自然重生奖励。",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN
                )
            )
        }

        return assigned
    }

    fun isPenaltyActive(player: Player): Boolean {
        val cp = playerDataStore?.getCachedPlayer(player.uniqueId) ?: return false
        return cp.antiFarmData.isSuicidePenaltyActive(System.currentTimeMillis())
    }

    fun getPenaltyRemainingHours(player: Player): Int {
        val cp = playerDataStore?.getCachedPlayer(player.uniqueId) ?: return 0
        if (!cp.antiFarmData.isSuicidePenaltyActive(System.currentTimeMillis())) return 0

        val now = System.currentTimeMillis()
        val newestDeath = cp.antiFarmData.oneHourDeathTimestamps.maxOrNull() ?: return 0
        val elapsed = now - newestDeath
        val remaining = com.waterful.project.career.model.AntiFarmData.SUICIDE_PENALTY_DURATION_MS - elapsed
        return if (remaining > 0) (remaining / 3600000).toInt() + 1 else 0
    }
}
