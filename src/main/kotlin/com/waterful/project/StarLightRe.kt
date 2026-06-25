package com.waterful.project

import com.waterful.project.career.data.CareerDataLoader
import com.waterful.project.career.data.ConfigManager
import com.waterful.project.career.data.PlayerDataStore
import com.waterful.project.career.listener.AntiFarmListener
import com.waterful.project.career.listener.CareerGateListener
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
import com.waterful.project.career.command.DelCareerCommand
import com.waterful.project.career.command.ListeningCommand
import com.waterful.project.career.command.ResonanceCommand
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

        // Register listeners
        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(CareerGUIListener(), this)
        server.pluginManager.registerEvents(ResonanceListener(), this)
        server.pluginManager.registerEvents(AntiFarmListener(), this)
        server.pluginManager.registerEvents(PassiveSkillListener(), this)
        server.pluginManager.registerEvents(CareerGateListener(), this)

        // Start resonance tick task
        startResonanceTask()

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
}
