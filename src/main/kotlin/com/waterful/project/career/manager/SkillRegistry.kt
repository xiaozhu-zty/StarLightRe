package com.waterful.project.career.manager

import com.waterful.project.career.skill.SkillEffect

object SkillRegistry {

    private val skillEffects = mutableMapOf<String, SkillEffect>()
    private val eurekaEffects = mutableMapOf<String, SkillEffect>()

    fun registerSkill(effect: SkillEffect) {
        skillEffects[effect.id] = effect
    }

    fun registerEureka(effect: SkillEffect) {
        eurekaEffects[effect.id] = effect
    }

    fun getSkill(id: String): SkillEffect? = skillEffects[id]

    fun getEureka(id: String): SkillEffect? = eurekaEffects[id]

    fun hasSkill(id: String): Boolean = skillEffects.containsKey(id)

    fun hasEureka(id: String): Boolean = eurekaEffects.containsKey(id)

    fun getSkillCount(): Int = skillEffects.size

    fun getEurekaCount(): Int = eurekaEffects.size

    fun init() {
        // Skill/Eureka effects will be registered here
        // These are placeholder effects that will be populated when the skill system is built
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.Heal())
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.Damage())
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.PotionApply())
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.Buff())
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.Teleport())
        registerSkill(com.waterful.project.career.skill.effects.PlaceholderEffects.Message())
    }
}
