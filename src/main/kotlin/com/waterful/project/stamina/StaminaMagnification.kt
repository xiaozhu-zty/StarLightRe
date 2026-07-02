package com.waterful.project.stamina

import com.waterful.project.career.hasBranch
import com.waterful.project.career.hasEureka
import com.waterful.project.career.model.Branch
import com.waterful.project.career.skillLevel
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Boat
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player

/**
 * Stamina consumption magnification calculator — ported from StarLightCore.
 *
 * Returns the multiplier applied to movement distance for stamina cost.
 * Lower = more efficient movement.
 */
object StaminaMagnification {

    private val pathTypes = setOf(Material.DIRT_PATH)
    private val roadTypes = setOf(
        Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
        Material.DEEPSLATE_BRICKS, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES,
        Material.BRICKS, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
        Material.GRANITE, Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE,
        Material.ANDESITE, Material.POLISHED_ANDESITE,
        Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
        Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
        Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
        Material.CRIMSON_PLANKS, Material.WARPED_PLANKS,
        Material.BLACKSTONE, Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
        Material.SANDSTONE, Material.RED_SANDSTONE, Material.SMOOTH_SANDSTONE,
        Material.PRISMARINE, Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS,
        Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
        Material.END_STONE, Material.END_STONE_BRICKS,
        Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ,
        Material.CALCITE, Material.TUFF, Material.DRIPSTONE_BLOCK,
        Material.TERRACOTTA, Material.WHITE_TERRACOTTA
    )

    fun getMagnification(player: Player, isTeleport: Boolean = false): Double {
        if (player.isFlying || player.gameMode == GameMode.SPECTATOR) return 0.0

        var result: Double

        if (isTeleport) {
            result = 0.5
        } else if (player.isInsideVehicle) {
            val vehicle = player.vehicle
            result = when {
                vehicle is Boat -> 0.75
                vehicle is Minecart -> 0.3
                else -> 0.4
            }
        } else {
            result = when {
                player.isRiptiding -> 0.75
                player.isSwimming -> 1.0
                player.isGliding -> 1.5
                player.isSneaking -> 0.15
                player.isSprinting -> 1.0
                else -> 0.35
            }

            // Terrain modifiers (only in NORMAL world)
            val world = player.world
            if (world.environment == World.Environment.NORMAL) {
                val ground = getGroundBlock(player, world)
                val groundType = ground.first
                val groundBiome = ground.second
                val y = player.location.y
                if (y >= 60) {
                    if (roadTypes.contains(groundType)) result *= 0.5
                    if (pathTypes.contains(groundType)) result *= 0.75
                    if (groundBiome.contains("SWAMP")) result *= 1.5
                } else {
                    result *= 1.0 + 0.01 * (60.0 - y)
                }
            }
        }

        // World environment multiplier
        result *= when (player.world.environment) {
            World.Environment.NORMAL -> 1.0
            World.Environment.NETHER -> 4.0
            World.Environment.THE_END -> 1.5
            else -> 1.0
        }

        // Station halo: being in any station's range reduces stamina consumption
        val stationBonus = com.waterful.project.station.StationManager.stations.values.any { station ->
            val loc = station.location ?: return@any false
            if (player.location.world != loc.world) return@any false
            val dx = player.location.x - loc.x
            val dz = player.location.z - loc.z
            val hDist = kotlin.math.sqrt(dx * dx + dz * dz)
            val vDist = kotlin.math.abs(player.location.y - loc.y)
            hDist <= station.horizontal() && vDist <= station.vertical()
        }
        if (stationBonus) result *= 0.25

        // Explorer career: 原野跋涉 reduces stamina consumption
        if (player.hasBranch(Branch.WARRIOR_EXPLORER)) {
            val lv = player.skillLevel(Branch.WARRIOR_EXPLORER, 0) // 原野跋涉 = skill 0
            result *= when (lv) {
                0 -> 0.9
                1 -> 0.8
                2 -> 0.7
                3 -> 0.6
                else -> 1.0
            }
        }

        // Explorer eureka 0: 山海一过 — stamina ×0.3 (not 0.5 as previously)
        if (player.hasEureka(Branch.WARRIOR_EXPLORER, 0)) {
            result *= 0.3
        }

        return result
    }

    /** Get the first non-air block below the player and its biome */
    private fun getGroundBlock(player: Player, world: World): Pair<Material, String> {
        var y = player.location.y
        while (y-- >= -64) {
            val loc = player.location.clone()
            loc.y = y
            val block = world.getBlockAt(loc)
            if (block.type != Material.AIR) {
                return block.type to block.biome.toString().uppercase()
            }
        }
        return Material.AIR to ""
    }
}
