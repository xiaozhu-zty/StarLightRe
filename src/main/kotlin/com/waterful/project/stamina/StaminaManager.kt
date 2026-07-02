package com.waterful.project.stamina

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.abs

/**
 * Stamina system manager — ported from StarLightCore.
 *
 * - Movement costs stamina based on distance × magnification
 * - Resting (stationary) restores stamina
 * - Sleeping restores stamina faster
 * - Eating restores stamina
 * - Low stamina applies progressive debuffs
 * - High stamina + sprint jump gives a velocity boost
 */
object StaminaManager : Listener {

    val staminaMap = mutableMapOf<UUID, StaminaData>()
    private val magnificationMap = mutableMapOf<UUID, Double>()
    private val locationMap = mutableMapOf<UUID, org.bukkit.Location>()
    private val resting = mutableSetOf<UUID>()

    private var plugin: JavaPlugin? = null
    private var dataStore: com.waterful.project.career.data.PlayerDataStore? = null

    fun init(plugin: JavaPlugin, dataStore: com.waterful.project.career.data.PlayerDataStore) {
        this.plugin = plugin
        this.dataStore = dataStore
        Bukkit.getPluginManager().registerEvents(this, plugin)
        startStaminaTick()
        startDebuffTick()
    }

    fun getStamina(player: Player): StaminaData {
        return staminaMap.getOrPut(player.uniqueId) { StaminaData() }
    }

    fun getMagnification(player: Player): Double {
        return magnificationMap[player.uniqueId] ?: 1.0
    }

    /** /tl — show current stamina */
    fun showStamina(player: Player) {
        val s = getStamina(player)
        val display = s.display()
        player.sendMessage("§a☀ 当前体力: $display")
    }

    /** /tlbl — show current magnification */
    fun showMagnification(player: Player) {
        val mag = getMagnification(player)
        player.sendMessage("§a☀ 当前体力消耗倍率: §e${"%.2f".format(mag)}")
    }

    fun isResting(player: Player): Boolean = player.uniqueId in resting

    // ---- Core tick: stamina consumption & recovery ----

    private fun startStaminaTick() {
        Bukkit.getScheduler().runTaskTimer(plugin!!, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val uuid = player.uniqueId
                val stamina = staminaMap[uuid] ?: continue

                // Sleeping recovery
                if (player.isSleeping) {
                    stamina.add(1.5)
                }

                // Calculate and store magnification
                val mag = StaminaMagnification.getMagnification(player)
                magnificationMap[uuid] = mag

                // Movement tracking
                val currentLoc = player.location
                val lastLoc = locationMap[uuid]
                if (lastLoc == null) {
                    locationMap[uuid] = currentLoc.clone()
                    continue
                }

                if (lastLoc.world != currentLoc.world) {
                    locationMap[uuid] = currentLoc.clone()
                    continue
                }

                val distance = lastLoc.distance(currentLoc)
                if (distance <= 0.02) {
                    // Resting — very slight movement (e.g., micro-adjustments)
                    resting.add(uuid)
                    stamina.add(0.5)
                } else {
                    resting.remove(uuid)
                    locationMap[uuid] = currentLoc.clone()
                    // Stamina cost = distance × magnification
                    stamina.take(mag * distance)
                }
            }
        }, 20L, 20L) // Every 1 second
    }

    // ---- Debuff tick ----

    private fun startDebuffTick() {
        Bukkit.getScheduler().runTaskTimer(plugin!!, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val st = staminaMap[player.uniqueId]?.stamina ?: continue
                when {
                    st <= 250.0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 6 * 20, 2, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 6 * 20, 2, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 8 * 20, 1, false, false))
                    }
                    st <= 500.0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 6 * 20, 2, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 6 * 20, 2, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 8 * 20, 0, false, false))
                    }
                    st <= 750.0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 6 * 20, 2, false, false))
                        player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 6 * 20, 1, false, false))
                    }
                    st <= 1000.0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 6 * 20, 1, false, false))
                    }
                }
            }
        }, 5L, 5L) // Every 0.25s for fast debuff response
    }

    // ---- Event handlers ----

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        // Load from persistence or create fresh
        val loaded = dataStore?.loadStamina(uuid) ?: StaminaData()
        staminaMap[uuid] = loaded
        locationMap[uuid] = event.player.location.clone()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        // Save stamina to persistence
        staminaMap[uuid]?.let { dataStore?.saveStamina(uuid, it) }
        locationMap.remove(uuid)
        magnificationMap.remove(uuid)
        resting.remove(uuid)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        locationMap.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEat(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        val new = event.foodLevel
        val old = player.foodLevel
        if (new > old && !player.hasPotionEffect(PotionEffectType.SATURATION)) {
            val stamina = staminaMap[player.uniqueId] ?: return
            stamina.add((new - old) * 3.0)
        }
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val uuid = player.uniqueId
        locationMap.remove(uuid)
        // Only apply stamina cost for ender pearl and chorus fruit teleports
        if (event.cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
            event.cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
        ) return
        val from = event.from
        val to = event.to ?: return
        if (from.world != to.world) return

        val mag = StaminaMagnification.getMagnification(player, isTeleport = true)
        magnificationMap[uuid] = mag
        val distance = from.distance(to)
        val stamina = staminaMap[uuid] ?: return
        stamina.take(distance * mag)
    }

    // ---- High-stamina sprint jump boost ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        // Reset location tracking on fall damage to prevent huge stamina drain
        val player = event.entity as? Player ?: return
        locationMap.remove(player.uniqueId)
    }
}
