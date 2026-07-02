package com.waterful.project.station

import com.waterful.project.career.hasEureka
import com.waterful.project.career.model.Branch
import com.waterful.project.career.skillLevel
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import java.util.*

/**
 * Station (驻扎信标) data — ported from StarLightCore.
 *
 * Represents a player's soul campfire beacon.
 * Provides stamina halo in range. Level determines range and cooldown.
 */
data class StationData(
    val ownerId: UUID,
    var level: Int = 1,
    var location: Location? = null,
    var stamp: Long = System.currentTimeMillis()
) {
    /** Re-place cooldown in seconds */
    fun cooldown(): Int = when (level) {
        1 -> 1800   // 30 min
        2 -> 3600   // 1 hour
        3 -> 7200   // 2 hours
        else -> 7200
    }

    /** Base stamina restoration per second */
    fun halo(): Double = when (level) {
        1 -> 3.0
        2 -> 6.0
        3 -> 12.0
        else -> 3.0
    }

    /** Horizontal range in blocks */
    fun horizontal(): Int = when (level) {
        1 -> 16
        2 -> 32
        3 -> 48
        else -> 16
    }

    /** Vertical range in blocks */
    fun vertical(): Int = when (level) {
        1 -> 72
        2 -> 144
        3 -> 512
        else -> 72
    }

    /** Check if station can be re-placed. Returns (canPlace, cooldownRemaining in seconds) */
    fun checkStamp(): Pair<Boolean, Int> {
        val elapsed = ((System.currentTimeMillis() - stamp) / 1000).toInt()
        val cd = cooldown()
        val remaining = cd - elapsed
        return (remaining <= 0) to remaining.coerceAtLeast(0)
    }

    /** Create the Station item (soul campfire with lore + PDC) */
    fun generateItem(): ItemStack {
        val item = ItemStack(Material.SOUL_CAMPFIRE)
        item.editMeta { meta: ItemMeta ->
            meta.setDisplayName("§6驻扎篝火")
            meta.lore = listOf(
                "§8| §7主人: §e${Bukkit.getOfflinePlayer(ownerId).name ?: ownerId.toString()}",
                "§8| §7等级: §a${level.toRoman()}",
                "§8| §7范围: §b${horizontal()}格 §7(水平)",
                "       §b${vertical()}格 §7(垂直)",
                "§8| §7冷却: §6${cooldown()}s",
                "",
                "§8| §7放置于地面上以提供范围内的体力光环效果",
                "§8| §7注意: §c回收后重放置需要较长冷却时间"
            )
            meta.persistentDataContainer.set(
                NamespacedKey("starlightre", "station_owner_id"),
                PersistentDataType.STRING,
                ownerId.toString()
            )
        }
        return item
    }

    /** Destroy the station block from the world (doesn't give item) */
    fun deleteFromWorld() {
        location?.let { loc ->
            val block = loc.block
            if (block.type == Material.SOUL_CAMPFIRE) {
                block.type = Material.AIR
            }
        }
        location = null
        stamp = System.currentTimeMillis()
    }

    /** Place the station at a location. Returns (success, message) */
    fun place(player: Player, loc: Location): Pair<Boolean, String> {
        if (player.uniqueId != ownerId) return false to "无法放置不属于自己的驻扎篝火"
        if (player.world.getHighestBlockYAt(loc) > loc.y && player.world.environment == World.Environment.NORMAL)
            return false to "驻扎篝火必须放置于地表"
        if (player.hasEureka(Branch.WARRIOR_EXPLORER, 0)) // 山海一过
            return false to "你已经无法再放置驻扎篝火了"

        val (canPlace, remaining) = checkStamp()
        if (!canPlace) return false to "重放置冷却中，还有 §e${remaining.secToDisplay()}"

        // Remove old station if exists
        deleteFromWorld()
        location = loc

        // Clear pillars above station (only in normal world)
        if (loc.world?.environment == World.Environment.NORMAL) {
            for (y in loc.blockY + 1..loc.world!!.maxHeight) {
                val above = loc.clone().also { it.y = y.toDouble() }.block
                if (above.type != Material.AIR) {
                    above.type = Material.AIR
                }
            }
        }

        // Place soul campfire with PDC
        val block = loc.block
        block.type = Material.SOUL_CAMPFIRE
        val blockPdc = block.chunk.persistentDataContainer
        // Use block-relative key

        return true to "放置驻扎篝火，开始生效…"
    }

    /** Get stamina bonus for a player in halo range */
    fun getStaminaBonus(player: Player, hasSkillBonus: Boolean, skillLevel: Int): Double {
        val stationLoc = location ?: return 0.0
        if (player.location.world != stationLoc.world) return 0.0

        val dx = player.location.x - stationLoc.x
        val dz = player.location.z - stationLoc.z
        val hDist = kotlin.math.sqrt(dx * dx + dz * dz)
        if (hDist > horizontal()) return 0.0
        val vDist = kotlin.math.abs(player.location.y - stationLoc.y)
        if (vDist > vertical()) return 0.0

        var add = halo() * minOf(1.0, 1.0 - hDist / horizontal() + 0.3)
        if (player.uniqueId == ownerId) add *= 1.5
        if (!Bukkit.getOfflinePlayer(ownerId).isOnline) add /= 2.0

        // 前进营地 skill bonus
        if (hasSkillBonus) {
            val mag = when (skillLevel) { 1 -> 1.1; 2 -> 1.2; 3 -> 1.3; else -> 1.0 }
            add *= if (skillLevel == 3 && com.waterful.project.stamina.StaminaManager.getStamina(player).stamina <= 1000) 1.4
            else mag
        }

        return add
    }

    companion object {
        /** Check if a soul campfire block is a station */
        fun isStationBlock(loc: Location): Boolean {
            val block = loc.block
            if (block.type != Material.SOUL_CAMPFIRE) return false
            return StationManager.stations.values.any { it.location == loc }
        }
    }
}

fun Int.toRoman(): String = when (this) {
    1 -> "I"; 2 -> "II"; 3 -> "III"
    else -> toString()
}

fun Int.secToDisplay(): String {
    val m = this / 60
    val s = this % 60
    return if (m > 0) "${m}分${s}秒" else "${s}秒"
}
