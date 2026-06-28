package com.waterful.project.career.skill

import com.waterful.project.career.manager.CareerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Handles eureka skill execution for hotkey-bound eurekas.
 * Each eureka is mapped by its ID from career_data.yml.
 */
object EurekaEffectHandler {

    fun execute(eurekaId: String, player: Player): Boolean {
        return try {
            when (eurekaId) {
                "scholar_enchanter_eureka_1" -> openEnchantingTable(player)
                "architect_fortress_eureka_2" -> openStonecutter(player)
                "architect_traffic_eureka_2" -> applyDolphinGrace(player)
                "architect_demolition_eureka_2" -> applyTntArrows(player)
                "farmer_fisherman_eureka_0" -> applyOceanResistance(player)
                "farmer_fisherman_eureka_1" -> applyFishingExp(player)
                "warrior_weapon_eureka_0" -> applyTridentMastery(player)
                "warrior_weapon_eureka_1" -> applyPiercingArrow(player)
                "scholar_teacher_eureka_0" -> applyTeacherExp(player)
                "farmer_fisherman_eureka_2" -> applyGuardianDefense(player)
                "worker_lumberjack_eureka_0" -> giveScaffolding(player)
                "worker_lumberjack_eureka_1" -> extinguishFires(player)
                "worker_miner_eureka_0" -> applyCaveBuff(player)
                "worker_miner_eureka_1" -> applyDarkAdapt(player)
                "worker_miner_eureka_2" -> locateOres(player)
                "scholar_redstone_eureka_1" -> targetedPulse(player)
                "scholar_redstone_eureka_0" -> redstoneTorchConvert(player)
                else -> return false
            }
            true
        } catch (e: Exception) {
            player.sendMessage(Component.text("顿悟执行出错: ${e.message}", NamedTextColor.RED))
            false
        }
    }

    // ===== Implementations =====

    private fun openEnchantingTable(player: Player) {
        val loc = player.location.clone()
        val plugin = Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return
        val placedBlocks = mutableListOf<org.bukkit.block.Block>()

        // Place enchanting table at player feet
        val tableLoc = loc.clone()
        val oldFloor = tableLoc.block.type
        tableLoc.block.type = Material.ENCHANTING_TABLE
        placedBlocks.add(tableLoc.block)

        // Standard level-30 bookshelf ring (5×5 with 1-block gap, 15 shelves)
        // Place at BOTH y (table level) and y+1 (one above) for guaranteed level 30
        val ringOffsets = listOf(
            -2 to -2, -1 to -2, 0 to -2, 1 to -2, 2 to -2,
            -2 to -1, 2 to -1,
            -2 to 0, 2 to 0,
            -2 to 1, 2 to 1,
            -2 to 2, -1 to 2, 0 to 2, 1 to 2, 2 to 2
        )
        for (yOff in 0..1) { // Both same level and one above
            for ((dx, dz) in ringOffsets) {
                val shelfLoc = tableLoc.clone().add(dx.toDouble(), yOff.toDouble(), dz.toDouble())
                if (shelfLoc.block.type.isAir) {
                    shelfLoc.block.type = Material.BOOKSHELF
                    placedBlocks.add(shelfLoc.block)
                }
            }
        }

        // Open enchanting GUI
        player.openEnchanting(tableLoc, true)

        // Clean up after 2 seconds (after GUI rendering is done)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            placedBlocks.forEach { it.type = Material.AIR }
            tableLoc.block.type = oldFloor
        }, 40L)
        player.sendMessage(Component.text("§5✦ 附魔领域展开：30级附魔台已开启"))
    }

    private fun openStonecutter(player: Player) {
        val loc = player.location.clone()
        val old = loc.block.type
        loc.block.type = Material.STONECUTTER
        player.sendMessage(Component.text("§5✦ 便携式切石机：切石机已生成在你的位置，右击使用"))
        val plugin = Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { loc.block.type = old }, 1200L)
    }

    private fun applyDolphinGrace(player: Player) {
        val world = player.world
        val isOcean = world.name.contains("ocean") || player.location.block.biome.toString().contains("OCEAN")
        val duration = if (isOcean) 45 else 30
        player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, duration * 20, 1, false, true))
        player.sendMessage(Component.text("§5✦ 游弋信风：海豚的恩惠 ${if(isOcean)"III" else "II"}，持续${duration}秒"))
    }

    // TNT arrow active players: UUID -> expiry timestamp
    private val tntArrowPlayers = mutableMapOf<java.util.UUID, Long>()
    // Track spawned TNT entities that should not destroy blocks
    private val noGriefTnt = mutableSetOf<java.util.UUID>()

    private fun applyTntArrows(player: Player) {
        tntArrowPlayers[player.uniqueId] = System.currentTimeMillis() + 30_000L
        player.sendMessage(Component.text("§5✦ 手摇TNT火炮：30秒内弓箭将变为已点燃TNT（不破坏方块）"))
    }

    fun onBowShoot(player: Player, event: org.bukkit.event.entity.EntityShootBowEvent) {
        val expiry = tntArrowPlayers[player.uniqueId] ?: return
        if (System.currentTimeMillis() > expiry) { tntArrowPlayers.remove(player.uniqueId); return }
        event.isCancelled = true
        val arrow = event.projectile
        val tnt = player.world.spawn(arrow.location, org.bukkit.entity.TNTPrimed::class.java)
        tnt.velocity = arrow.velocity
        tnt.fuseTicks = 60
        noGriefTnt.add(tnt.uniqueId)
    }

    /** Call from EntityExplodeEvent listener — cancels block damage for our TNT */
    fun isNoGriefTnt(entity: org.bukkit.entity.Entity): Boolean {
        noGriefTnt.remove(entity.uniqueId) // Clean up after check
        return true // Already tracked = our TNT
    }

    private fun applyOceanResistance(player: Player) {
        val world = player.world
        if (world.hasStorm() || world.isThundering) {
            val biome = player.location.block.biome
            if (biome.toString().contains("OCEAN")) {
                player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 60 * 20, 1, false, true))
                player.sendMessage(Component.text("§5✦ 骇浪征服者：抗性提升II"))
                return
            }
        }
        player.sendMessage(Component.text("§7骇浪征服者：需要在降水/雷暴天气的海洋中"))
    }

    private fun applyFishingExp(player: Player) {
        player.sendMessage(Component.text("§5✦ 授人以渔：每次钓鱼将额外获得1~6点经验"))
    }

    private fun applyTridentMastery(player: Player) {
        player.sendMessage(Component.text("§5✦ 涌潮长戟：三叉戟近战回血，远程挖掘疲劳"))
    }

    private fun applyPiercingArrow(player: Player) {
        player.sendMessage(Component.text("§5✦ 穿甲箭头：弩箭+1点真实伤害"))
    }

    private fun applyTeacherExp(player: Player) {
        val found = (-3..3).any { x -> (-3..3).any { y -> (-3..3).any { z ->
            player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type == Material.LECTERN
        }}}
        if (found) {
            player.giveExp(20)
            player.sendMessage(Component.text("§5✦ 诲人不倦：获得20点经验"))
        } else {
            player.sendMessage(Component.text("§c诲人不倦：周围没有讲台"))
        }
    }

    private fun applyGuardianDefense(player: Player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE)
        player.sendMessage(Component.text("§5✦ 凝望反制：清除挖掘疲劳，守卫者伤害-6"))
    }

    private fun giveScaffolding(player: Player) {
        player.inventory.addItem(ItemStack(Material.SCAFFOLDING, 16))
        player.sendMessage(Component.text("§5✦ 巨树攀登者：获得16个脚手架"))
    }

    private fun extinguishFires(player: Player) {
        var count = 0
        for (x in -5..5) for (y in -5..5) for (z in -5..5) {
            val block = player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
            if (block.type == Material.FIRE) { block.type = Material.AIR; count++ }
        }
        player.sendMessage(Component.text("§5✦ 护林员：熄灭了${count}团火焰"))
    }

    private fun applyCaveBuff(player: Player) {
        if (player.location.block.lightLevel < 8) {
            player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 30 * 20, 0, false, true))
            player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 30 * 20, 0, false, true))
            player.sendMessage(Component.text("§5✦ 自然矿洞勘探：夜视+抗性提升"))
        } else {
            player.sendMessage(Component.text("§7自然矿洞勘探：需要在洞穴环境中"))
        }
    }

    private fun applyDarkAdapt(player: Player) {
        if (player.location.block.lightLevel < 4) {
            player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 60 * 20, 0, false, true))
            player.sendMessage(Component.text("§5✦ 黑暗适应：夜视效果60秒"))
        }
    }

    private fun locateOres(player: Player) {
        val ores = listOf(Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.ANCIENT_DEBRIS,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE)
        var found: org.bukkit.Location? = null
        var minDist = Double.MAX_VALUE
        for (x in -8..8) for (y in -8..8) for (z in -8..8) {
            val loc = player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
            if (loc.block.type in ores) {
                val dist = loc.distanceSquared(player.location)
                if (dist < minDist) { minDist = dist; found = loc.clone() }
            }
        }
        if (found != null) {
            player.sendMessage(Component.text("§5✦ 地质学：最近的矿石在 ${found.blockX}, ${found.blockY}, ${found.blockZ} (${found.block.type.name})"))
        } else {
            player.sendMessage(Component.text("§7地质学：周围8格内未发现矿石"))
        }
    }

    /** 定点脉冲: 消耗16红石粉，破坏周围4格内所有进阶红石器械 */
    private fun targetedPulse(player: Player) {
        // Check and consume 16 redstone dust
        if (!player.inventory.contains(Material.REDSTONE, 16)) {
            player.sendMessage(Component.text("§c定点脉冲：需要16个红石粉！"))
            return
        }
        player.inventory.removeItem(ItemStack(Material.REDSTONE, 16))

        val advancedRedstone = setOf(
            Material.REPEATER, Material.COMPARATOR, Material.PISTON, Material.STICKY_PISTON,
            Material.OBSERVER, Material.DROPPER, Material.DISPENSER, Material.HOPPER,
            Material.REDSTONE_BLOCK, Material.REDSTONE_LAMP, Material.DAYLIGHT_DETECTOR,
            Material.TARGET, Material.LECTERN, Material.NOTE_BLOCK, Material.TRIPWIRE_HOOK
        )

        var count = 0
        for (x in -4..4) for (y in -4..4) for (z in -4..4) {
            val block = player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
            if (block.type in advancedRedstone) {
                block.breakNaturally()
                count++
            }
        }
        player.sendMessage(Component.text("§5✦ 定点脉冲：消耗16红石粉，摧毁了${count}个红石器械"))
    }

    /** 红色炬火: 火把→红石火把 + 攻击25%点燃 */
    private fun redstoneTorchConvert(player: Player) {
        // Convert regular torches to redstone torches in inventory
        var count = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == Material.TORCH) {
                item.type = Material.REDSTONE_TORCH
                count++
            }
        }
        if (count > 0) {
            player.sendMessage(Component.text("§5✦ 红色炬火：${count}个火把已转化为红石火把"))
        } else {
            player.sendMessage(Component.text("§5✦ 红色炬火：背包中没有火把可转换（被动：合成台火把→红石火把，攻击25%点燃）"))
        }
    }
}
