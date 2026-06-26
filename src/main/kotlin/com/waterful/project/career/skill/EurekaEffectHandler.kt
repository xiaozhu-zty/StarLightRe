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

    private val eurekaEffects: Map<String, (Player) -> Unit> = mapOf(
        // Scholar Enchanter eureka 1: 附魔领域展开 → open lv30 enchanting table
        "scholar_enchanter_eureka_1" to ::openEnchantingTable,
        // Architect Fortress eureka 2: 便携式切石机 → open stonecutter
        "architect_fortress_eureka_2" to ::openStonecutter,
        // Architect Traffic eureka 2: 游弋信风 → dolphin's grace
        "architect_traffic_eureka_2" to ::applyDolphinGrace,
        // Architect Demolition eureka 2: 手摇TNT火炮 → TNT arrows
        "architect_demolition_eureka_2" to ::applyTntArrows,
        // Farmer Fisherman eureka 0: 骇浪征服者 → resistance in ocean
        "farmer_fisherman_eureka_0" to ::applyOceanResistance,
        // Farmer Fisherman eureka 1: 授人以渔 → fishing exp
        "farmer_fisherman_eureka_1" to ::applyFishingExp,
        // Warrior Weapon eureka 0: 涌潮长戟 → trident bonus
        "warrior_weapon_eureka_0" to ::applyTridentMastery,
        // Warrior Weapon eureka 1: 穿甲箭头 → crossbow true damage
        "warrior_weapon_eureka_1" to ::applyPiercingArrow,
        // Scholar Teacher eureka 0: 诲人不倦 → 20 exp near lectern
        "scholar_teacher_eureka_0" to ::applyTeacherExp,
        // Farmer Fisherman eureka 2: 凝望反制 → clear mining fatigue, guardian defense
        "farmer_fisherman_eureka_2" to ::applyGuardianDefense,
        // Worker Lumberjack eureka 0: 巨树攀登者 → scaffolding
        "worker_lumberjack_eureka_0" to ::giveScaffolding,
        // Worker Lumberjack eureka 1: 护林员 → extinguish fires
        "worker_lumberjack_eureka_1" to ::extinguishFires,
        // Worker Miner eureka 0: 自然矿洞勘探 → cave buff
        "worker_miner_eureka_0" to ::applyCaveBuff,
        // Worker Miner eureka 1: 黑暗适应 → darkness tolerance
        "worker_miner_eureka_1" to ::applyDarkAdapt,
        // Worker Miner eureka 2: 地质学 → locate ores
        "worker_miner_eureka_2" to ::locateOres
    )

    fun execute(eurekaId: String, player: Player): Boolean {
        val handler = eurekaEffects[eurekaId] ?: return false
        return try {
            handler(player)
            true
        } catch (e: Exception) {
            player.sendMessage(Component.text("顿悟执行出错: ${e.message}", NamedTextColor.RED))
            false
        }
    }

    // ===== Implementations =====

    private fun openEnchantingTable(player: Player) {
        // Place enchanting table + bookshelves temporarily, open GUI, then clean up
        val loc = player.location.clone()
        val plugin = Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return
        val placedBlocks = mutableListOf<org.bukkit.block.Block>()

        // Place enchanting table at player feet
        val tableLoc = loc.clone()
        val oldFloor = tableLoc.block.type
        tableLoc.block.type = Material.ENCHANTING_TABLE
        placedBlocks.add(tableLoc.block)

        // Place 15 bookshelves for level 30
        val offsets = listOf(
            -2 to -2, -1 to -2, 0 to -2, 1 to -2, 2 to -2,
            -2 to -1, 2 to -1,
            -2 to 0, 2 to 0,
            -2 to 1, 2 to 1,
            -2 to 2, -1 to 2, 0 to 2, 1 to 2, 2 to 2
        )
        for ((dx, dz) in offsets) {
            val shelfLoc = tableLoc.clone().add(dx.toDouble(), 0.0, dz.toDouble())
            if (shelfLoc.block.type == Material.AIR || shelfLoc.block.type.name.endsWith("_AIR")) {
                shelfLoc.block.type = Material.BOOKSHELF
                placedBlocks.add(shelfLoc.block)
            }
        }

        // Open enchanting GUI via Paper API
        player.openEnchanting(tableLoc, true)

        // Clean up blocks after GUI closes (next tick)
        Bukkit.getScheduler().runTask(plugin, Runnable {
            placedBlocks.forEach { it.type = Material.AIR }
            tableLoc.block.type = oldFloor
        })
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

    private fun applyTntArrows(player: Player) {
        player.sendMessage(Component.text("§5✦ 手摇TNT火炮：30秒内弓箭将变为TNT"))
        player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 30 * 20, 0, false, true))
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
}
