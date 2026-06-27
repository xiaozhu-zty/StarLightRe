package com.waterful.project.career.manager

import com.waterful.project.career.data.ConfigManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.model.SkillInstance
import com.waterful.project.career.model.SkillType
import com.waterful.project.career.skill.SkillExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object ResonanceManager {

    private var config: ConfigManager? = null
    private var plugin: JavaPlugin? = null

    fun init(configManager: ConfigManager, javaPlugin: JavaPlugin) {
        this.config = configManager
        this.plugin = javaPlugin
    }

    /**
     * Main tick handler — called every N ticks to distribute resonance effects
     */
    fun tick() {
        val server = plugin?.server ?: return
        val onlinePlayers = server.onlinePlayers.toList()

        for (source in onlinePlayers) {
            val sourceCP = CareerManager.getPlayer(source) ?: continue
            val branch = sourceCP.resonantBranch ?: continue
            val mode = sourceCP.getResonanceMode(branch)
            if (mode == ResonanceMode.DISABLED) continue

            val passiveSkill = sourceCP.getSkill(branch, Branch.getPassiveSkillIndex())
            val passiveLevel = passiveSkill?.currentLevel ?: 0
            if (passiveLevel == 0 && mode != ResonanceMode.SOLO_ECHO) continue

            when (mode) {
                ResonanceMode.SOLO_ECHO -> applySelfResonance(source, branch, passiveLevel, doubleEffect = true)
                else -> {
                    val range = sourceCP.getResonanceRange(branch)
                    if (range <= 0) continue
                    val targets = getEffectivePlayers(source, sourceCP, branch, mode, range)
                    for (target in targets) {
                        applyResonanceToTarget(sourceCP, branch, target, passiveLevel)
                    }
                }
            }
        }
    }

    /**
     * Get players within range that should receive resonance from the source
     */
    fun getEffectivePlayers(
        source: Player,
        sourceCP: CareerPlayer,
        branch: Branch,
        mode: ResonanceMode,
        range: Double
    ): List<Player> {
        return source.location.getNearbyPlayers(range).filter { target ->
            if (target.uniqueId == source.uniqueId) return@filter false

            when (mode) {
                ResonanceMode.GLOBAL -> true
                ResonanceMode.FRIENDLY -> {
                    // Same career or no hostile relation
                    val targetCP = CareerManager.getPlayer(target)
                    targetCP?.selectedClasses?.any { sourceCP.selectedClasses.contains(it) } ?: false
                }
                ResonanceMode.INTERNAL -> {
                    // Same branch unlocked
                    val targetCP = CareerManager.getPlayer(target)
                    targetCP?.let { CareerManager.hasBranch(it, branch) } ?: false
                }
                else -> false
            }
        }.toList()
    }

    /**
     * Apply resonance effects from a source player's branch to a target
     */
    fun applyResonanceToTarget(
        sourceCP: CareerPlayer,
        branch: Branch,
        target: Player,
        passiveLevel: Int
    ) {
        // Apply base branch effect
        applyBranchBaseEffect(branch, target)

        // Apply passive skill effects based on level
        if (passiveLevel >= 2) {
            // At level 2+, passive skill level 1 effects
            val passiveSkill = sourceCP.getSkill(branch, Branch.getPassiveSkillIndex())
            if (passiveSkill != null && passiveSkill.currentLevel >= 1) {
                SkillExecutor.applyPassiveEffectToTarget(branch, 1, target)
            }
        }
        if (passiveLevel >= 3) {
            // At level 3, passive skill level 2 effects
            SkillExecutor.applyPassiveEffectToTarget(branch, 2, target)
        }
    }

    /**
     * Apply self-resonance (Solo Echo mode — doubles own effects)
     */
    fun applySelfResonance(source: Player, branch: Branch, passiveLevel: Int, doubleEffect: Boolean) {
        // Apply base effect (doubled in solo echo)
        applyBranchBaseEffect(branch, source)
        if (doubleEffect) {
            applyBranchBaseEffect(branch, source) // Stack once for "doubled"
        }
    }

    /**
     * Apply a branch's base potion effect to a player
     */
    private fun applyBranchBaseEffect(branch: Branch, target: Player) {
        val effectType = getResonancePotionType(branch) ?: return
        val existing = target.getPotionEffect(effectType)
        val currentAmplifier = existing?.amplifier ?: -1
        // Only apply if we can provide a higher amplifier
        if (currentAmplifier < 0) {
            target.addPotionEffect(PotionEffect(effectType, 20 * 5, 0, true, false, true))
        }
    }

    /**
     * Map branch to its resonance potion effect type
     */
    private fun getResonancePotionType(branch: Branch): PotionEffectType? {
        return when (branch.resonanceEffect) {
            "HASTE" -> PotionEffectType.HASTE
            "SPEED" -> PotionEffectType.SPEED
            "RESISTANCE" -> PotionEffectType.RESISTANCE
            "STRENGTH" -> PotionEffectType.STRENGTH
            "REGENERATION" -> PotionEffectType.REGENERATION
            "LUCK" -> PotionEffectType.LUCK
            "NIGHT_VISION" -> PotionEffectType.NIGHT_VISION
            "JUMP_BOOST" -> PotionEffectType.JUMP_BOOST
            "FIRE_RESISTANCE" -> PotionEffectType.FIRE_RESISTANCE
            "WATER_BREATHING" -> PotionEffectType.WATER_BREATHING
            else -> null
        }
    }

    /**
     * Remove resonance effects applied to a target (called when source leaves)
     */
    fun removeResonanceFromTarget(target: Player, branch: Branch) {
        val effectType = getResonancePotionType(branch) ?: return
        // Only remove if the effect was applied by resonance
        // (For simplicity, we remove the lowest amplifier that matches; in production you'd track sources)
        target.removePotionEffect(effectType)
    }

    /**
     * Force refresh resonance for a player (on join/move)
     */
    fun refreshPlayer(target: Player) {
        val server = plugin?.server ?: return
        val targetCP = CareerManager.getPlayer(target) ?: return
        val targetLoc = target.location

        for (source in server.onlinePlayers) {
            if (source.uniqueId == target.uniqueId) continue
            val sourceCP = CareerManager.getPlayer(source) ?: continue

            for (branch in sourceCP.unlockedBranches.keys) {
                val mode = sourceCP.getResonanceMode(branch)
                if (mode == ResonanceMode.DISABLED || mode == ResonanceMode.SOLO_ECHO) continue

                val range = sourceCP.getResonanceRange(branch)
                if (range <= 0) continue

                if (targetLoc.distanceSquared(source.location) <= range * range) {
                    val applies = when (mode) {
                        ResonanceMode.GLOBAL -> true
                        ResonanceMode.FRIENDLY -> targetCP.selectedClasses.any { sourceCP.selectedClasses.contains(it) }
                        ResonanceMode.INTERNAL -> CareerManager.hasBranch(targetCP, branch)
                        else -> false
                    }
                    if (applies) {
                        val passiveLevel = sourceCP.getResonanceLevel(branch)
                        applyResonanceToTarget(sourceCP, branch, target, passiveLevel)
                    }
                }
            }
        }
    }

    /**
     * Remove all resonance effects from a player (on quit)
     */
    fun removeAllResonanceFromPlayer(player: Player) {
        // Remove all common resonance potion effects
        val resonanceEffects = listOf(
            PotionEffectType.HASTE, PotionEffectType.NIGHT_VISION, PotionEffectType.SPEED,
            PotionEffectType.JUMP_BOOST, PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.REGENERATION, PotionEffectType.STRENGTH, PotionEffectType.LUCK,
            PotionEffectType.WATER_BREATHING
        )
        resonanceEffects.forEach { player.removePotionEffect(it) }
    }
}
