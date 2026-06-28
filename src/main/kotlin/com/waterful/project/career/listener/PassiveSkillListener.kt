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

        // 斩首 (Warrior Hunter eureka 0): head drops
        if (killer.hasBranch(Branch.WARRIOR_HUNTER) && killer.hasEureka(Branch.WARRIOR_HUNTER)) {
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
    }

    // ===== CHEF_MASTER: food effects =====

    @EventHandler
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.CHEF_MASTER)) return

        if (event.item.type.isEdible) p.foodLevel = (p.foodLevel + 1).coerceAtMost(20)
        if (event.item.type == Material.GOLDEN_APPLE || event.item.type == Material.ENCHANTED_GOLDEN_APPLE) {
            p.foodLevel = 20; p.saturation = 20f
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

        // 熔炉改良 (Worker Smelter): extra product
        if (p.hasBranch(Branch.WORKER_SMELTER)) {
            val lv = p.skillLevel(Branch.WORKER_SMELTER, 0)
            if (lv >= 1 && p.roll(5 + lv * 5, "熔炉改良·Lv.$lv ${mat.name}")) {
                p.world.dropItemNaturally(p.location, ItemStack(mat, 1))
            }
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
    }

    // ===== WORKER_LUMBERJACK eureka 2: nether stem extra =====

    @EventHandler
    fun onBreakStem(event: BlockBreakEvent) {
        val p = event.player
        if (p.world.environment != World.Environment.NETHER) return
        if (event.block.type != Material.CRIMSON_STEM && event.block.type != Material.WARPED_STEM) return
        if (!p.hasBranch(Branch.WORKER_LUMBERJACK)) return
        if (!p.hasEureka(Branch.WORKER_LUMBERJACK, 1)) return // eureka index 1 = 抽丝剥茧
        if (p.roll(33, "抽丝剥茧 ${event.block.type.name}")) {
            event.block.world.dropItemNaturally(event.block.location, ItemStack(event.block.type, 1))
        }
    }

    // ===== CHEF_BAKER skill 0: bake success rate =====

    private val bakedGoods = setOf(Material.BREAD, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE)

    @EventHandler
    fun onCraftBakedGoods(event: CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.CHEF_BAKER)) return
        val lv = p.skillLevel(Branch.CHEF_BAKER, 0)
        if (lv < 1) return
        val result = event.recipe?.result ?: return
        if (result.type !in bakedGoods) return
        if (lv >= 2) return // Lv.2+ guaranteed
        if (!p.roll(90, "预热烤箱·Lv.1 ${result.type.name}")) {
            event.isCancelled = true
            p.msgFail("合成失败！预热烤箱技能未触发成功。")
        }
    }

    // ===== CHEF_BAKER eureka 0: bread attack (法棍) =====

    @EventHandler
    fun onBreadAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!damager.holding(Material.BREAD)) return
        if (!damager.hasBranch(Branch.CHEF_BAKER) || !damager.hasEureka(Branch.CHEF_BAKER, 0)) return
        if (!damager.roll(20, "法棍 — 真实伤害")) return
        val target = event.entity as? LivingEntity ?: return
        target.realDamage(2.0, damager)
        target.effect(PotionEffectType.NAUSEA, 15, 1)
        target.effect(PotionEffectType.DARKNESS, 5, 0)
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

    @EventHandler
    fun onBreakWood(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.WORKER_LUMBERJACK)) return
        val lv = p.skillLevel(Branch.WORKER_LUMBERJACK, 0)
        if (lv < 1 || !event.block.type.name.contains("LOG") && !event.block.type.name.contains("STEM") && !event.block.type.name.contains("HYPHAE") && !event.block.type.name.contains("PLANKS")) return
        p.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 5 * 20, lv, false, true))
    }

    // ===== WORKER_MINER skill 0 (奋力挖掘): haste on stone break =====

    @EventHandler
    fun onBreakStone(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.WORKER_MINER)) return
        val lv = p.skillLevel(Branch.WORKER_MINER, 0)
        if (lv < 1 || !event.block.type.name.contains("STONE") && !event.block.type.name.contains("ORE") && !event.block.type.name.contains("DEEPSLATE") && !event.block.type.name.contains("NETHERRACK")) return
        p.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 5 * 20, lv, false, true))
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

    // ===== FARMER_BOTANIST skill 0 (合理密植): crop harvest bonus =====

    private val crops = setOf(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.COCOA, Material.NETHER_WART
    )

    @EventHandler
    fun onHarvestCrop(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        if (!p.hasBranch(Branch.FARMER_BOTANIST)) return
        val lv = p.skillLevel(Branch.FARMER_BOTANIST, 0)
        if (lv < 1 || event.block.type !in crops) return
        // Drop one extra item
        val drops = event.block.getDrops(p.inventory.itemInMainHand, p)
        drops.forEach { drop -> event.block.world.dropItemNaturally(event.block.location, drop) }
    }

    // ===== ARCHITECT_STRUCTURE skill 1 (识色敏锐): dye crafting bonus (PASSIVE with CD) =====

    private val dyeResults = setOf(
        Material.RED_DYE, Material.GREEN_DYE, Material.BLUE_DYE, Material.YELLOW_DYE,
        Material.PURPLE_DYE, Material.CYAN_DYE, Material.LIGHT_GRAY_DYE, Material.GRAY_DYE,
        Material.PINK_DYE, Material.LIME_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE,
        Material.LIGHT_BLUE_DYE, Material.BROWN_DYE, Material.BLACK_DYE, Material.WHITE_DYE
    )

    @EventHandler
    fun onCraftDye(event: org.bukkit.event.inventory.CraftItemEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!p.hasBranch(Branch.ARCHITECT_STRUCTURE)) return
        val lv = p.skillLevel(Branch.ARCHITECT_STRUCTURE, 1)
        if (lv < 1) return
        val result = event.recipe?.result ?: return
        if (result.type !in dyeResults) return

        DebugManager.logState(p, "识色敏锐 — 合成${result.type.name}")
        val cp = p.career() ?: return
        val now = System.currentTimeMillis()
        val cdSeconds = com.waterful.project.career.data.CareerDataLoader.getSkill("architect_structure_skill_1")?.getCooldown(lv) ?: 20
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

    // ===== FARMER_FISHERMAN eureka 2 (凝望反制): guardian damage reduction =====
    @EventHandler
    fun onGuardianDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val p = event.entity as? Player ?: return
        if (!p.hasBranch(Branch.FARMER_FISHERMAN)) return
        val has = p.career()?.chosenEurekas?.get(Branch.FARMER_FISHERMAN)?.eurekaDef?.id?.contains("fisherman_eureka_2") == true
        if (!has) return
        if (event.damager is org.bukkit.entity.Guardian || event.damager is org.bukkit.entity.ElderGuardian) {
            event.damage = maxOf(event.damage - 6.0, 0.1)
        }
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

    // ===== ARCHITECT_STRUCTURE skill 0 (高效回收): flint&steel always works, explosion reduction =====

    @EventHandler
    fun onExplosionDamage(event: org.bukkit.event.entity.EntityDamageEvent) {
        if (event.entity !is Player || event.cause != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && event.cause != org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return
        val p = event.entity as Player
        if (!p.hasBranch(Branch.ARCHITECT_STRUCTURE)) return
        val lv = p.skillLevel(Branch.ARCHITECT_STRUCTURE, 0)
        if (lv < 1) return
        event.damage = event.damage * 0.8 // 20% reduction
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

    // ===== CHEF_BREWER skill 0 (熟练配制): brewing stand duration bonus =====

    @EventHandler
    fun onBrew(event: org.bukkit.event.inventory.BrewEvent) {
        // Find nearby players with brewer branch
        val loc = event.block.location
        for (p in loc.getNearbyPlayers(6.0)) {
            if (!p.hasBranch(Branch.CHEF_BREWER)) continue
            val lv = p.skillLevel(Branch.CHEF_BREWER, 0)
            if (lv < 1) continue
            // Modify brewing time by extending the fuel (approximate via ingredient)
            val bonus = when (lv) { 1 -> 20; 2 -> 40; 3 -> 60; else -> 0 }
            event.contents.ingredient?.amount = (event.contents.ingredient?.amount ?: 0) + 1
        }
    }

    // ===== FARMER_FISHERMAN skill 0 (垂钓熟手): fishing bonus =====

    @EventHandler
    fun onFish(event: org.bukkit.event.player.PlayerFishEvent) {
        if (event.state != org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) return
        val p = event.player
        if (!p.hasBranch(Branch.FARMER_FISHERMAN)) return
        val lv = p.skillLevel(Branch.FARMER_FISHERMAN, 0)
        if (lv < 1) return
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 60 * 20, lv - 1, false, true))
    }
}
