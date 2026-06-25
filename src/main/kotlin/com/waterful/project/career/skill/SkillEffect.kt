package com.waterful.project.career.skill

import org.bukkit.entity.Player

interface SkillEffect {
    val id: String
    val displayName: String

    /**
     * Execute the skill/eureka effect
     * @param player The player executing/affected by the skill
     * @param level The skill level (1-3) for skills, 1 for eureka
     */
    fun execute(player: Player, level: Int)

    /** Get a description of what the skill does at the given level */
    fun getDescription(level: Int): String
}
