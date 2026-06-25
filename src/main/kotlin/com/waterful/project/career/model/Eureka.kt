package com.waterful.project.career.model

data class EurekaDef(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val branch: Branch,
    val effectId: String,
    val skillType: SkillType,
    val cooldownSeconds: Int = 0
)

data class EurekaInstance(
    val eurekaDef: EurekaDef
)
