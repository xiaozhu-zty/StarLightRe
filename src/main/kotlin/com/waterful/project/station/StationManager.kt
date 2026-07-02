package com.waterful.project.station

import com.waterful.project.career.hasBranch
import com.waterful.project.career.model.Branch
import com.waterful.project.career.skillLevel
import com.waterful.project.stamina.StaminaManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

object StationManager : Listener {

    val stations = mutableMapOf<UUID, StationData>()
    /** Per-player: which station halos are affecting this player (ownerName -> staminaPerSec) */
    val haloMap = mutableMapOf<UUID, MutableMap<String, Double>>()
    /** Quick lookup: "worldName:x:y:z" -> owner UUID */
    private val blockLookup = mutableMapOf<String, UUID>()

    private var plugin: JavaPlugin? = null
    private val KEY_OWNER = NamespacedKey("starlightre", "station_owner_id")
    private val KEY_LEVEL = NamespacedKey("starlightre", "station_level")

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)
        registerRecipes()
        startHaloTick()
    }

    /** Restore station from persistence (call on player join after data loaded) */
    fun restoreStation(uuid: UUID, data: StationData?) {
        if (data != null) {
            stations[uuid] = data
            data.location?.let { blockLookup[blockKey(it)] = uuid }
        } else {
            stations.putIfAbsent(uuid, StationData(uuid))
        }
    }

    /** Get serializable data for persistence */
    fun getStationData(uuid: UUID): StationData? = stations[uuid]

    // ===== Recipes =====

    private fun registerRecipes() {
        val p = plugin ?: return
        // Lv.1: barrel + cobblestone + iron block
        registerShaped(p, "station_1", 1,
            listOf("ABA", "BIB", "ABA"),
            mapOf('A' to Material.BARREL, 'B' to Material.COBBLESTONE, 'I' to Material.IRON_BLOCK))
        // Lv.2: chorus flower + crying obsidian + netherite block
        registerShaped(p, "station_2", 2,
            listOf("ACA", "CIC", "ACA"),
            mapOf('A' to Material.CHORUS_FLOWER, 'C' to Material.CRYING_OBSIDIAN, 'I' to Material.NETHERITE_BLOCK))
        // Lv.3: exp bottle + dragon head + nether star
        registerShaped(p, "station_3", 3,
            listOf("ABA", "BIB", "ABA"),
            mapOf('A' to Material.EXPERIENCE_BOTTLE, 'B' to Material.DRAGON_HEAD, 'I' to Material.NETHER_STAR))
    }

    private fun registerShaped(plugin: JavaPlugin, name: String, level: Int, shape: List<String>, ingredients: Map<Char, Material>) {
        val result = ItemStack(Material.SOUL_CAMPFIRE).apply {
            editMeta { meta: ItemMeta ->
                meta.setDisplayName("§6驻扎篝火")
                meta.lore = listOf("§8| §7等级: §a${level.toRoman()}", "", "§8| §7注意: §c一旦放置新的篝火，旧的篝火将会失效")
                meta.persistentDataContainer.set(KEY_LEVEL, PersistentDataType.INTEGER, level)
            }
        }
        val key = NamespacedKey("starlightre", name)
        try {
            val recipe = org.bukkit.inventory.ShapedRecipe(key, result)
            recipe.shape(*shape.toTypedArray())
            ingredients.forEach { (char, mat) -> recipe.setIngredient(char, mat) }
            Bukkit.addRecipe(recipe)
        } catch (_: Exception) {}
    }

    // ===== Halo tick (every 1s) =====

    private fun startHaloTick() {
        Bukkit.getScheduler().runTaskTimer(plugin!!, Runnable {
            // Reset halo map
            haloMap.values.forEach { it.clear() }

            for (station in stations.values) {
                val loc = station.location ?: continue
                val world = loc.world ?: continue

                // Ensure block exists and is lit
                val block = loc.block
                if (block.type != Material.SOUL_CAMPFIRE) {
                    block.type = Material.SOUL_CAMPFIRE
                }
                (block.blockData as? Campfire)?.let {
                    if (!it.isLit) {
                        val lit = it.clone() as Campfire; lit.isLit = true; block.blockData = lit
                    }
                }

                val hRange = station.horizontal()
                val vRange = station.vertical()
                val halo = station.halo()
                val ownerOnline = Bukkit.getOfflinePlayer(station.ownerId).isOnline

                for (player in world.players) {
                    val pl = player.location
                    val dx = pl.x - loc.x; val dz = pl.z - loc.z
                    val hDist = kotlin.math.sqrt(dx * dx + dz * dz)
                    if (hDist > hRange) continue
                    val vDist = kotlin.math.abs(pl.y - loc.y)
                    if (vDist > vRange) continue

                    var add = halo * minOf(1.0, 1.0 - hDist / hRange + 0.3)
                    if (player.uniqueId == station.ownerId) add *= 1.5
                    if (!ownerOnline) add /= 2.0

                    // 前进营地 skill bonus
                    val hasSkill = player.hasBranch(Branch.WARRIOR_EXPLORER)
                    val skillLv = if (hasSkill) player.skillLevel(Branch.WARRIOR_EXPLORER, 2) else 0
                    if (hasSkill && skillLv > 0) {
                        add *= when {
                            skillLv == 3 && StaminaManager.getStamina(player).stamina <= 1000 -> 1.4
                            else -> 1.0 + skillLv * 0.1 // 1.1, 1.2, 1.3
                        }
                    }

                    StaminaManager.getStamina(player).add(add)
                    haloMap.getOrPut(player.uniqueId) { mutableMapOf() }[Bukkit.getOfflinePlayer(station.ownerId).name ?: "?"] = add
                }
            }
        }, 20L, 20L)
    }

    // ===== Events =====

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val recipe = event.recipe ?: return
        val result = recipe.result
        if (result.type != Material.SOUL_CAMPFIRE) return
        val meta = result.itemMeta ?: return
        val level = meta.persistentDataContainer.get(KEY_LEVEL, PersistentDataType.INTEGER) ?: return
        val player = event.whoClicked as? Player ?: return

        val station = stations.getOrPut(player.uniqueId) { StationData(player.uniqueId) }
        station.level = level
        station.stamp = System.currentTimeMillis() - 100000000L
        event.currentItem = station.generateItem()
        player.sendMessage("§b繁星工坊 §7>> 驻扎等级已经变更为 §e${level.toRoman()}")
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val meta = event.itemInHand.itemMeta ?: return
        val ownerStr = meta.persistentDataContainer.get(KEY_OWNER, PersistentDataType.STRING) ?: return
        val ownerId = UUID.fromString(ownerStr)
        val station = stations.getOrPut(ownerId) { StationData(ownerId) }

        val result = station.place(event.player, event.block.location)
        event.player.sendMessage("§b繁星工坊 §7>> ${result.second}")
        if (!result.first) {
            event.isCancelled = true
        } else {
            // Register in lookup
            val loc = event.block.location
            blockLookup[blockKey(loc)] = ownerId
        }
    }

    private fun getStationAt(block: org.bukkit.block.Block): StationData? {
        val key = blockKey(block.world, block.x, block.y, block.z)
        val ownerId = blockLookup[key] ?: return null
        return stations[ownerId]
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.SOUL_CAMPFIRE) return
        val station = getStationAt(block) ?: return
        val player = event.player
        event.isCancelled = true

        if (player.uniqueId == station.ownerId) {
            station.deleteFromWorld()
            blockLookup.remove(blockKey(block.world, block.x, block.y, block.z))
            player.sendMessage("§b繁星工坊 §7>> 成功回收驻扎篝火")
            player.inventory.addItem(station.generateItem()).values.forEach { player.world.dropItemNaturally(player.location, it) }
        } else {
            station.deleteFromWorld()
            blockLookup.remove(blockKey(block.world, block.x, block.y, block.z))
            Bukkit.getOfflinePlayer(station.ownerId).let { if (it.isOnline) (it as Player).sendMessage("§b繁星工坊 §7>> 你的驻扎篝火被 §e${player.name} §7破坏") }
            player.sendMessage("§b繁星工坊 §7>> 破坏了 §e${Bukkit.getOfflinePlayer(station.ownerId).name ?: "?"} §7的驻扎篝火")
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { it.type == Material.SOUL_CAMPFIRE && blockKey(it.world, it.x, it.y, it.z) in blockLookup }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { it.type == Material.SOUL_CAMPFIRE && blockKey(it.world, it.x, it.y, it.z) in blockLookup }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.SOUL_CAMPFIRE) return
        val station = getStationAt(block) ?: return
        event.isCancelled = true
        event.player.sendMessage("§b繁星工坊 §7>> 这是 §e${Bukkit.getOfflinePlayer(station.ownerId).name ?: "?"} §7的驻扎篝火，等级 ${station.level.toRoman()}")
    }

    // ===== Query helpers =====

    private fun blockKey(world: org.bukkit.World, x: Int, y: Int, z: Int): String = "${world.name}:$x:$y:$z"
    private fun blockKey(loc: org.bukkit.Location): String = "${loc.world?.name}:${loc.blockX}:${loc.blockY}:${loc.blockZ}"

    /** Get total stamina recovery rate for a player */
    fun getRecoveryRate(player: Player): Double {
        return haloMap[player.uniqueId]?.values?.sum() ?: 0.0
    }
}
