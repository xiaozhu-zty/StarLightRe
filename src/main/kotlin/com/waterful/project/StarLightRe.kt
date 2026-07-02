package com.waterful.project

import com.waterful.project.career.data.CareerDataLoader
import com.waterful.project.career.data.ConfigManager
import com.waterful.project.career.data.PlayerDataStore
import com.waterful.project.career.listener.AntiFarmListener
import com.waterful.project.career.listener.CareerGateListener
import com.waterful.project.career.listener.TeacherListener
import com.waterful.project.career.listener.CareerGUIListener
import com.waterful.project.career.listener.PassiveSkillListener
import com.waterful.project.career.listener.ResonanceListener
import com.waterful.project.career.manager.AntiFarmManager
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.ResonanceManager
import com.waterful.project.career.manager.RecipeManager
import com.waterful.project.career.manager.SkillPointManager
import com.waterful.project.career.manager.SkillRegistry
import com.waterful.project.career.command.CareerCommand
import com.waterful.project.career.command.ClassCommand
import com.waterful.project.career.command.SkillCommand
import com.waterful.project.career.command.EurekaCommand
import com.waterful.project.career.command.AddPointsCommand
import com.waterful.project.career.command.ClearCDCommand
import com.waterful.project.career.command.ConfirmCommand
import com.waterful.project.career.command.DelCareerCommand
import com.waterful.project.career.command.ListeningCommand
import com.waterful.project.career.command.ResonanceCommand
import com.waterful.project.career.command.UnlockCommand
import com.waterful.project.listeners.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

class StarLightRe : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var playerDataStore: PlayerDataStore

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()

        // Initialize config
        configManager = ConfigManager(this)
        configManager.load()

        // Initialize data store
        playerDataStore = PlayerDataStore(this)
        playerDataStore.init()

        // Initialize managers (order matters)
        SkillPointManager
        AntiFarmManager.init(playerDataStore)
        CareerManager.init(playerDataStore, configManager)
        CareerDataLoader.init(this)
        RecipeManager.init(this)
        SkillRegistry.init()
        com.waterful.project.career.skill.QTEProvider.init(this)
        ResonanceManager.init(configManager, this)

        // Register commands
        getCommand("career")?.setExecutor(CareerCommand())
        getCommand("shengya")?.setExecutor(CareerCommand())
        getCommand("class")?.setExecutor(ClassCommand())
        getCommand("skill1")?.setExecutor(SkillCommand(1))
        getCommand("skill2")?.setExecutor(SkillCommand(2))
        getCommand("eureka")?.setExecutor(EurekaCommand())
        getCommand("resonance")?.setExecutor(ResonanceCommand())
        getCommand("addpoints")?.setExecutor(AddPointsCommand())
        getCommand("delcareer")?.setExecutor(DelCareerCommand())
        getCommand("listening")?.setExecutor(ListeningCommand())
        getCommand("confirm")?.setExecutor(ConfirmCommand())
        getCommand("clearcd")?.setExecutor(ClearCDCommand())
        val unlockCmd = UnlockCommand()
        getCommand("unlock")?.setExecutor(unlockCmd)
        getCommand("unlock")?.tabCompleter = unlockCmd

        // Register listeners
        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(CareerGUIListener(), this)
        server.pluginManager.registerEvents(ResonanceListener(), this)
        server.pluginManager.registerEvents(AntiFarmListener(), this)
        server.pluginManager.registerEvents(PassiveSkillListener(), this)
        server.pluginManager.registerEvents(CareerGateListener(), this)
        server.pluginManager.registerEvents(TeacherListener(this), this)

        // Start resonance tick task
        startResonanceTask()

        // Start fisherman passive effects tick (Luck + Conduit Power)
        startFishermanTick()

        // Start kuangdeng (dynamic light) follow tick
        startKuangDengTick()

        // Start fuel pipeline tick (keeps nearby furnaces burning without fuel)
        startRanLiaoGuanDaoTick()

        // Start investment vision expiry tick
        startInvestmentTick()

        // Start auto-release tick (fires auto-cast skills when cooldowns are ready)
        startAutoReleaseTick()

        logger.info("✦ ${description.name} v${description.version} 已启用！")
        logger.info("✦ 生涯系统已加载：6职业 | 24分支 | 72技能 | 72顿悟")
    }

    override fun onDisable() {
        RecipeManager.unregister()
        playerDataStore.saveAll()
        logger.info("${description.name} 已禁用，所有玩家数据已保存。")
    }

    private fun startResonanceTask() {
        val tickInterval = configManager.resonanceTickInterval
        server.scheduler.runTaskTimer(this, Runnable {
            ResonanceManager.tick()
        }, tickInterval, tickInterval)
    }

    private fun startFishermanTick() {
        server.scheduler.runTaskTimer(this, Runnable {
            for (player in server.onlinePlayers) {
                val cp = com.waterful.project.career.manager.CareerManager.getPlayer(player) ?: continue
                if (com.waterful.project.career.manager.CareerManager.hasBranch(cp,
                        com.waterful.project.career.model.Branch.FARMER_FISHERMAN)) {
                    // Base: always Luck I (force=false: don't overwrite stronger Luck like 大洋眷顾)
                    player.addPotionEffect(org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.LUCK, 40, 0, false, false, false))
                    // Base: Conduit Power when in water
                    if (player.isInWater) {
                        player.addPotionEffect(org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.CONDUIT_POWER, 40, 0, false, false, true))
                    }
                    // 骇浪征服者 (Eureka 0): passive — ocean + storm → Resistance II
                    if (player.world.hasStorm() || player.world.isThundering) {
                        val biome = player.location.block.biome.toString().uppercase()
                        if (cp.chosenEurekas.containsKey(com.waterful.project.career.model.Branch.FARMER_FISHERMAN) &&
                            cp.chosenEurekas[com.waterful.project.career.model.Branch.FARMER_FISHERMAN]!!.eurekaDef.id.contains("fisherman_eureka_0") &&
                            biome.contains("OCEAN")) {
                            player.addPotionEffect(org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.RESISTANCE, 40, 1, false, false, true))
                        }
                    }
                }
            }
        }, 20L, 20L)
    }

    private fun startKuangDengTick() {
        server.scheduler.runTaskTimer(this, Runnable {
            com.waterful.project.career.skill.SkillExecutor.tickKuangDeng()
        }, 5L, 5L) // Every 5 ticks for smooth following
    }

    private fun startRanLiaoGuanDaoTick() {
        server.scheduler.runTaskTimer(this, Runnable {
            com.waterful.project.career.skill.SkillExecutor.tickRanLiaoGuanDao()
        }, 10L, 10L) // Every 0.5s: refresh burn time for furnaces in range
    }

    private val autoCastFailNotified = mutableSetOf<String>() // "${uuid}_${skillId}"

    private fun startInvestmentTick() {
        server.scheduler.runTaskTimer(this, Runnable {
            com.waterful.project.career.skill.EurekaEffectHandler.tickInvestmentVision()
        }, 20L, 20L) // Every 1s: check for expired investment tracking
    }

    private fun startAutoReleaseTick() {
        server.scheduler.runTaskTimer(this, Runnable {
            for (player in server.onlinePlayers) {
                val cp = com.waterful.project.career.manager.CareerManager.getPlayer(player) ?: continue
                val now = System.currentTimeMillis()

                // --- Auto-release skills ---
                for ((autoKey, enabled) in cp.autoCastSkills) {
                    if (!enabled) continue
                    if (!autoKey.endsWith("_auto")) continue

                    // Eureka auto-cast: key format "BRANCH_NAME_eureka_auto"
                    if (autoKey.endsWith("_eureka_auto")) {
                        val branchName = autoKey.removeSuffix("_eureka_auto")
                        val branch = com.waterful.project.career.model.Branch.entries.find { it.name == branchName } ?: continue
                        val eureka = cp.chosenEurekas[branch] ?: continue
                        val eurekaId = eureka.eurekaDef.id
                        val cdSeconds = eureka.eurekaDef.cooldownSeconds
                        if (cdSeconds <= 0) continue
                        if (cp.isOnCooldown(eurekaId, cdSeconds, now)) continue
                        val fired = com.waterful.project.career.skill.EurekaEffectHandler.execute(eurekaId, player)
                        if (fired) {
                            autoCastFailNotified.remove("${player.uniqueId}_$eurekaId")
                        } else {
                            val tag = "${player.uniqueId}_$eurekaId"
                            if (tag !in autoCastFailNotified) {
                                autoCastFailNotified.add(tag)
                            }
                        }
                        continue
                    }

                    // Skill auto-cast: key format "BRANCH_NAME_skill_N_auto"
                    val parts = autoKey.removeSuffix("_auto").split("_")
                    if (parts.size < 4) continue
                    val branchName = parts[0] + "_" + parts[1]
                    val skillIdx = parts.last().toIntOrNull() ?: continue
                    val branch = com.waterful.project.career.model.Branch.entries.find { it.name == branchName } ?: continue
                    val skill = cp.getSkill(branch, skillIdx) ?: continue
                    if (skill.currentLevel < 1) continue
                    val skillId = "${branch.name.lowercase()}_skill_$skillIdx"
                    val cdSeconds = skill.skillDef.getCooldown(skill.currentLevel)
                    if (cdSeconds <= 0) continue
                    if (cp.isOnCooldown(skillId, cdSeconds, now)) continue
                    val fired = com.waterful.project.career.skill.SkillExecutor.executeSkill(player, skill, silent = true)
                    if (fired) {
                        cp.setCooldown(skillId, now)
                        autoCastFailNotified.remove("${player.uniqueId}_$skillId")
                    } else {
                        // Condition not met — don't consume CD, show message only once
                        val tag = "${player.uniqueId}_$skillId"
                        if (tag !in autoCastFailNotified) {
                            player.sendMessage("§7[自动释放] ${skill.skillDef.name}：条件不满足，等待中…")
                            autoCastFailNotified.add(tag)
                        }
                    }
                }
            }
        }, 10L, 10L) // Every 10 ticks
    }
}
