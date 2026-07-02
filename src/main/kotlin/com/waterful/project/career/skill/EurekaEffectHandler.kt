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
        // Check cooldown
        val cp = CareerManager.getPlayer(player)
        val def = com.waterful.project.career.data.CareerDataLoader.getEureka(eurekaId)
        if (cp != null && def != null && def.cooldownSeconds > 0) {
            val now = System.currentTimeMillis()
            if (cp.isOnCooldown(eurekaId, def.cooldownSeconds, now)) {
                val remaining = cp.getRemainingCooldown(eurekaId, def.cooldownSeconds, now)
                player.sendMessage(Component.text("顿悟冷却中！剩余 ${remaining} 秒", NamedTextColor.RED))
                return false
            }
        }

        val success = try {
            when (eurekaId) {
                "scholar_enchanter_eureka_1" -> { openEnchantingTable(player); true }
                "architect_fortress_eureka_2" -> { openStonecutter(player); true }
                "architect_traffic_eureka_2" -> { applyDolphinGrace(player); true }
                "architect_demolition_eureka_2" -> { applyTntArrows(player); true }
                "farmer_fisherman_eureka_1" -> { applyFishingExp(player); true }
                "warrior_weapon_eureka_0" -> { applyTridentMastery(player); true }
                "farmer_fisherman_eureka_2" -> { applyGuardianDefense(player); true }
                "worker_lumberjack_eureka_0" -> { giveScaffolding(player); true }
                "worker_miner_eureka_0" -> { applyCaveBuff(player); true }
                "worker_miner_eureka_1" -> { applyDarkAdapt(player); true }
                "worker_miner_eureka_2" -> { locateOres(player); true }
                "scholar_redstone_eureka_1" -> { targetedPulse(player); true }
                "scholar_redstone_eureka_0" -> { redstoneTorchConvert(player); true }
                "warrior_soldier_eureka_1" -> { applyFearlessThrust(player); true }
                "warrior_hunter_eureka_2" -> { applyHolySpring(player); true }
                "chef_master_eureka_1" -> { applyRecipeLegacy(player); true }
                "chef_butcher_eureka_2" -> { applyPursuit(player); true }
                "chef_brewer_eureka_1" -> { applyOverdose(player); true }
                "farmer_merchant_eureka_2" -> { applyInvestmentVision(player); true }
                "warrior_weapon_eureka_1" -> { applyPiercingArrow(player); true }
                "scholar_teacher_eureka_0" -> { applyTeacherExp(player); true }
                "worker_lumberjack_eureka_1" -> { extinguishFires(player); true }
                else -> {
                    player.sendMessage(Component.text("该顿悟无需主动触发", NamedTextColor.GRAY))
                    false
                }
            }
        } catch (e: Exception) {
            player.sendMessage(Component.text("顿悟执行出错: ${e.message}", NamedTextColor.RED))
            false
        }

        // Only set cooldown on success (conditional eurekas like 骇浪征服者 may fail)
        if (success && cp != null && def != null && def.cooldownSeconds > 0) {
            cp.setCooldown(eurekaId, System.currentTimeMillis())
        }
        return success
    }

    // ===== 无畏突刺: 60秒内挥剑/斧向前突进 =====
    private val fearlessThrustPlayers = mutableMapOf<java.util.UUID, Long>()
    private val fearlessThrustLastDash = mutableMapOf<java.util.UUID, Long>()

    private fun applyFearlessThrust(player: Player) {
        fearlessThrustPlayers[player.uniqueId] = System.currentTimeMillis() + 60_000L
        player.sendMessage(Component.text("§5✦ 无畏突刺：持续60秒，手持剑/斧挥动后向前突进"))
    }

    /** Called on arm swing — dash if 无畏突刺 active (works on both air swing and hit) */
    fun onFearlessThrustSwing(player: Player) {
        val expiry = fearlessThrustPlayers[player.uniqueId] ?: return
        if (System.currentTimeMillis() > expiry) { fearlessThrustPlayers.remove(player.uniqueId); return }
        // Anti-spam: max one dash per 400ms
        val last = fearlessThrustLastDash[player.uniqueId] ?: 0L
        if (System.currentTimeMillis() - last < 400) return
        val held = player.inventory.itemInMainHand
        if (!held.type.name.contains("SWORD") && !held.type.name.contains("AXE")) return
        fearlessThrustLastDash[player.uniqueId] = System.currentTimeMillis()
        val dir = player.location.direction.normalize()
        player.velocity = dir.multiply(2.8).setY(0.25)
    }

    // ===== 圣洁泉源: 清除周围4格内所有玩家负面效果 =====
    private fun applyHolySpring(player: Player) {
        var count = 0
        for (entity in player.location.getNearbyEntities(4.0, 4.0, 4.0)) {
            if (entity is Player) {
                // Remove negative potion effects (bad effects)
                val negativeEffects = listOf(
                    org.bukkit.potion.PotionEffectType.POISON, org.bukkit.potion.PotionEffectType.WITHER,
                    org.bukkit.potion.PotionEffectType.WEAKNESS, org.bukkit.potion.PotionEffectType.SLOWNESS,
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, org.bukkit.potion.PotionEffectType.BLINDNESS,
                    org.bukkit.potion.PotionEffectType.NAUSEA, org.bukkit.potion.PotionEffectType.HUNGER,
                    org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, org.bukkit.potion.PotionEffectType.UNLUCK,
                    org.bukkit.potion.PotionEffectType.DARKNESS, org.bukkit.potion.PotionEffectType.LEVITATION,
                    org.bukkit.potion.PotionEffectType.BAD_OMEN, org.bukkit.potion.PotionEffectType.RAID_OMEN,
                    org.bukkit.potion.PotionEffectType.TRIAL_OMEN, org.bukkit.potion.PotionEffectType.WIND_CHARGED,
                    org.bukkit.potion.PotionEffectType.WEAVING, org.bukkit.potion.PotionEffectType.OOZING,
                    org.bukkit.potion.PotionEffectType.INFESTED
                )
                for (effect in negativeEffects) {
                    if (entity.hasPotionEffect(effect)) {
                        entity.removePotionEffect(effect)
                        count++
                    }
                }
            }
        }
        player.sendMessage(Component.text("§5✦ 圣洁泉源：清除了${count}个负面效果"))
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

    private fun applyOceanResistance(player: Player): Boolean {
        val world = player.world
        if (world.hasStorm() || world.isThundering) {
            val biome = player.location.block.biome
            if (biome.toString().contains("OCEAN")) {
                player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 60 * 20, 1, false, true))
                player.sendMessage(Component.text("§5✦ 骇浪征服者：抗性提升II"))
                return true
            }
        }
        player.sendMessage(Component.text("§7骇浪征服者：需要在降水/雷暴天气的海洋中"))
        return false
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

    private val guardianDefensePlayers = mutableMapOf<java.util.UUID, Long>()

    /** Clear all eureka tracking data for a player (called on branch forget) */
    fun clearPlayerTracking(uuid: java.util.UUID) {
        fearlessThrustPlayers.remove(uuid)
        fearlessThrustLastDash.remove(uuid)
        guardianDefensePlayers.remove(uuid)
        tntArrowPlayers.remove(uuid)
        pursuitTargets.entries.removeAll { it.value == uuid }
    }

    // ===== 追猎 (Butcher eureka 2): target mob → debuff 60s → kill reward 25xp =====
    private val pursuitTargets = mutableMapOf<java.util.UUID, java.util.UUID>() // mobUuid -> playerUuid

    private fun applyPursuit(player: Player) {
        val target = player.getTargetEntity(10) ?: run {
            player.sendMessage(Component.text("§c追猎：请对准一个生物！"))
            return
        }
        if (target !is org.bukkit.entity.LivingEntity || target is Player) {
            player.sendMessage(Component.text("§c追猎：不能对玩家使用！"))
            return
        }
        target.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 60 * 20, 0, false, true))
        target.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 60 * 20, 1, false, true))
        pursuitTargets[target.uniqueId] = player.uniqueId
        player.sendMessage(Component.text("§5✦ 追猎：已标记 ${target.name}，击杀后获得25经验值"))
    }

    fun onPursuitKill(entity: org.bukkit.entity.LivingEntity, killer: Player): Boolean {
        val marker = pursuitTargets.remove(entity.uniqueId) ?: return false
        if (marker != killer.uniqueId) return false
        killer.giveExp(25)
        killer.sendMessage(Component.text("§5✦ 追猎：击杀标记目标，获得25经验值！"))
        return true
    }

    // ===== 投资远见 (Merchant eureka 2): track purchases 5min, reward emeralds =====
    private val investmentPlayers = mutableMapOf<java.util.UUID, Pair<Long, Int>>() // UUID -> (expiry, totalSpent)

    private fun applyInvestmentVision(player: Player) {
        investmentPlayers[player.uniqueId] = System.currentTimeMillis() + 5 * 60_000L to 0
        player.sendMessage(Component.text("§5✦ 投资远见：持续5分钟，记录购买花费的绿宝石"))
    }

    /** Called from trade handler: add emerald cost to tracking */
    fun onInvestmentTrade(player: Player, emeraldCost: Int) {
        val (expiry, spent) = investmentPlayers[player.uniqueId] ?: return
        if (System.currentTimeMillis() > expiry) { investmentPlayers.remove(player.uniqueId); return }
        investmentPlayers[player.uniqueId] = expiry to (spent + emeraldCost)
    }

    /** Called periodically or at expiry: check and reward */
    fun tickInvestmentVision() {
        val now = System.currentTimeMillis()
        val toProcess = mutableListOf<java.util.UUID>()
        for ((uuid, data) in investmentPlayers) {
            if (now > data.first) toProcess.add(uuid)
        }
        for (uuid in toProcess) {
            val (_, spent) = investmentPlayers.remove(uuid) ?: continue
            if (spent <= 0) continue
            val player = org.bukkit.Bukkit.getPlayer(uuid) ?: continue
            val multiplier = 0.5 + Math.random() * 1.25 // 0.5 ~ 1.75
            val reward = (spent * multiplier).toInt().coerceAtLeast(1)
            player.inventory.addItem(org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, reward))
            player.sendMessage(Component.text("§5✦ 投资远见：花费${spent}绿宝石，获得${reward}绿宝石回报！"))
        }
    }

    // ===== 过量服用 (Brewer eureka 1): boost all effects +1 lv for 60s, then 8s poison II =====
    private fun applyOverdose(player: Player) {
        var boosted = 0
        for (effect in player.activePotionEffects) {
            val currentAmp = effect.amplifier
            val currentDur = effect.duration
            val type = effect.type
            // Remove old, add boosted version (cap at amplifier 4 = level V)
            player.removePotionEffect(type)
            val newAmp = (currentAmp + 1).coerceAtMost(4)
            if (currentDur > 60 * 20) {
                // If effect lasts longer than 60s, keep boosted for full remaining duration
                player.addPotionEffect(org.bukkit.potion.PotionEffect(type, currentDur, newAmp, effect.isAmbient, effect.hasParticles(), effect.hasIcon()))
            } else {
                // Boost for at least 60s
                player.addPotionEffect(org.bukkit.potion.PotionEffect(type, 60 * 20, newAmp, effect.isAmbient, effect.hasParticles(), effect.hasIcon()))
            }
            boosted++
        }
        player.sendMessage(Component.text("§5✦ 过量服用：${boosted}个效果等级+1，持续60秒"))

        // After 60s: apply poison II for 8s
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            player.addPotionEffect(org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.POISON, 8 * 20, 1, false, true, true))
            player.sendMessage(Component.text("§c过量服用结束：中毒II 8秒"))
        }, 60 * 20L)
    }

    // ===== 地狱厨房: 周围8格所有Chef职业玩家CD-20 =====
    private fun applyRecipeLegacy(player: Player) {
        var count = 0
        for (nearby in player.location.getNearbyPlayers(8.0)) {
            val cp = CareerManager.getPlayer(nearby) ?: continue
            val hasChefClass = cp.selectedClasses.any { it.name.startsWith("CHEF") } ||
                cp.unlockedBranches.keys.any { it.careerClass.name.startsWith("CHEF") }
            if (!hasChefClass) continue
            // Reduce all skill cooldowns by 20 seconds (only for skills, not eurekas)
            val toReduce = mutableListOf<String>()
            for ((key, lastUsed) in cp.cooldowns) {
                if (key.contains("_skill_") && !key.contains("_eureka_")) {
                    toReduce.add(key)
                }
            }
            for (key in toReduce) {
                val old = cp.cooldowns[key] ?: continue
                cp.cooldowns[key] = old - 20_000L // 20 seconds earlier
            }
            count++
            nearby.sendMessage(Component.text("§5✦ 地狱厨房：${player.name} 使你的技能冷却减少20秒！"))
        }
        player.sendMessage(Component.text("§5✦ 地狱厨房：影响了${count}个厨师职业玩家的冷却"))
    }

    private fun applyGuardianDefense(player: Player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE)
        guardianDefensePlayers[player.uniqueId] = System.currentTimeMillis() + 30_000L
        player.sendMessage(Component.text("§5✦ 凝望反制：清除挖掘疲劳，持续30秒内守卫者伤害-6"))
    }

    /** Check if 凝望反制 is currently active for this player */
    fun isGuardianDefenseActive(uuid: java.util.UUID): Boolean {
        val expiry = guardianDefensePlayers[uuid] ?: return false
        if (System.currentTimeMillis() > expiry) { guardianDefensePlayers.remove(uuid); return false }
        return true
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
