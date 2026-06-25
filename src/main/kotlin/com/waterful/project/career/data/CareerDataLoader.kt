package com.waterful.project.career.data

import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.EurekaDef
import com.waterful.project.career.model.SkillDef
import com.waterful.project.career.model.SkillType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Loads career skill and eureka definitions from career_data.yml.
 */
object CareerDataLoader {

    private val skillDefs: MutableMap<String, SkillDef> = mutableMapOf()
    private val eurekaDefs: MutableMap<String, EurekaDef> = mutableMapOf()

    fun init(plugin: JavaPlugin) {
        skillDefs.clear()
        eurekaDefs.clear()

        val file = File(plugin.dataFolder, "career_data.yml")
        if (!file.exists()) {
            plugin.saveResource("career_data.yml", false)
        }

        val cfg = YamlConfiguration.loadConfiguration(file)
        val careersSection = cfg.getConfigurationSection("careers") ?: run {
            plugin.logger.warning("[StarLightRe] career_data.yml missing 'careers' section")
            return
        }

        for (careerName in careersSection.getKeys(false)) {
            val branchesSection = careersSection.getConfigurationSection("$careerName.branches") ?: continue
            for (branchName in branchesSection.getKeys(false)) {
                val branch = Branch.fromName(branchName) ?: continue
                val branchPath = "$careerName.branches.$branchName"

                // Load skills — use full path from root
                val skillsList = cfg.getMapList("careers.$branchPath.skills")
                for ((index, skillMap) in skillsList.withIndex()) {
                    val id = skillMap["id"] as? String ?: continue
                    val name = skillMap["name"] as? String ?: continue
                    val typeStr = skillMap["type"] as? String ?: "PASSIVE"
                    val skillType = try { SkillType.valueOf(typeStr) } catch (_: Exception) { SkillType.PASSIVE }
                    val descriptions = (skillMap["levels"] as? List<*>)?.map { it.toString() } ?: listOf("", "", "")
                    val cooldowns = (skillMap["cooldown"] as? List<*>)?.map {
                        (it as? Number)?.toInt() ?: 60
                    } ?: listOf(60, 60, 60)

                    skillDefs[id] = SkillDef(
                        id = id,
                        name = name,
                        branch = branch,
                        index = index,
                        skillType = skillType,
                        effectId = id,
                        descriptions = descriptions,
                        cooldownSeconds = cooldowns
                    )
                }

                // Load eurekas — use full path from root
                val eurekaList = cfg.getMapList("careers.$branchPath.eurekas")
                for (eurekaMap in eurekaList) {
                    val id = eurekaMap["id"] as? String ?: continue
                    val name = eurekaMap["name"] as? String ?: continue
                    val description = eurekaMap["description"] as? String ?: ""
                    val cooldown = (eurekaMap["cooldown"] as? Number)?.toInt() ?: 0

                    eurekaDefs[id] = EurekaDef(
                        id = id,
                        name = name,
                        displayName = name,
                        description = description,
                        branch = branch,
                        effectId = id,
                        skillType = SkillType.PASSIVE,
                        cooldownSeconds = cooldown
                    )
                }
            }
        }

        plugin.logger.info("[StarLightRe] CareerDataLoader: loaded ${skillDefs.size} skills, ${eurekaDefs.size} eurekas")
    }

    fun getSkill(id: String): SkillDef? = skillDefs[id]

    fun getEureka(id: String): EurekaDef? = eurekaDefs[id]

    fun getSkillsForBranch(branch: Branch): List<SkillDef> =
        skillDefs.values.filter { it.branch == branch }.sortedBy { it.index }

    fun getEurekasForBranch(branch: Branch): List<EurekaDef> =
        eurekaDefs.values.filter { it.branch == branch }
}
