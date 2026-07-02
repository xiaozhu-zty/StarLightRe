package com.waterful.project.career.listener

import com.waterful.project.career.*
import com.waterful.project.career.manager.DebugManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.skill.SkillExecutor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Handles PASSIVE skill effects with probability-based outcomes.
 * Uses TabooLib-style extension functions for clean, readable code.
 */
class PassiveSkillListener : Listener {

    // ===== CHEF_BUTCHER: 庖丁遗风 + bone drop =====

    private val livestockTypes = setOf(
        EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
        EntityType.RABBIT, EntityType.GOAT, EntityType.HOGLIN, EntityType.MOOSHROOM
    )
    private val rawMeat = mapOf(
        EntityType.COW to Material.BEEF, EntityType.PIG to Material.PORKCHOP,
        EntityType.SHEEP to Material.MUTTON, EntityType.CHICKEN to Material.CHICKEN,
        EntityType.RABBIT to Material.RABBIT, EntityType.GOAT to Material.MUTTON,
        EntityType.HOGLIN to Material.PORKCHOP, EntityType.MOOSHROOM to Material.BEEF
    )
    private val cookedMeat = mapOf(
        EntityType.COW to Material.COOKED_BEEF, EntityType.PIG to Material.COOKED_PORKCHOP,
        EntityType.SHEEP to Material.COOKED_MUTTON, EntityType.CHICKEN to Material.COOKED_CHICKEN,
        EntityType.RABBIT to Material.COOKED_RABBIT, EntityType.GOAT to Material.COOKED_MUTTON,
        EntityType.HOGLIN to Material.COOKED_PORKCHOP, EntityType.MOOSHROOM to Material.COOKED_BEEF
    )

    // ===== 斩首 (Warrior Hunter eureka 0): head drops (independent handler — NOT gated by livestock check) =====
    @EventHandler
    fun onDecapitation(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        if (!killer.hasBranch(Branch.WARRIOR_HUNTER) || !killer.hasEureka(Branch.WARRIOR_HUNTER, 0)) return
        val heads = mapOf(
            EntityType.WITHER_SKELETON to Pair(5, Material.WITHER_SKELETON_SKULL),
            EntityType.SKELETON to Pair(2, Material.SKELETON_SKULL),
            EntityType.ZOMBIE to Pair(2, Material.ZOMBIE_HEAD),
            EntityType.CREEPER to Pair(2, Material.CREEPER_HEAD),
            EntityType.PIGLIN to Pair(1, Material.PIGLIN_HEAD),
            EntityType.PIGLIN_BRUTE to Pair(1, Material.PIGLIN_HEAD),
            EntityType.ENDER_DRAGON to Pair(5, Material.DRAGON_HEAD)
        )
        heads[event.entityType]?.let { (chance, headMat) ->
            if (killer.roll(chance, "斩首·${event.entityType}"))
                event.entity.world.dropItemNaturally(event.entity.location, ItemStack(headMat, 1))
        }
    }

    // ===== CHEF_BUTCHER: livestock drops =====
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        if (event.entityType !in livestockTypes) return
        killer.career() ?: return
        if (!killer.hasBranch(Branch.CHEF_BUTCHER)) return

        // Base: bone
        event.entity.world.dropItemNaturally(event.entity.location, ItemStack(Material.BONE, 1))

        // Skill 0: extra meat
        val level = killer.skillLevel(Branch.CHEF_BUTCHER, 0)
        if (level >= 1 && event.entity.fireTicks <= 0) {
            val mat = if (level >= 3) cookedMeat[event.entityType] else rawMeat[event.entityType]
            mat?.let { event.entity.world.dropItemNaturally(event.entity.location, ItemStack(it, 1)) }
        }

        // Eureka 2 (追猎): check kill reward
        com.waterful.project.career.skill.EurekaEffectHandler.onPursuitKill(event.entity, killer)
    }

    // ===== CHEF_MASTER: food effects + Skill 1 (奇味异珍) + Skill 2 (镀金美馔) =====

    @EventHandler
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.CHEF_MASTER)) return
        val food = event.item.type

        // Base passive: +1 food level for any edible
        if (food.isEdible) p.foodLevel = (p.foodLevel + 1).coerceAtMost(20)
        if (food == Material.GOLDEN_APPLE || food == Material.ENCHANTED_GOLDEN_APPLE) {
            p.foodLevel = 20; p.saturation = 20f
        }

        // ---- Skill 1: 奇味异珍 (peek first, consume only on matching food) ----
        val qiWeiLv = com.waterful.project.career.skill.SkillExecutor.peekQiWeiYiZhen(p)
        if (qiWeiLv > 0) {
            var consumed = false
            when (food) {
                Material.CHORUS_FRUIT -> {
                    consumed = true
                    p.foodLevel = (p.foodLevel + 4).coerceAtMost(20)
                    val dir = p.location.direction.normalize()
                    val dist = 3 + (0..5).random()
                    val target = p.location.clone().add(dir.multiply(dist.toDouble()))
                    target.y = target.y.coerceIn(p.world.minHeight.toDouble(), p.world.maxHeight.toDouble() - 1)
                    if (target.block.type.isSolid) target.y = p.location.y
                    p.teleport(target)
                    p.sendMessage("§a✦ 奇味异珍·紫颂果：恢复4饥饿 + 传送")
                }
                Material.GOLDEN_CARROT -> {
                    consumed = true
                    p.foodLevel = (p.foodLevel + 2).coerceAtMost(20)
                    for (nearby in p.location.getNearbyPlayers(4.0)) {
                        if (nearby != p) nearby.foodLevel = (nearby.foodLevel + 2).coerceAtMost(20)
                    }
                    p.sendMessage("§a✦ 奇味异珍·金胡萝卜：恢复2饥饿（周围4格分享）")
                }
                Material.GLOW_BERRIES -> {
                    if (qiWeiLv >= 2) {
                        consumed = true
                        p.foodLevel = (p.foodLevel + 2).coerceAtMost(20)
                        p.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 30 * 20, 0, false, true))
                        p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 30 * 20, 1, false, true))
                        p.sendMessage("§a✦ 奇味异珍·发光浆果：恢复2饥饿 + 发光 + 速度II 30秒")
                    }
                }
                Material.POISONOUS_POTATO -> {
                    if (qiWeiLv >= 3) {
                        consumed = true
                        p.foodLevel = (p.foodLevel + 2).coerceAtMost(20)
                        p.removePotionEffect(PotionEffectType.POISON)
                        p.inventory.addItem(org.bukkit.inventory.ItemStack(Material.POTATO, 3))
                        p.sendMessage("§a✦ 奇味异珍·毒马铃薯：恢复2饥饿 + 解毒 + 3个马铃薯")
                    }
                }
                else -> {} // Not a matching food → flag stays active
            }
            if (consumed) com.waterful.project.career.skill.SkillExecutor.consumeQiWeiYiZhen(p)
        }

        // ---- Skill 2: 镀金美馔 (peek first, consume only on golden food) ----
        val duJinLv = com.waterful.project.career.skill.SkillExecutor.peekDuJinMeiZhuan(p)
        if (duJinLv > 0) {
            val isGoldenFood = food == Material.GOLDEN_CARROT || food == Material.GOLDEN_APPLE || food == Material.ENCHANTED_GOLDEN_APPLE
            if (isGoldenFood) {
                val nonHostile = { pl: org.bukkit.entity.Player -> pl.gameMode != org.bukkit.GameMode.SPECTATOR }
                when (food) {
                    Material.GOLDEN_CARROT -> {
                        for (nearby in p.location.getNearbyPlayers(4.0)) {
                            if (nonHostile(nearby)) nearby.foodLevel = (nearby.foodLevel + 2).coerceAtMost(20)
                        }
                        p.sendMessage("§a✦ 镀金美馔·金胡萝卜：周围玩家恢复2饥饿")
                    }
                    Material.GOLDEN_APPLE -> {
                        if (duJinLv >= 2) {
                            for (nearby in p.location.getNearbyPlayers(4.0)) {
                                if (nonHostile(nearby)) {
                                    nearby.foodLevel = (nearby.foodLevel + 4).coerceAtMost(20)
                                    nearby.health = (nearby.health + 2.0).coerceAtMost(nearby.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0)
                                }
                            }
                            p.sendMessage("§a✦ 镀金美馔·金苹果：周围玩家恢复4饥饿 + 2生命")
                        }
                    }
                    Material.ENCHANTED_GOLDEN_APPLE -> {
                        if (duJinLv >= 3) {
                            val range = 8.0
                            for (nearby in p.location.getNearbyPlayers(range)) {
                                if (nonHostile(nearby)) {
                                    nearby.foodLevel = (nearby.foodLevel + 6).coerceAtMost(20)
                                    nearby.health = (nearby.health + 8.0).coerceAtMost(nearby.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0)
                                    nearby.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30 * 20, 0, false, true))
                                    nearby.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 30 * 20, 0, false, true))
                                }
                            }
                            p.sendMessage("§a✦ 镀金美馔·附魔金苹果：周围8格玩家恢复6饥饿 + 8生命 + 30秒抗火/抗性I")
                        }
                    }
                    else -> {}
                }
                com.waterful.project.career.skill.SkillExecutor.consumeDuJinMeiZhuan(p)
            }
        }
    }

    // ===== CHEF_MASTER skill 0 + WORKER_SMELTER skill 0: furnace extraction =====

    private val cookedFoods = setOf(
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
        Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.COOKED_SALMON, Material.BAKED_POTATO, Material.DRIED_KELP
    )

    @EventHandler
    fun onFurnaceExtract(event: FurnaceExtractEvent) {
        val p = event.player; val mat = event.itemType

        // 精准火候 (Chef Master): food duplicate
        if (p.hasBranch(Branch.CHEF_MASTER) && mat in cookedFoods) {
            val lv = p.skillLevel(Branch.CHEF_MASTER, 0)
            if (lv >= 1 && p.roll(lv * 10, "精准火候·Lv.$lv ${mat.name}")) {
                p.world.dropItemNaturally(p.location, ItemStack(mat, event.itemAmount))
            }
        }

        // 熔炉改良 (Worker Smelter): extra product — roll per item extracted
        if (p.hasBranch(Branch.WORKER_SMELTER)) {
            val lv = p.skillLevel(Branch.WORKER_SMELTER, 0)
            if (lv >= 1) {
                var extraCount = 0
                repeat(event.itemAmount) {
                    if (p.roll(5 + lv * 5, "熔炉改良·Lv.$lv ${mat.name}"))
                        extraCount++
                }
                if (extraCount > 0)
                    p.world.dropItemNaturally(p.location, ItemStack(mat, extraCount))
            }
        }

        // 回炉重铸 (Worker Smelter eureka 1): blast furnace nugget bonus
        if (p.hasEureka(Branch.WORKER_SMELTER, 1) &&
            (mat == Material.IRON_NUGGET || mat == Material.GOLD_NUGGET) &&
            event.block.type == Material.BLAST_FURNACE) {
            p.world.dropItemNaturally(p.location, ItemStack(mat, 8))
            if (com.waterful.project.career.manager.DebugManager.isListening(p.uniqueId))
                p.sendMessage("§7[Debug] 回炉重铸：额外+8 ${mat.name}")
        }
    }

    // ===== ARCHITECT_FORTRESS skill 0 Lv.3: fall damage immunity =====

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.entity !is Player || event.cause != EntityDamageEvent.DamageCause.FALL) return
        val p = event.entity as Player
        if (!p.hasBranch(Branch.ARCHITECT_FORTRESS)) return
        if (p.skillLevel(Branch.ARCHITECT_FORTRESS, 0) != 3) return
        if (p.roll(20, "缓冲装置·Lv.3 摔落免伤")) event.isCancelled = true
    }

    // ===== SCHOLAR_ENCHANTER skill 0: enchant cost reduction =====

    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        val p = event.enchanter
        if (!p.hasBranch(Branch.SCHOLAR_ENCHANTER)) return
        val lv = p.skillLevel(Branch.SCHOLAR_ENCHANTER, 0)
        if (lv >= 1 && p.roll(lv * 20, "注魔宝典·Lv.$lv 经验减免")) {
            event.expLevelCost = (event.expLevelCost - 3).coerceAtLeast(1)
        }
    }

    // ===== SCHOLAR_REDSTONE skill 0: redstone place refund =====

    @EventHandler
    fun onPlaceRedstone(event: BlockPlaceEvent) {
        if (event.block.type != Material.REDSTONE_WIRE && event.block.type != Material.REDSTONE) return
        val p = event.player
        if (!p.hasBranch(Branch.SCHOLAR_REDSTONE)) return
        val lv = p.skillLevel(Branch.SCHOLAR_REDSTONE, 0)
        if (lv >= 1 && p.roll(2 + lv * 2, "电路设计·Lv.$lv 不消耗红石粉")) {
            val held = p.inventory.itemInMainHand
            if (held.type == Material.REDSTONE) held.amount = (held.amount + 1).coerceAtMost(64)
            else p.inventory.addItem(ItemStack(Material.REDSTONE, 1))
        }
    }

    // ===== WORKER_TOOLMAKER skill 0: smithing netherite refund =====

    @EventHandler
    fun onSmithingClick(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.SMITHING) return
        if (event.slotType != InventoryType.SlotType.RESULT) return
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.WORKER_TOOLMAKER)) return
        val lv = p.skillLevel(Branch.WORKER_TOOLMAKER, 0)
        if (lv < 1) return
        if (p.roll(5 + lv * 5, "镀层工艺·Lv.$lv 不消耗下界合金锭")) {
            val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe")
            if (plugin != null) {
                p.server.scheduler.runTask(plugin, Runnable {
                    p.inventory.addItem(ItemStack(Material.NETHERITE_INGOT, 1)).values.forEach {
                        p.world.dropItemNaturally(p.location, it)
                    }
                })
            }
        }
    }

    // ===== FARMER_MERCHANT skill 0: villager trade emerald return =====

    @EventHandler
    fun onTradeCompleted(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.MERCHANT || event.slot != 2) return
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.FARMER_MERCHANT)) return

        // Get the trade recipe through the merchant inventory
        val merchant = (event.inventory as? org.bukkit.inventory.MerchantInventory)?.merchant ?: return
        val recipe = merchant.getRecipe(0) ?: return
        val emeraldCost = recipe.ingredients.filter { it.type == Material.EMERALD }.sumOf { it.amount }
        if (emeraldCost <= 0) return

        DebugManager.logState(p, "商业谈判 — 检测到交易: 花费${emeraldCost}绿宝石 (${recipe.result.type.name})")
        val lv = p.skillLevel(Branch.FARMER_MERCHANT, 0)
        if (lv >= 1 && p.roll(10 + lv * 5, "商业谈判·Lv.$lv 绿宝石返还(花费${emeraldCost}颗)")) {
            val refund = (emeraldCost * (20 + lv * 10) / 100).coerceAtLeast(1)
            p.giveItem(ItemStack(Material.EMERALD, refund))
            p.msgSuccess("商业谈判生效，返还了 $refund 颗绿宝石！")
        }
        // 投资远见 (Eureka 2): track emerald spending
        com.waterful.project.career.skill.EurekaEffectHandler.onInvestmentTrade(p, emeraldCost)
    }

    // ===== WORKER_LUMBERJACK eureka 2: nether stem extra =====

    @EventHandler
    fun onBreakStem(event: BlockBreakEvent) {
        val p = event.player
        if (p.world.environment != World.Environment.NETHER) return
        if (event.block.type != Material.CRIMSON_STEM && event.block.type != Material.WARPED_STEM) return
        if (!p.hasBranch(Branch.WORKER_LUMBERJACK)) return
        if (!p.hasEureka(Branch.WORKER_LUMBERJACK, 2)) return // eureka index 2 = 抽丝剥茧
        if (p.roll(33, "抽丝剥茧 ${event.block.type.name}")) {
            event.block.world.dropItemNaturally(event.block.location, ItemStack(event.block.type, 1))
        }
    }

    // ===== CHEF_BAKER skill 0: bake success rate + Skill 1: 文火慢炖 =====

    private val bakedGoods = setOf(Material.BREAD, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE)
    private val stews = setOf(Material.MUSHROOM_STEW, Material.BEETROOT_SOUP, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW)

    @EventHandler
    fun onCraftBakedGoods(event: CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.CHEF_BAKER)) return
        val result = event.recipe?.result ?: return
        val resultType = result.type
        val isBakerItem = resultType in bakedGoods || resultType in stews

        // ---- Skill 0: 预热烤箱 (bake success rate, per output item) ----
        if (isBakerItem) {
            val ovenLv = p.skillLevel(Branch.CHEF_BAKER, 0)
            val outputAmount = result.amount
            // Lv.1: 90% success. Judge each output item separately
            if (ovenLv == 1) {
                var successCount = 0
                repeat(outputAmount) {
                    if (p.roll(90, "预热烤箱·Lv.1 ${resultType.name}")) successCount++
                }
                if (successCount == 0) {
                    event.isCancelled = true
                    val inv = event.inventory as? org.bukkit.inventory.CraftingInventory
                    inv?.matrix?.forEach { it?.amount = (it.amount - 1).coerceAtLeast(0) }
                    p.msgFail("合成失败！预热烤箱技能未触发成功（原料已消耗）。")
                    return
                }
                if (successCount < outputAmount) {
                    // Partial success: cancel event, manually give successful items
                    event.isCancelled = true
                    val inv = event.inventory as? org.bukkit.inventory.CraftingInventory
                    inv?.matrix?.forEach { it?.amount = (it.amount - 1).coerceAtLeast(0) }
                    p.inventory.addItem(org.bukkit.inventory.ItemStack(resultType, successCount))
                    p.sendMessage("§e预热烤箱·Lv.1：${outputAmount}个中合成了${successCount}个${resultType.name}")
                    return
                }
            }
            // Lv.2+ guaranteed, Lv.0 no baker skill → normal vanilla behavior
        }

        // ---- Skill 1: 文火慢炖 (extra stew on next craft) ----
        if (resultType in stews) {
            val stewLv = com.waterful.project.career.skill.SkillExecutor.onWenHuoManDunCraft(p)
            if (stewLv > 0) {
                val extra = when {
                    stewLv >= 3 && resultType == Material.SUSPICIOUS_STEW -> {
                        // 2 random suspicious stews
                        repeat(2) { p.inventory.addItem(org.bukkit.inventory.ItemStack(Material.SUSPICIOUS_STEW, 1)) }
                        "2个随机谜之炖菜"
                    }
                    else -> {
                        val randomStew = stews.filter { it != Material.SUSPICIOUS_STEW }.random()
                        p.inventory.addItem(org.bukkit.inventory.ItemStack(randomStew, 1))
                        "1个额外汤煲"
                    }
                }
                p.sendMessage("§a✦ 文火慢炖：额外获得$extra")
            }
        }
    }

    // ===== CHEF_BAKER eureka 0: bread attack (法棍) =====

    @EventHandler
    fun onBreadAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.holding(Material.BREAD)) return
        if (!damager.hasBranch(Branch.CHEF_BAKER) || !damager.hasEureka(Branch.CHEF_BAKER, 0)) return
        val target = event.entity as? LivingEntity ?: return
        // Always apply: 15s nausea II + 5s darkness
        target.effect(PotionEffectType.NAUSEA, 15, 1)   // amp 1 = Nausea II
        target.effect(PotionEffectType.DARKNESS, 5, 0)   // amp 0 = Darkness I
        // 20% chance: 2 true damage (bypasses armor)
        if (damager.roll(20, "法棍 — 真实伤害(20%)")) {
            target.realDamage(2.0, damager)
        }
    }

    // ===== CHEF_BAKER eureka 2: lucky cookie =====

    @EventHandler
    fun onEatCookie(event: PlayerItemConsumeEvent) {
        val p = event.player
        if (event.item.type != Material.COOKIE) return
        if (!p.hasBranch(Branch.CHEF_BAKER) || !p.hasEureka(Branch.CHEF_BAKER, 2)) return
        if (p.roll(20, "幸运曲奇 — 幸运I(20%)"))
            p.effect(PotionEffectType.LUCK, 30, 0)
        if (p.roll(5, "幸运曲奇 — 幸运II(5%)"))
            p.effect(PotionEffectType.LUCK, 30, 1)
        if (DebugManager.rollChanceFraction(p, 1, 1000, "幸运曲奇 — 钻石(0.1%)"))
            p.giveItem(ItemStack(Material.DIAMOND, 1))
    }

    // ===== SCHOLAR_REDSTONE eureka 0: redstone torch ignite =====

    @EventHandler
    fun onRedstoneTorchAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.holding(Material.REDSTONE_TORCH)) return
        if (!damager.hasBranch(Branch.SCHOLAR_REDSTONE) || !damager.hasEureka(Branch.SCHOLAR_REDSTONE, 0)) return
        if (damager.roll(25, "红色炬火 — 点燃")) event.entity.fireTicks = 20 * 5
    }

    // ===== SCHOLAR_REDSTONE eureka 2: lightning rod =====

    @EventHandler
    fun onLightningRodUse(event: PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val p = event.player
        if (!p.holding(Material.LIGHTNING_ROD)) return
        if (!p.hasBranch(Branch.SCHOLAR_REDSTONE) || !p.hasEureka(Branch.SCHOLAR_REDSTONE, 2)) return

        val world = p.world
        val (chance, weather) = when {
            world.isThundering -> 33 to "雷暴"
            world.hasStorm() -> 20 to "降雨"
            else -> 10 to "晴朗"
        }
        DebugManager.logState(p, "雷霆之杖 — 天气:$weather 概率:$chance%")
        if (!p.roll(chance, "雷霆之杖($weather$chance%)")) {
            val held = p.inventory.itemInMainHand
            if (held.type == Material.LIGHTNING_ROD) held.amount -= 1
            return
        }
        val target = p.getTargetBlockExact(50)?.location ?: p.location
        world.strikeLightning(target)
    }

    // ===== WORKER_TOOLMAKER skill 2: durability enchant on diamond tool =====

    private val diamondTools = setOf(
        Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
        Material.DIAMOND_HOE, Material.DIAMOND_SWORD
    )

    @EventHandler
    fun onCraftDiamondTool(event: CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.WORKER_TOOLMAKER)) return
        val lv = p.skillLevel(Branch.WORKER_TOOLMAKER, 2)
        if (lv < 1) return
        val result = event.recipe?.result ?: return
        if (result.type !in diamondTools) return

        val enchLv = when (lv) {
            1 -> 1
            2 -> if (p.roll(25, "关键结构加固·Lv.2 耐久II(25%)")) 2 else 1
            3 -> when {
                p.roll(5, "关键结构加固·Lv.3 耐久III(5%)") -> 3
                p.roll(40, "关键结构加固·Lv.3 耐久II(40%)") -> 2
                else -> 1
            }
            else -> 1
        }
        event.inventory.result?.editMeta {
            it.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, enchLv, true)
        }
    }

    // ===== SCHOLAR_ENCHANTER skill 1: grindstone durability =====

    @EventHandler
    fun onGrindstoneUse(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.GRINDSTONE || event.slot != 2) return
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.SCHOLAR_ENCHANTER)) return
        val lv = p.skillLevel(Branch.SCHOLAR_ENCHANTER, 1)
        if (lv >= 1 && p.roll(lv * 10, "魔力虹吸·Lv.$lv 耐久回复")) p.giveExp(5)
    }

    // ===== FARMER_RANCHER skill 0: breed spawn egg =====

    @EventHandler
    fun onAnimalBreedForEgg(event: org.bukkit.event.entity.EntityBreedEvent) {
        val breeder = event.breeder as? Player ?: return
        if (!breeder.hasBranch(Branch.FARMER_RANCHER)) return
        val lv = breeder.skillLevel(Branch.FARMER_RANCHER, 0)
        if (lv < 1) return

        val chance = when (lv) { 1 -> 1; 2 -> 1; 3 -> 2; else -> 0 }
        if (!breeder.roll(chance, "动物育种·Lv.$lv 刷怪蛋(${if(lv==1)"~0.5" else if(lv==2)"1" else "2"}%)")) return

        val eggMat = when (event.entityType) {
            EntityType.COW -> Material.COW_SPAWN_EGG
            EntityType.PIG -> Material.PIG_SPAWN_EGG
            EntityType.SHEEP -> Material.SHEEP_SPAWN_EGG
            EntityType.CHICKEN -> Material.CHICKEN_SPAWN_EGG
            EntityType.RABBIT -> Material.RABBIT_SPAWN_EGG
            EntityType.GOAT -> Material.GOAT_SPAWN_EGG
            EntityType.HOGLIN -> Material.HOGLIN_SPAWN_EGG
            EntityType.MOOSHROOM -> Material.MOOSHROOM_SPAWN_EGG
            else -> null
        }
        eggMat?.let { event.entity.world.dropItemNaturally(event.entity.location, ItemStack(it, 1)) }
    }

    // ===== WORKER_LUMBERJACK skill 0 (全力劈砍): haste on wood break =====

    private val woodBlocks = setOf(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
        Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.CRIMSON_STEM, Material.WARPED_STEM,
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
        Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE,
        Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
        Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM,
        Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD,
        Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
        Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE,
        Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
        Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
        Material.CRIMSON_PLANKS, Material.WARPED_PLANKS, Material.BAMBOO_PLANKS,
        Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK
    )

    @EventHandler
    fun onBreakWood(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.WORKER_LUMBERJACK)) return
        if (event.block.type !in woodBlocks) return
        val lv = p.skillLevel(Branch.WORKER_LUMBERJACK, 0)
        val amp = if (lv < 1) 0 else lv // lvl.0 = Haste I (amp 0), lvl.1+ = Haste II-IV
        p.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 5 * 20, amp, false, true))
        // 巧力用斧 (强力运斧): prevent durability damage on next axe break
        if (com.waterful.project.career.skill.SkillExecutor.onQiaoLiYongFuBreak(p)) {
            val item = p.inventory.itemInMainHand
            if (item is org.bukkit.inventory.meta.Damageable && item.type.name.contains("AXE")) {
                val meta = item.itemMeta as org.bukkit.inventory.meta.Damageable
                val oldDmg = meta.damage
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe")!!,
                    Runnable {
                        val newMeta = item.itemMeta as org.bukkit.inventory.meta.Damageable
                        newMeta.damage = oldDmg
                        item.itemMeta = newMeta
                    }, 1L)
            }
        }
    }

    // ===== WORKER_MINER skill 0 (奋力挖掘): haste on stone break =====

    private val stoneBlocks = setOf(
        Material.STONE, Material.DEEPSLATE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
        Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE,
        Material.SANDSTONE, Material.RED_SANDSTONE, Material.SMOOTH_SANDSTONE, Material.SMOOTH_RED_SANDSTONE,
        Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE, Material.END_STONE,
        Material.COBBLESTONE, Material.COBBLED_DEEPSLATE,
        Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
        // Ore variants
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS, Material.GILDED_BLACKSTONE,
        // Additional stone variants
        Material.PRISMARINE, Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS,
        Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
        Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
        Material.BRICKS, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
        Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
        Material.DEEPSLATE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS,
        Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_TILES,
        Material.POLISHED_GRANITE, Material.POLISHED_DIORITE, Material.POLISHED_ANDESITE,
        Material.POLISHED_DEEPSLATE, Material.POLISHED_BASALT,
        Material.SMOOTH_BASALT, Material.SMOOTH_STONE,
        Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
        Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
        Material.QUARTZ_BLOCK, Material.QUARTZ_BRICKS, Material.QUARTZ_PILLAR,
        Material.SMOOTH_QUARTZ, Material.CHISELED_QUARTZ_BLOCK,
        Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST,
        Material.MAGMA_BLOCK, Material.GLOWSTONE,
        Material.TERRACOTTA, Material.WHITE_TERRACOTTA
    )

    @EventHandler
    fun onBreakStone(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.WORKER_MINER)) return
        if (event.block.type !in stoneBlocks) return
        // Haste buff
        val lv = p.skillLevel(Branch.WORKER_MINER, 0)
        val amp = if (lv < 1) 0 else lv // lvl.0 = Haste I (amp 0), lvl.1+ = Haste II-IV
        p.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 5 * 20, amp, false, true))
        // 强力运镐: prevent durability damage on next pickaxe break
        if (com.waterful.project.career.skill.SkillExecutor.onQiangLiYunGaoBreak(p)) {
            val item = p.inventory.itemInMainHand
            if (item is org.bukkit.inventory.meta.Damageable && item.type.name.contains("PICKAXE")) {
                val meta = item.itemMeta as org.bukkit.inventory.meta.Damageable
                val oldDmg = meta.damage
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe")!!,
                    Runnable {
                        val newMeta = item.itemMeta as org.bukkit.inventory.meta.Damageable
                        newMeta.damage = oldDmg
                        item.itemMeta = newMeta
                    }, 1L)
            }
        }
    }

    // ===== WARRIOR_WEAPON skill 0 (以锋为御): sword damage reduction =====

    @EventHandler
    fun onWeaponDefense(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        if (!victim.hasBranch(Branch.WARRIOR_WEAPON)) return
        val lv = victim.skillLevel(Branch.WARRIOR_WEAPON, 0)
        if (lv < 1) return
        val held = victim.inventory.itemInMainHand
        if (!held.type.name.contains("SWORD")) return
        val reduction = when (lv) { 1 -> 0.08; 2 -> 0.16; 3 -> 0.25; else -> 0.0 }
        event.damage = event.damage * (1.0 - reduction)
    }

    // ===== WARRIOR_HUNTER skill 0 (除魅使命): elite monster damage bonus =====

    private val eliteMobs = setOf(
        EntityType.IRON_GOLEM, EntityType.ELDER_GUARDIAN, EntityType.RAVAGER,
        EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.WITHER
    )

    @EventHandler
    fun onHunterDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.hasBranch(Branch.WARRIOR_HUNTER)) return
        val lv = damager.skillLevel(Branch.WARRIOR_HUNTER, 0)
        if (lv < 1 || event.entityType !in eliteMobs) return
        val bonus = when (lv) { 1 -> 0.40; 2 -> 0.50; 3 -> 0.60; else -> 0.0 }
        event.damage = event.damage * (1.0 + bonus)
    }

    // ===== WARRIOR_SOLDIER skill 0 (军事训练): vs-player damage bonus + base 10% =====

    @EventHandler
    fun onSoldierBonus(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (event.entity !is Player) return // Only vs players
        if (!damager.hasBranch(Branch.WARRIOR_SOLDIER)) return
        // Base +10% always, skill adds +20/30/40%
        val baseBonus = 0.10
        val lv = damager.skillLevel(Branch.WARRIOR_SOLDIER, 0)
        val skillBonus = when (lv) { 1 -> 0.20; 2 -> 0.30; 3 -> 0.40; else -> 0.0 }
        event.damage = event.damage * (1.0 + baseBonus + skillBonus)
    }

    // ===== WARRIOR_SOLDIER base: low HP resistance =====

    @EventHandler
    fun onSoldierLowHp(event: org.bukkit.event.entity.EntityDamageEvent) {
        val p = event.entity as? Player ?: return
        if (!p.hasBranch(Branch.WARRIOR_SOLDIER)) return
        if (p.health <= 10.0) {
            p.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 0, false, true))
        }
    }

    // ===== FARMER_BOTANIST skill 0 (合理密植): crop harvest bonus (lvl.0 = 10%) =====

    private val crops = setOf(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.COCOA, Material.NETHER_WART
    )

    @EventHandler
    fun onHarvestCrop(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.FARMER_BOTANIST)) return
        if (event.block.type !in crops) return
        val lv = p.skillLevel(Branch.FARMER_BOTANIST, 0)
        // lvl.0: base 10%. lvl.1: 15%. lvl.2: 20%. lvl.3: 25%
        val chance = when (lv) { 0 -> 10; 1 -> 15; 2 -> 20; 3 -> 25; else -> 10 }
        if (p.roll(chance, "合理密植·Lv.$lv 额外收获($chance%)")) {
            val drops = event.block.getDrops(p.inventory.itemInMainHand, p)
            drops.forEach { drop -> event.block.world.dropItemNaturally(event.block.location, drop) }
        }
    }

    // ===== FARMER_BOTANIST skill 1 (科学施肥): bone meal save chance =====

    @EventHandler
    fun onBoneMealUse(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val p = event.player
        if (p.inventory.itemInMainHand.type != Material.BONE_MEAL) return
        val chance = com.waterful.project.career.skill.SkillExecutor.getKeXueShiFeiChance(p)
        if (chance <= 0) return
        if (p.roll(chance, "科学施肥·骨粉不消耗($chance%)")) {
            // Refund the bone meal after use
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe")!!,
                Runnable { p.inventory.addItem(org.bukkit.inventory.ItemStack(Material.BONE_MEAL, 1)) },
                1L
            )
        }
    }

    // ===== FARMER_BOTANIST eureka 0 (于荫求果): 10% apple from leaves =====

    private val leaves = Material.entries.filter { it.name.contains("LEAVES") }.toSet()

    @EventHandler
    fun onBreakLeaves(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.FARMER_BOTANIST, 0)) return
        if (event.block.type !in leaves) return
        if (!p.roll(10, "于荫求果·苹果")) return
        event.block.world.dropItemNaturally(event.block.location, org.bukkit.inventory.ItemStack(Material.APPLE, 1))
    }

    // ===== FARMER_BOTANIST eureka 1 (终极奉献): broken netherite hoe → new one =====

    @EventHandler
    fun onHoeBreak(event: org.bukkit.event.player.PlayerItemBreakEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.FARMER_BOTANIST, 1)) return
        if (event.brokenItem.type != Material.NETHERITE_HOE) return
        p.inventory.addItem(org.bukkit.inventory.ItemStack(Material.NETHERITE_HOE, 1))
        p.sendMessage("§a✦ 终极奉献：获得一把新的下界合金锄")
    }

    // ===== FARMER_RANCHER eureka 0 (游牧习俗): plains + milk → instant health II =====

    @EventHandler
    fun onMilkDrink(event: PlayerItemConsumeEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.FARMER_RANCHER, 0)) return
        if (event.item.type != Material.MILK_BUCKET) return
        val biomeName = p.location.block.biome.toString().uppercase()
        if (!biomeName.contains("PLAINS")) return
        p.addPotionEffect(PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true))
        p.sendMessage("§a✦ 游牧习俗：平原饮用奶获得瞬间治疗II")
    }

    // ===== FARMER_RANCHER eureka 2 (克隆技术): break spawner → spawn eggs =====

    @EventHandler
    fun onBreakSpawner(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.FARMER_RANCHER, 2)) return
        if (event.block.type != Material.SPAWNER) return
        val spawner = event.block.state as? org.bukkit.block.CreatureSpawner ?: return
        val spawnedType = spawner.spawnedType ?: return
        val eggMaterial = Material.entries.find { it.name == "${spawnedType.name}_SPAWN_EGG" }
            ?: return
        val count = (1..3).random()
        event.block.world.dropItemNaturally(event.block.location, org.bukkit.inventory.ItemStack(eggMaterial, count))
        p.sendMessage("§a✦ 克隆技术：获得${count}个${spawnedType.name}刷怪蛋")
    }

    // ===== ARCHITECT_STRUCTURE skill 1 (识色敏锐): dyed block crafting bonus with CD =====

    private val dyedBlockResults: Set<Material> by lazy {
        Material.entries.filter { mat ->
            mat.isBlock && (mat.name.contains("CONCRETE") || mat.name.contains("TERRACOTTA") ||
                mat.name.contains("WOOL") || mat.name.contains("STAINED_GLASS") ||
                mat.name.contains("GLAZED") || mat.name.contains("CANDLE") ||
                mat.name.endsWith("_BED") || mat.name.contains("SHULKER_BOX") ||
                mat.name.endsWith("_BANNER") || mat.name.endsWith("_CARPET"))
        }.toSet()
    }

    @EventHandler
    fun onCraftDye(event: org.bukkit.event.inventory.CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.ARCHITECT_STRUCTURE)) return
        val lv = p.skillLevel(Branch.ARCHITECT_STRUCTURE, 1)
        if (lv < 1) return
        val result = event.recipe?.result ?: return
        if (result.type !in dyedBlockResults) return

        DebugManager.logState(p, "识色敏锐 — 合成${result.type.name}")
        val cp = p.career() ?: return
        val now = System.currentTimeMillis()
        val cdSeconds = com.waterful.project.career.data.CareerDataLoader.getSkill("architect_structure_skill_1")?.getCooldown(lv) ?: (24 - lv * 5) // [20,15,10]
        if (cp.isOnCooldown("architect_structure_skill_1", cdSeconds, now)) {
            DebugManager.logState(p, "识色敏锐 — CD冷却中(${cp.getRemainingCooldown("architect_structure_skill_1", cdSeconds, now)}秒)")
            return
        }
        val bonus = when (lv) { 1 -> 1; 2 -> 1; 3 -> 2; else -> 1 }
        p.inventory.addItem(ItemStack(result.type, bonus))
        cp.setCooldown("architect_structure_skill_1", now)
        DebugManager.logState(p, "识色敏锐 — 获得${bonus}个${result.type.name}")
    }

    // ===== SCHOLAR_REDSTONE skill 1 (自动化生产): redstone craft bonus (PASSIVE with CD) =====

    private val redstoneCrafts = setOf(
        Material.REDSTONE, Material.REPEATER, Material.COMPARATOR,
        Material.PISTON, Material.STICKY_PISTON, Material.OBSERVER,
        Material.DROPPER, Material.DISPENSER, Material.HOPPER,
        Material.REDSTONE_TORCH, Material.REDSTONE_BLOCK, Material.DAYLIGHT_DETECTOR
    )

    @EventHandler
    fun onCraftRedstone(event: org.bukkit.event.inventory.CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.SCHOLAR_REDSTONE)) return
        val lv = p.skillLevel(Branch.SCHOLAR_REDSTONE, 1)
        if (lv < 1) return
        val result = event.recipe?.result ?: return
        if (result.type !in redstoneCrafts) return

        val cp = p.career() ?: return
        val now = System.currentTimeMillis()
        val cdSeconds = 240
        if (cp.isOnCooldown("scholar_redstone_skill_1", cdSeconds, now)) {
            DebugManager.logState(p, "自动化生产 — CD冷却中")
            return
        }

        DebugManager.logState(p, "自动化生产 — 合成${result.type.name}")
        val chance = lv * 10 // Lv.1=10%, Lv.2=20%, Lv.3=30%
        if (DebugManager.rollChance(p, chance, "自动化生产·Lv.$lv 额外获得${result.type.name}")) {
            p.inventory.addItem(ItemStack(result.type, 1))
            cp.setCooldown("scholar_redstone_skill_1", now)
        }
    }

    // ===== WORKER_TOOLMAKER skill 1 (熟能生巧): anvil use bonus (PASSIVE with CD) =====

    @EventHandler
    fun onAnvilUse(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.ANVIL) return
        if (event.slot != 2) return
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.WORKER_TOOLMAKER)) return
        val lv = p.skillLevel(Branch.WORKER_TOOLMAKER, 1)
        if (lv < 1) return

        DebugManager.logState(p, "熟能生巧 — 使用铁砧")
        val cp = p.career() ?: return
        val now = System.currentTimeMillis()
        val cdSeconds = 480
        if (cp.isOnCooldown("worker_toolmaker_skill_1", cdSeconds, now)) {
            DebugManager.logState(p, "熟能生巧 — CD冷却中")
            return
        }
        val exp = when (lv) { 1 -> 4; 2 -> 6; 3 -> 8; else -> 4 }
        p.giveExp(exp)
        cp.setCooldown("worker_toolmaker_skill_1", now)
        DebugManager.logState(p, "熟能生巧 — 获得${exp}经验等级")
    }

    // ===== WARRIOR WEAPON base: 弩25%不消耗普通箭 =====
    @EventHandler
    fun onBowShoot(event: org.bukkit.event.entity.EntityShootBowEvent) {
        val p = event.entity as? Player ?: return
        // 手摇TNT火炮: 箭变为TNT
        com.waterful.project.career.skill.EurekaEffectHandler.onBowShoot(p, event)
        if (event.isCancelled) return
        // 信号弹: 光灵箭不消耗 + 发光延长
        val ammo = event.arrowItem ?: return
        val projectile = event.projectile
        if (projectile is org.bukkit.entity.AbstractArrow) {
            SkillExecutor.onSignalFlareShoot(p, projectile, ammo)
        }
        // 武器专家: 弩箭返还
        if (event.bow?.type == Material.CROSSBOW && p.hasBranch(Branch.WARRIOR_WEAPON)) {
            if (ammo.type == Material.ARROW && p.roll(25, "武器专家 — 弩箭不消耗")) {
                p.inventory.addItem(ItemStack(ammo.type, 1))
            }
        }
    }

    // ===== 信号弹: 延长发光效果 =====
    @EventHandler
    fun onSignalFlareHit(event: org.bukkit.event.entity.ProjectileHitEvent) {
        SkillExecutor.onSignalFlareHit(event)
    }

    // ===== WARRIOR WEAPON eureka 0 (涌潮长戟): 三叉戟近战回血 =====
    @EventHandler
    fun onTridentMelee(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val p = event.damager as? Player ?: return
        if (p.inventory.itemInMainHand.type != Material.TRIDENT) return
        if (!p.hasBranch(Branch.WARRIOR_WEAPON)) return
        val has = p.career()?.chosenEurekas?.get(Branch.WARRIOR_WEAPON)?.eurekaDef?.id?.contains("weapon_eureka_0") == true
        if (!has) return
        p.health = (p.health + 2.0).coerceAtMost(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0)
    }

    // ===== WARRIOR WEAPON eureka 1 (穿甲箭头): 弩箭+1真伤 + 每级穿透+1 =====
    @EventHandler
    fun onPiercingArrow(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        if (event.cause != org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE) return
        val arrow = event.damager as? org.bukkit.entity.AbstractArrow ?: return
        val p = arrow.shooter as? Player ?: return
        if (!p.hasBranch(Branch.WARRIOR_WEAPON)) return
        val has = p.career()?.chosenEurekas?.get(Branch.WARRIOR_WEAPON)?.eurekaDef?.id?.contains("weapon_eureka_1") == true
        if (!has) return
        // Only crossbow arrows
        if (p.inventory.itemInMainHand.type != Material.CROSSBOW && p.inventory.itemInOffHand.type != Material.CROSSBOW) return
        val piercing = p.inventory.itemInMainHand.enchantments[org.bukkit.enchantments.Enchantment.PIERCING] ?: 0
        val bonusDamage = (1 + piercing).toDouble()
        val target = event.entity as? org.bukkit.entity.LivingEntity ?: return
        // Apply true damage as extra on the event (added after armor calc by MC)
        event.damage += bonusDamage
    }

    // ===== WARRIOR SOLDIER base: 全套盔甲受伤-2 =====
    @EventHandler
    fun onSoldierArmor(event: org.bukkit.event.entity.EntityDamageEvent) {
        val p = event.entity as? Player ?: return
        if (!p.hasBranch(Branch.WARRIOR_SOLDIER)) return
        if (p.inventory.armorContents.any { it == null || it.type == Material.AIR }) return
        event.damage = (event.damage - 2.0).coerceAtLeast(0.0)
    }

    // ===== WARRIOR SOLDIER eureka 0 (游击战): 受玩家攻击后清除发光+速度II =====
    @EventHandler
    fun onSoldierRetreat(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        if (event.damager !is Player) return
        if (!victim.hasBranch(Branch.WARRIOR_SOLDIER)) return
        val has = victim.career()?.chosenEurekas?.get(Branch.WARRIOR_SOLDIER)?.eurekaDef?.id?.contains("soldier_eureka_0") == true
        if (!has) return
        victim.removePotionEffect(PotionEffectType.GLOWING)
        victim.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 3 * 20, 1))
    }

    // ===== WARRIOR EXPLORER: base lvl.0 始终迅捷I + eureka_1 迅捷II + eureka_2 旷野互助 =====
    @EventHandler
    fun onExplorerJoin(event: org.bukkit.event.player.PlayerJoinEvent) { applyExplorerEffects(event.player) }
    @EventHandler
    fun onExplorerRespawn(event: org.bukkit.event.player.PlayerRespawnEvent) { applyExplorerEffects(event.player) }
    @EventHandler
    fun onExplorerMove(event: org.bukkit.event.player.PlayerMoveEvent) {
        // Reapply speed buff if missing or about to expire (handles death, milk, eureka unlock while online)
        val p = event.player
        if (!p.hasBranch(Branch.WARRIOR_EXPLORER)) return
        if (event.from.blockX == event.to?.blockX && event.from.blockZ == event.to?.blockZ) return
        val existing = p.getPotionEffect(PotionEffectType.SPEED)
        if (existing == null || existing.duration < 10 * 20) {
            applyExplorerEffects(p)
        }
    }

    private fun applyExplorerEffects(p: Player) {
        if (!p.hasBranch(Branch.WARRIOR_EXPLORER)) return
        val cp = p.career() ?: return
        val hasEureka1 = cp.chosenEurekas[Branch.WARRIOR_EXPLORER]?.eurekaDef?.id?.contains("explorer_eureka_1") == true
        val spdLvl = if (hasEureka1) 1 else 0 // Speed I (base) or Speed II (eureka)
        p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 72000 * 20, spdLvl, false, false, true))
    }

    // ===== TNT Cannon: prevent block damage from eureka-spawned TNT =====
    @EventHandler
    fun onTntExplode(event: org.bukkit.event.entity.EntityExplodeEvent) {
        if (com.waterful.project.career.skill.EurekaEffectHandler.isNoGriefTnt(event.entity)) {
            event.blockList().clear()
        }
    }

    // ===== WARRIOR EXPLORER eureka 2 (旷野互助): 平原群系给周围生物再生II =====
    @EventHandler
    fun onExplorerPlainsAura(event: org.bukkit.event.player.PlayerMoveEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.WARRIOR_EXPLORER)) return
        val has = p.career()?.chosenEurekas?.get(Branch.WARRIOR_EXPLORER)?.eurekaDef?.id?.contains("explorer_eureka_2") == true
        if (!has) return
        if (event.from.blockX == event.to?.blockX && event.from.blockZ == event.to?.blockZ) return
        if (!p.location.block.biome.toString().contains("PLAINS")) return
        for (entity in p.location.getNearbyEntities(6.0, 6.0, 6.0)) {
            if (entity is org.bukkit.entity.LivingEntity && entity != p) {
                entity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1, false, false))
            }
        }
    }

    // FARMER_FISHERMAN base effects: handled by StarLightRe.startFishermanTick (20tick repeating task)

    // ===== FARMER_FISHERMAN eureka 2 (凝望反制): guardian damage reduction (30s window) =====
    @EventHandler
    fun onGuardianDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val p = event.entity as? Player ?: return
        if (!p.hasBranch(Branch.FARMER_FISHERMAN)) return
        if (event.damager !is org.bukkit.entity.Guardian && event.damager !is org.bukkit.entity.ElderGuardian) return
        // Check if 凝望反制 is active (30s window after activation)
        if (!com.waterful.project.career.skill.EurekaEffectHandler.isGuardianDefenseActive(p.uniqueId)) return
        event.damage = maxOf(event.damage - 6.0, 0.1)
    }

    // ===== FARMER_MERCHANT base: 村庄中生命恢复I =====
    @EventHandler
    fun onMerchantMove(event: org.bukkit.event.player.PlayerMoveEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.FARMER_MERCHANT)) return
        if (event.from.blockX == event.to?.blockX && event.from.blockZ == event.to?.blockZ) return
        val inVillage = p.location.getNearbyEntities(32.0, 32.0, 32.0).any { it is org.bukkit.entity.Villager }
        if (inVillage) {
            p.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 0, false, false))
        }
    }

    // ===== ARCHITECT_STRUCTURE skill 0 (高效回收): decorative/dyed/glass block drops =====

    // Decorative blocks (Lv.1): bells, bookshelves, lanterns, barrels, item frames, etc.
    private val decorativeBlocks = setOf(
        Material.BELL, Material.CHISELED_BOOKSHELF, Material.FLETCHING_TABLE,
        Material.JUKEBOX, Material.LECTERN, Material.LODESTONE, Material.RESPAWN_ANCHOR,
        Material.END_ROD, Material.LOOM, Material.LANTERN, Material.SOUL_LANTERN,
        Material.BARREL, Material.ENDER_CHEST, Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX, Material.BOOKSHELF, Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME,
        Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER,
        Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER,
        Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER,
        Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
        Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER,
        Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET,
        Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET,
        Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET,
        Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
        Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET, Material.BLACK_CARPET,
        Material.SCAFFOLDING, Material.NOTE_BLOCK
    )
    // Dyed blocks (Lv.2): colored concrete, terracotta, wool, glass
    private val dyedBlocks: Set<Material> by lazy {
        Material.entries.filter { it.name.startsWith("WHITE_") || it.name.startsWith("ORANGE_") ||
            it.name.startsWith("MAGENTA_") || it.name.startsWith("LIGHT_BLUE_") ||
            it.name.startsWith("YELLOW_") || it.name.startsWith("LIME_") ||
            it.name.startsWith("PINK_") || it.name.startsWith("GRAY_") && !it.name.startsWith("GRAY_") == false ||
            it.name.startsWith("LIGHT_GRAY_") || it.name.startsWith("CYAN_") ||
            it.name.startsWith("PURPLE_") || it.name.startsWith("BLUE_") ||
            it.name.startsWith("BROWN_") || it.name.startsWith("GREEN_") ||
            it.name.startsWith("RED_") || it.name.startsWith("BLACK_")
        }.filter { it.name.contains("CONCRETE") || it.name.contains("TERRACOTTA") ||
            it.name.contains("WOOL") || it.name.contains("STAINED_GLASS") || it.name.contains("GLAZED") ||
            it.name.contains("CANDLE") || it.name.contains("BED") }.toSet()
    }
    // Glass blocks (Lv.3)
    private val glassBlocks = setOf(
        Material.GLASS, Material.GLASS_PANE, Material.TINTED_GLASS,
        Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS,
        Material.LIGHT_BLUE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
        Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS,
        Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
        Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.RED_STAINED_GLASS,
        Material.BLACK_STAINED_GLASS,
        Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
        Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE,
        Material.BLACK_STAINED_GLASS_PANE
    )

    @EventHandler
    fun onBreakStructureBlock(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.ARCHITECT_STRUCTURE)) return
        val lv = p.skillLevel(Branch.ARCHITECT_STRUCTURE, 0)
        if (lv < 1) return
        val block = event.block.type
        val eligible = (lv >= 1 && block in decorativeBlocks) ||
            (lv >= 2 && block in dyedBlocks) ||
            (lv >= 3 && block in glassBlocks)
        if (eligible) {
            event.isDropItems = true // Ensure drops happen (override other gating)
        }
    }

    // ===== ARCHITECT_STRUCTURE eureka 0 (奇珍颜料): 20% dye drop on dyed block break =====

    @EventHandler
    fun onRarePigment(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.ARCHITECT_STRUCTURE, 0)) return
        val block = event.block.type
        if (block !in dyedBlocks) return
        if (!p.roll(20, "奇珍颜料 ${block.name}")) return
        // Map dyed block to dye
        val dye = blockToDye(block) ?: return
        event.block.world.dropItemNaturally(event.block.location, org.bukkit.inventory.ItemStack(dye, 1))
    }

    private fun blockToDye(block: Material): Material? {
        val name = block.name
        val color = listOf("WHITE","ORANGE","MAGENTA","LIGHT_BLUE","YELLOW","LIME",
            "PINK","GRAY","LIGHT_GRAY","CYAN","PURPLE","BLUE","BROWN","GREEN","RED","BLACK")
            .firstOrNull { name.startsWith(it) } ?: return null
        return Material.entries.firstOrNull { it.name == "${color}_DYE" }
    }

    // ===== ARCHITECT_DEMOLITION skill 0 (稳定三硝基甲苯): TNT crafting reliability =====

    @EventHandler
    fun onTntCraft(event: org.bukkit.event.inventory.CraftItemEvent) {
        val result = event.recipe?.result ?: return
        if (result.type != org.bukkit.Material.TNT && result.type != org.bukkit.Material.TNT_MINECART) return
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.ARCHITECT_DEMOLITION)) return
        val lv = p.skillLevel(Branch.ARCHITECT_DEMOLITION, 0)
        if (lv < 3) return // Lv.3 only: guaranteed success with no explosion
        // Lv.1: 25% explosion, Lv.2: 10% explosion - these are negative effects we skip
    }

    // ===== CHEF_BUTCHER base: eating cooked meat +4 food level =====

    private val cookedMeats = setOf(
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
        Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.COOKED_SALMON
    )

    @EventHandler
    fun onButcherEat(event: PlayerItemConsumeEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.CHEF_BUTCHER)) return
        if (event.item.type in cookedMeats) {
            p.foodLevel = (p.foodLevel + 4).coerceAtMost(20)
        }
    }

    // ===== CHEF_BREWER skill 0 (熟练配制): extend potion duration =====

    @EventHandler
    fun onBrew(event: org.bukkit.event.inventory.BrewEvent) {
        // Find nearby players with brewer branch
        val loc = event.block.location
        for (p in loc.getNearbyPlayers(6.0)) {
            if (!p.hasBranch(Branch.CHEF_BREWER)) continue
            val lv = p.skillLevel(Branch.CHEF_BREWER, 0)
            if (lv < 1) continue
            // Extend potion duration: modify each output slot
            var extended = false
            for (i in 0..2) {
                val item = event.contents.getItem(i) ?: continue
                if (item.type != Material.POTION && item.type != Material.SPLASH_POTION &&
                    item.type != Material.LINGERING_POTION) continue
                item.editMeta { meta ->
                    if (meta is org.bukkit.inventory.meta.PotionMeta) {
                        val baseType = meta.basePotionType ?: return@editMeta
                        val hasRedstone = baseType.name.contains("LONG")
                        val newType = baseType // Keep same type, duration bonus is separate
                        // Add custom effect to extend duration (20s/40s/60s bonus)
                        val bonusSeconds = if (hasRedstone) when (lv) { 1 -> 30; 2 -> 40; 3 -> 60; else -> 0 }
                            else when (lv) { 1 -> 20; 2 -> 20; 3 -> 40; else -> 0 }
                        // The duration extension is applied via the potion's existing effects
                        meta.customEffects.forEach { effect ->
                            meta.addCustomEffect(
                                org.bukkit.potion.PotionEffect(
                                    effect.type,
                                    effect.duration + bonusSeconds * 20,
                                    effect.amplifier,
                                    effect.isAmbient,
                                    effect.hasParticles(),
                                    effect.hasIcon()
                                ), true
                            )
                        }
                        extended = true
                    }
                }
            }
            if (extended) p.sendMessage("§a✦ 熟练配制：药水持续时间延长")
        }
    }

    // ===== CHEF_BREWER skill 1 (蒸发控件): active 60s → brew drops blaze powder =====
    @EventHandler
    fun onBrewBlazeDrop(event: org.bukkit.event.inventory.BrewEvent) {
        val loc = event.block.location
        for (p in loc.getNearbyPlayers(3.0)) {
            if (!p.hasBranch(Branch.CHEF_BREWER)) continue
            val bonus = com.waterful.project.career.skill.SkillExecutor.getZhengFaBonus(p)
            if (bonus <= 0) continue
            if (p.roll(bonus, "蒸发控件·烈焰粉掉落(${bonus}%)")) {
                event.block.world.dropItemNaturally(event.block.location,
                    org.bukkit.inventory.ItemStack(Material.BLAZE_POWDER, 1))
                p.sendMessage("§a✦ 蒸发控件：获得烈焰粉")
            }
        }
    }

    // ===== FARMER_FISHERMAN skill 0 (垂钓熟手): fishing QTE reduction + Luck =====

    @EventHandler
    fun onFish(event: org.bukkit.event.player.PlayerFishEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.FARMER_FISHERMAN)) return
        val lv = p.skillLevel(Branch.FARMER_FISHERMAN, 0)

        when (event.state) {
            org.bukkit.event.player.PlayerFishEvent.State.FISHING -> {
                // 垂钓熟手: reduce wait time for bite (QTE difficulty reduction)
                if (lv >= 1) {
                    val hook = event.hook
                    // Reduce max wait time by 20%/35%/50% per level
                    val reduction = when (lv) { 1 -> 0.20; 2 -> 0.35; 3 -> 0.50; else -> 0.0 }
                    val minWait = hook.minWaitTime
                    val maxWait = hook.maxWaitTime
                    val newMax = (maxWait * (1.0 - reduction)).toInt().coerceAtLeast(minWait + 20)
                    hook.setWaitTime(minWait, newMax)
                }
            }
            org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH -> {
                // 垂钓熟手: QTE handled above; Luck I comes from base passive tick

                // 收获涛声 (Skill 1): extra matching fish on next catch
                val extraFish = com.waterful.project.career.skill.SkillExecutor.onShouHuoTaoShengCatch(p)
                if (extraFish > 0 && event.caught != null) {
                    val caught = event.caught!!
                    if (caught is org.bukkit.entity.Item) {
                        val stack = caught.itemStack
                        val extraStack = org.bukkit.inventory.ItemStack(stack.type, extraFish)
                        p.world.dropItemNaturally(p.location, extraStack)
                        p.sendMessage("§a✦ 收获涛声：额外获得 ${extraFish} 条鱼！")
                    }
                }

                // 授人以渔 (Eureka 1): extra 1~6 XP on each catch
                if (p.hasEureka(Branch.FARMER_FISHERMAN, 1)) {
                    val xp = (1..6).random()
                    p.giveExp(xp)
                }
            }
            else -> {}
        }
    }

    // ===== 决战冲锋: custom damage bonus (vs-player) + shield silence =====

    @EventHandler
    fun onChargeAssaultDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val bonus = SkillExecutor.getChargeAssaultBonus(damager)
        if (bonus <= 0.0) return
        // Damage bonus applies to all targets
        event.damage = event.damage * (1.0 + bonus)
        // Shield silence: extend shield cooldown for player victims blocking
        val shieldBonus = SkillExecutor.getChargeAssaultShield(damager)
        if (shieldBonus > 0.0 && event.entity is Player) {
            val victim = event.entity as Player
            if (victim.isBlocking) {
                victim.setCooldown(Material.SHIELD, (100 + (100 * shieldBonus).toInt()).coerceAtMost(200))
            }
        }
    }

    // ===== 我即梦魇: custom monster damage bonus =====

    @EventHandler
    fun onNightmareDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val bonus = SkillExecutor.getNightmareBonus(damager)
        if (bonus <= 0.0) return
        if (event.entity is org.bukkit.entity.Monster) {
            event.damage = event.damage * (1.0 + bonus)
        }
    }

    // ===== 血腥屠宰者 (Chef Butcher eureka 0): axe +25% dmg, +6 vs unarmored =====

    @EventHandler
    fun onBloodySlaughter(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.hasEureka(Branch.CHEF_BUTCHER, 0)) return
        val held = damager.inventory.itemInMainHand.type
        if (!held.name.contains("AXE")) return
        // +25% axe damage
        event.damage = event.damage * 1.25
        // +6 if target is unarmored
        if (event.entity is org.bukkit.entity.LivingEntity) {
            val living = event.entity as org.bukkit.entity.LivingEntity
            val armor = living.equipment?.armorContents?.count { it != null && it.type != Material.AIR } ?: 0
            if (armor == 0) {
                event.damage += 6.0
            }
        }
    }

    // ===== 放血 (Chef Butcher eureka 1): axe crit on livestock → poison I; execute low-HP poisoned livestock =====

    @EventHandler
    fun onBloodletting(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.hasEureka(Branch.CHEF_BUTCHER, 1)) return
        val held = damager.inventory.itemInMainHand.type
        if (!held.name.contains("AXE")) return
        val target = event.entity
        if (target !is org.bukkit.entity.Animals) return
        if (target.type !in livestockTypes) return

        // On critical hit: apply poison I (5s)
        val isCrit = damager.fallDistance > 0.0f && !damager.isOnGround && !damager.isInWater &&
            !damager.isInsideVehicle && !damager.isClimbing && !damager.isGliding
        if (isCrit) {
            target.addPotionEffect(PotionEffect(PotionEffectType.POISON, 5 * 20, 0, false, true))
        }

        // Execute check: nearby poisoned livestock below 50% HP
        val playerLoc = damager.location
        for (entity in playerLoc.getNearbyEntities(4.0, 4.0, 4.0)) {
            if (entity !is org.bukkit.entity.Animals) continue
            if (entity.type !in livestockTypes) continue
            if (!entity.hasPotionEffect(PotionEffectType.POISON)) continue
            val maxHp = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 10.0
            if (entity.health > maxHp * 0.5) continue
            // Execute: set health to 0 directly (triggers death without damage recursion)
            entity.health = 0.0
            // Drop extra raw meat
            rawMeat[entity.type]?.let { mat ->
                entity.world.dropItemNaturally(entity.location, ItemStack(mat, 1))
            }
        }
    }

    // ===== 凌空创想: scaffolding boost on move =====

    @EventHandler
    fun onLingKongMove(event: org.bukkit.event.player.PlayerMoveEvent) {
        val p = event.player
        if (event.from.blockX == event.to?.blockX && event.from.blockY == event.to?.blockY && event.from.blockZ == event.to?.blockZ) return
        SkillExecutor.tickLingKongChuangXiang(p)
    }

    // ===== 我即梦魇: reapply aura on player move =====

    @EventHandler
    fun onNightmareMove(event: org.bukkit.event.player.PlayerMoveEvent) {
        val p = event.player
        if (event.from.blockX == event.to?.blockX && event.from.blockZ == event.to?.blockZ) return
        SkillExecutor.reapplyNightmareAura(p)
    }

    /** 无畏突刺: 挥剑/斧时向前突进 (PlayerAnimationEvent covers both air swing and hit) */
    @EventHandler
    fun onFearlessThrustSwing(event: org.bukkit.event.player.PlayerAnimationEvent) {
        com.waterful.project.career.skill.EurekaEffectHandler.onFearlessThrustSwing(event.player)
    }
}
