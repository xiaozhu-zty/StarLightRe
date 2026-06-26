package com.waterful.project.career.model

data class SkillDef(
    val id: String,
    val name: String,
    val branch: Branch,
    val index: Int,
    val skillType: SkillType,
    val effectId: String,
    /** Level-specific effect descriptions (index 0 = lvl.1, index 1 = lvl.2, index 2 = lvl.3) */
    val descriptions: List<String> = listOf("", "", ""),
    /** Cooldown in seconds per level */
    val cooldownSeconds: List<Int> = listOf(30, 20, 10)
) {
    fun getDescription(level: Int): String =
        descriptions.getOrElse(level - 1) { "" }

    fun getCooldown(level: Int): Int =
        cooldownSeconds.getOrElse(level - 1) { 60 }

    companion object {
        /** Generate skill ID matching YAML keys (0-based: skill_0, skill_1, skill_2) */
        fun makeId(branch: Branch, index: Int): String =
            "${branch.name.lowercase()}_skill_$index"
    }
}

data class SkillInstance(
    val skillDef: SkillDef,
    var currentLevel: Int = 0
) {
    val isMaxed: Boolean get() = currentLevel >= 3

    /** Get the description for the current level */
    fun currentDescription(): String = skillDef.getDescription(currentLevel)
}
