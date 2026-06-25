package com.waterful.project.career.data

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

class ConfigManager(private val plugin: JavaPlugin) {

    private var config: YamlConfiguration = YamlConfiguration()

    // Career settings
    var initialSkillPoints: Int = 3
        private set
    var rebirthBonusSkillPoints: Int = 3
        private set

    // Resonance settings
    var resonanceRangePerLevel: Double = 8.0
        private set
    var resonanceFriendlyMultiplier: Double = 0.75
        private set
    var resonanceInternalMultiplier: Double = 0.5
        private set
    var resonanceTickInterval: Long = 20L
        private set

    // Anti-farm settings
    var rebirthCooldownDays: Int = 30
        private set
    var maxDeathsBeforePenalty: Int = 6
        private set
    var suicidePenaltyDurationHours: Int = 24
        private set

    // Branch settings
    var maxUnlockedBranches: Int = 6
        private set
    var forgetBaseCost: Int = 2
        private set

    // Skill settings
    var maxSkillLevel: Int = 3
        private set
    var skillLevelUpCost: Int = 1
        private set

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        val cfg = plugin.config

        initialSkillPoints = cfg.getInt("career.skill-points.initial", 3)
        rebirthBonusSkillPoints = cfg.getInt("career.skill-points.rebirth-bonus", 3)

        resonanceRangePerLevel = cfg.getDouble("career.resonance.range-per-level", 8.0)
        resonanceFriendlyMultiplier = cfg.getDouble("career.resonance.friendly-multiplier", 0.75)
        resonanceInternalMultiplier = cfg.getDouble("career.resonance.internal-multiplier", 0.5)
        resonanceTickInterval = cfg.getLong("career.resonance.tick-interval", 20L)

        rebirthCooldownDays = cfg.getInt("career.anti-farm.rebirth-cooldown-days", 30)
        maxDeathsBeforePenalty = cfg.getInt("career.anti-farm.max-deaths-before-penalty", 6)
        suicidePenaltyDurationHours = cfg.getInt("career.anti-farm.suicide-penalty-duration-hours", 24)

        maxUnlockedBranches = cfg.getInt("career.branch.max-unlocked", 6)
        forgetBaseCost = cfg.getInt("career.branch.forget-base-cost", 2)

        maxSkillLevel = cfg.getInt("career.skill.max-level", 3)
        skillLevelUpCost = cfg.getInt("career.skill.level-up-cost", 1)

        plugin.logger.info("[StarLightRe] ConfigManager loaded successfully")
    }

    fun getForgetCost(branchLevel: Int): Int = forgetBaseCost * branchLevel + 2
}
