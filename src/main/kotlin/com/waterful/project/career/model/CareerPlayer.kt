package com.waterful.project.career.model

import java.util.UUID

data class CareerPlayer(
    val uuid: UUID,
    var name: String,
    val selectedClasses: MutableList<CareerClass> = mutableListOf(),
    val unlockedBranches: MutableMap<Branch, MutableList<SkillInstance>> = mutableMapOf(),
    val chosenEurekas: MutableMap<Branch, EurekaInstance> = mutableMapOf(),
    val resonanceModes: MutableMap<Branch, ResonanceMode> = mutableMapOf(),
    var skillPoints: Int = 0,
    var onlineTimeSeconds: Long = 0L,
    val antiFarmData: AntiFarmData = AntiFarmData(),
    // Cooldowns: skillEffectId -> lastUsedEpochMs
    val cooldowns: MutableMap<String, Long> = mutableMapOf(),
    // Auto-cast settings for active skills
    val autoCastSkills: MutableMap<String, Boolean> = mutableMapOf()
) {
    /** Get the level of a branch: sum of all skill levels + (1 if eureka chosen) */
    fun getBranchLevel(branch: Branch): Int {
        val skills = unlockedBranches[branch] ?: return 0
        val skillSum = skills.sumOf { it.currentLevel }
        val eurekaBonus = if (chosenEurekas.containsKey(branch)) 1 else 0
        return skillSum + eurekaBonus
    }

    /** Check if all skills in a branch are maxed (level 3) */
    fun isBranchMaxed(branch: Branch): Boolean {
        val skills = unlockedBranches[branch] ?: return false
        return skills.all { it.isMaxed }
    }

    /** Get total number of unlocked branches across all classes */
    fun getTotalUnlockedBranches(): Int = unlockedBranches.size

    /** Check if player can unlock more branches (max 6) */
    fun canUnlockMoreBranches(): Boolean = unlockedBranches.size < 6

    /** Get skill instances for a branch */
    fun getSkills(branch: Branch): List<SkillInstance> =
        unlockedBranches[branch] ?: emptyList()

    /** Get a specific skill instance by index (0=passive, 1=skill2, 2=skill3) */
    fun getSkill(branch: Branch, index: Int): SkillInstance? =
        unlockedBranches[branch]?.getOrNull(index)

    /** Check if a skill is on cooldown */
    fun isOnCooldown(effectId: String, cooldownSeconds: Int, now: Long): Boolean {
        val lastUsed = cooldowns[effectId] ?: return false
        return (now - lastUsed) < cooldownSeconds * 1000L
    }

    /** Get remaining cooldown in seconds */
    fun getRemainingCooldown(effectId: String, cooldownSeconds: Int, now: Long): Int {
        val lastUsed = cooldowns[effectId] ?: return 0
        val elapsed = (now - lastUsed) / 1000L
        val remaining = cooldownSeconds - elapsed
        return if (remaining > 0) remaining.toInt() else 0
    }

    /** Record cooldown usage */
    fun setCooldown(effectId: String, now: Long) {
        cooldowns[effectId] = now
    }

    /** Get forget branch cost: (2 * level + 2) */
    fun getForgetCost(branch: Branch): Int {
        val level = getBranchLevel(branch)
        return 2 * level + 2
    }

    /** Get resonance mode for a branch (defaults to DISABLED) */
    fun getResonanceMode(branch: Branch): ResonanceMode =
        resonanceModes[branch] ?: ResonanceMode.DISABLED

    /** Check if player has the passive skill at a certain level for resonance */
    fun getResonanceLevel(branch: Branch): Int {
        val passiveSkill = getSkill(branch, Branch.getPassiveSkillIndex())
        return passiveSkill?.currentLevel ?: 0
    }

    /** Get resonance range based on passive skill level */
    fun getResonanceRange(branch: Branch): Double {
        val level = getResonanceLevel(branch)
        val mode = getResonanceMode(branch)
        if (mode == ResonanceMode.DISABLED || mode == ResonanceMode.SOLO_ECHO) return 0.0
        val base = level * 8.0
        return when (mode) {
            ResonanceMode.DISABLED -> 0.0
            ResonanceMode.GLOBAL -> base
            ResonanceMode.FRIENDLY -> base * 0.75
            ResonanceMode.INTERNAL -> base * 0.5
            ResonanceMode.SOLO_ECHO -> 0.0
        }
    }

    /** Check if the career selection is complete (3 classes chosen) */
    val isCareerSelected: Boolean get() = selectedClasses.size >= 3

    companion object {
        const val MAX_BRANCHES = 6
        const val MAX_SKILL_LEVEL = 3
        const val INITIAL_CLASSES = 3
        const val RANDOM_CLASSES_NORMAL = 2
        const val RANDOM_CLASSES_PENALTY = 1
    }
}
