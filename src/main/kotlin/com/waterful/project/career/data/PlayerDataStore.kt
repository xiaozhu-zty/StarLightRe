package com.waterful.project.career.data

import com.waterful.project.career.model.AntiFarmData
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.EurekaDef
import com.waterful.project.career.model.EurekaInstance
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.model.SkillDef
import com.waterful.project.career.model.SkillInstance
import com.waterful.project.career.model.SkillType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerDataStore(private val plugin: JavaPlugin) {

    private val dataDir: File
    private val playerCache = ConcurrentHashMap<UUID, CareerPlayer>()

    init {
        dataDir = File(plugin.dataFolder, "playerdata")
    }

    fun init() {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    fun loadPlayer(uuid: UUID, name: String): CareerPlayer {
        // Return cached if available
        playerCache[uuid]?.let { return it }

        val file = getPlayerFile(uuid)
        if (!file.exists()) {
            val newPlayer = CareerPlayer(uuid = uuid, name = name)
            playerCache[uuid] = newPlayer
            return newPlayer
        }

        return try {
            val cfg = YamlConfiguration.loadConfiguration(file)
            deserializePlayer(uuid, cfg)
        } catch (e: Exception) {
            plugin.logger.warning("[StarLightRe] Failed to load player data for $uuid (${e.message}), backing up and creating fresh")
            backupCorruptedFile(file)
            val newPlayer = CareerPlayer(uuid = uuid, name = name)
            playerCache[uuid] = newPlayer
            newPlayer
        }
    }

    fun savePlayer(player: CareerPlayer) {
        try {
            val file = getPlayerFile(player.uuid)
            val cfg = serializePlayer(player)
            cfg.save(file)
        } catch (e: Exception) {
            plugin.logger.severe("[StarLightRe] Failed to save player data for ${player.uuid}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveAll() {
        playerCache.values.forEach { savePlayer(it) }
        plugin.logger.info("[StarLightRe] All player data saved (${playerCache.size} players)")
    }

    fun unloadPlayer(uuid: UUID) {
        playerCache[uuid]?.let { savePlayer(it) }
        playerCache.remove(uuid)
    }

    fun getCachedPlayer(uuid: UUID): CareerPlayer? = playerCache[uuid]

    fun isCached(uuid: UUID): Boolean = playerCache.containsKey(uuid)

    private fun getPlayerFile(uuid: UUID): File =
        File(dataDir, "${uuid}.yml")

    private fun serializePlayer(player: CareerPlayer): YamlConfiguration {
        val cfg = YamlConfiguration()

        cfg.set("uuid", player.uuid.toString())
        cfg.set("name", player.name)
        cfg.set("skill-points", player.skillPoints)
        cfg.set("online-time", player.onlineTimeSeconds)

        // Classes
        cfg.set("classes", player.selectedClasses.map { it.name })

        // Branches
        for ((branch, skills) in player.unlockedBranches) {
            val path = "branches.${branch.name}"
            cfg.set("$path.skills.skill_1", skills.getOrNull(0)?.currentLevel ?: 0)
            cfg.set("$path.skills.skill_2", skills.getOrNull(1)?.currentLevel ?: 0)
            cfg.set("$path.skills.skill_3", skills.getOrNull(2)?.currentLevel ?: 0)
            player.chosenEurekas[branch]?.let {
                cfg.set("$path.eureka", it.eurekaDef.id)
            }
            cfg.set("$path.resonance-mode", player.resonanceModes[branch]?.name ?: ResonanceMode.DISABLED.name)
            // Auto-cast settings
            for ((key, value) in player.autoCastSkills) {
                if (key.startsWith(branch.name)) {
                    cfg.set("$path.auto-cast.$key", value)
                }
            }
        }

        // Anti-farm data
        cfg.set("anti-farm.total-deaths", player.antiFarmData.totalDeaths)
        cfg.set("anti-farm.last-natural-rebirth", player.antiFarmData.lastNaturalRebirthTime)
        cfg.set("anti-farm.suicide-count", player.antiFarmData.suicideCount)
        cfg.set("anti-farm.consecutive-deaths", player.antiFarmData.consecutiveDeaths)
        cfg.set("anti-farm.recent-death-timestamps", player.antiFarmData.oneHourDeathTimestamps)

        // Cooldowns
        for ((key, value) in player.cooldowns) {
            cfg.set("cooldowns.$key", value)
        }

        return cfg
    }

    private fun deserializePlayer(uuid: UUID, cfg: YamlConfiguration): CareerPlayer {
        val name = cfg.getString("name", "Unknown") ?: "Unknown"
        val player = CareerPlayer(
            uuid = uuid,
            name = name,
            skillPoints = cfg.getInt("skill-points", 0),
            onlineTimeSeconds = cfg.getLong("online-time", 0L)
        )

        // Load classes
        val classNames = cfg.getStringList("classes")
        classNames.forEach { className ->
            CareerClass.fromName(className)?.let { player.selectedClasses.add(it) }
        }

        // Load branches
        val branchesSection = cfg.getConfigurationSection("branches")
        branchesSection?.getKeys(false)?.forEach { branchName ->
            Branch.fromName(branchName)?.let { branch ->
                val path = "branches.$branchName"
                val skill1Level = cfg.getInt("$path.skills.skill_1", 0)
                val skill2Level = cfg.getInt("$path.skills.skill_2", 0)
                val skill3Level = cfg.getInt("$path.skills.skill_3", 0)

                val skills = mutableListOf<SkillInstance>()
                for (i in 0..2) {
                    val skillId = SkillDef.makeId(branch, i)
                    val loadedDef = CareerDataLoader.getSkill(skillId)
                    val skillDef = loadedDef ?: SkillDef(
                        id = skillId,
                        name = "${branch.displayName}技能${i + 1}",
                        branch = branch,
                        index = i,
                        skillType = if (i == 0) SkillType.PASSIVE else SkillType.ACTIVE,
                        effectId = skillId
                    )
                    val level = when (i) {
                        0 -> skill1Level
                        1 -> skill2Level
                        else -> skill3Level
                    }
                    skills.add(SkillInstance(skillDef = skillDef, currentLevel = level))
                }
                player.unlockedBranches[branch] = skills

                // Load eureka — use loaded def from career_data.yml if available
                val eurekaId = cfg.getString("$path.eureka")
                if (eurekaId != null) {
                    val loadedEureka = CareerDataLoader.getEureka(eurekaId)
                    val eurekaDef = loadedEureka ?: EurekaDef(
                        id = eurekaId,
                        name = eurekaId,
                        displayName = eurekaId,
                        description = "",
                        branch = branch,
                        effectId = eurekaId,
                        skillType = SkillType.PASSIVE
                    )
                    player.chosenEurekas[branch] = EurekaInstance(eurekaDef = eurekaDef)
                }

                // Load resonance mode
                val modeName = cfg.getString("$path.resonance-mode", "DISABLED") ?: "DISABLED"
                ResonanceMode.fromName(modeName)?.let {
                    player.resonanceModes[branch] = it
                }

                // Load auto-cast settings
                val autoCastSection = cfg.getConfigurationSection("$path.auto-cast")
                autoCastSection?.getKeys(false)?.forEach { key ->
                    player.autoCastSkills[key] = cfg.getBoolean("$path.auto-cast.$key")
                }
            }
        }

        // Load anti-farm data
        player.antiFarmData.totalDeaths = cfg.getInt("anti-farm.total-deaths", 0)
        player.antiFarmData.lastNaturalRebirthTime = cfg.getLong("anti-farm.last-natural-rebirth", 0L)
        player.antiFarmData.suicideCount = cfg.getInt("anti-farm.suicide-count", 0)
        player.antiFarmData.consecutiveDeaths = cfg.getInt("anti-farm.consecutive-deaths", 0)
        val timestamps = cfg.getLongList("anti-farm.recent-death-timestamps")
        player.antiFarmData.oneHourDeathTimestamps.addAll(timestamps)

        // Load cooldowns
        val cooldownSection = cfg.getConfigurationSection("cooldowns")
        cooldownSection?.getKeys(false)?.forEach { key ->
            player.cooldowns[key] = cfg.getLong("cooldowns.$key")
        }

        playerCache[uuid] = player
        return player
    }

    private fun backupCorruptedFile(file: File) {
        val corruptedDir = File(dataDir, "corrupted")
        if (!corruptedDir.exists()) corruptedDir.mkdirs()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val backupFile = File(corruptedDir, "${file.nameWithoutExtension}_$dateStr.yml.bak")
        file.copyTo(backupFile, overwrite = true)
        file.delete()
        plugin.logger.warning("[StarLightRe] Corrupted data backed up to ${backupFile.name}")
    }
}
