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

        // Hotkey bindings: slot(0-8) -> "branchName:skillIndex" or "eureka:branchName"
        for ((slot, binding) in player.hotkeyBinds) {
            cfg.set("hotkeys.$slot", binding)
        }

        // Release mode
        cfg.set("scroll-mode", player.scrollMode)

        // Resonant branch
        player.resonantBranch?.let { cfg.set("resonant-branch", it.name) }

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

        // Load hotkey bindings
        val hotkeysSection = cfg.getConfigurationSection("hotkeys")
        hotkeysSection?.getKeys(false)?.forEach { key ->
            val slot = key.toIntOrNull() ?: return@forEach
            val binding = cfg.getString("hotkeys.$key") ?: return@forEach
            player.hotkeyBinds[slot] = binding
        }

        // Load release mode
        player.scrollMode = cfg.getBoolean("scroll-mode", false)

        // Load resonant branch
        cfg.getString("resonant-branch")?.let {
            player.resonantBranch = Branch.fromName(it)
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

    // ===== Stamina persistence =====

    fun saveStamina(uuid: UUID, data: com.waterful.project.stamina.StaminaData) {
        try {
            val file = getPlayerFile(uuid)
            val cfg = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()
            cfg.set("stamina", data.stamina)
            cfg.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("[StarLightRe] Failed to save stamina for $uuid: ${e.message}")
        }
    }

    fun loadStamina(uuid: UUID): com.waterful.project.stamina.StaminaData {
        return try {
            val file = getPlayerFile(uuid)
            if (!file.exists()) return com.waterful.project.stamina.StaminaData()
            val cfg = YamlConfiguration.loadConfiguration(file)
            if (cfg.contains("stamina")) {
                com.waterful.project.stamina.StaminaData(cfg.getDouble("stamina", 2000.0))
            } else {
                com.waterful.project.stamina.StaminaData()
            }
        } catch (e: Exception) {
            com.waterful.project.stamina.StaminaData()
        }
    }

    // ===== Station persistence =====

    fun saveStation(uuid: UUID, data: com.waterful.project.station.StationData) {
        try {
            val file = getPlayerFile(uuid)
            val cfg = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()
            cfg.set("station.level", data.level)
            cfg.set("station.stamp", data.stamp)
            data.location?.let {
                cfg.set("station.location.world", it.world?.name)
                cfg.set("station.location.x", it.x)
                cfg.set("station.location.y", it.y)
                cfg.set("station.location.z", it.z)
            }
            cfg.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("[StarLightRe] Failed to save station for $uuid: ${e.message}")
        }
    }

    fun loadStation(uuid: UUID): com.waterful.project.station.StationData? {
        return try {
            val file = getPlayerFile(uuid)
            if (!file.exists() || !YamlConfiguration.loadConfiguration(file).contains("station.level")) return null
            val cfg = YamlConfiguration.loadConfiguration(file)
            val level = cfg.getInt("station.level", 1)
            val stamp = cfg.getLong("station.stamp", 0L)
            val worldName = cfg.getString("station.location.world")
            val loc = if (worldName != null) {
                org.bukkit.Location(
                    org.bukkit.Bukkit.getWorld(worldName),
                    cfg.getDouble("station.location.x"),
                    cfg.getDouble("station.location.y"),
                    cfg.getDouble("station.location.z")
                )
            } else null
            com.waterful.project.station.StationData(uuid, level, loc, stamp)
        } catch (e: Exception) {
            null
        }
    }
}
