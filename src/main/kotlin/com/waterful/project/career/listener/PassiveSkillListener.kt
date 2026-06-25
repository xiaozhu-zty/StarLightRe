package com.waterful.project.career.listener

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.DebugManager
import com.waterful.project.career.model.Branch
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantInventory

/**
 * Handles PASSIVE skill effects with probability-based outcomes.
 * ALL probability events output pass/fail via DebugManager when listening mode is on.
 */
class PassiveSkillListener : Listener {

    // ===== CHEF_BUTCHER: 庖丁遗风 + base bone drop =====

    private val livestockTypes: Set<EntityType> = setOf(
        EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
        EntityType.RABBIT, EntityType.GOAT, EntityType.HOGLIN, EntityType.MOOSHROOM
    )

    private val rawMeatMap: Map<EntityType, Material> = mapOf(
        EntityType.COW to Material.BEEF, EntityType.PIG to Material.PORKCHOP,
        EntityType.SHEEP to Material.MUTTON, EntityType.CHICKEN to Material.CHICKEN,
        EntityType.RABBIT to Material.RABBIT, EntityType.GOAT to Material.MUTTON,
        EntityType.HOGLIN to Material.PORKCHOP, EntityType.MOOSHROOM to Material.BEEF
    )

    private val cookedMeatMap: Map<EntityType, Material> = mapOf(
        EntityType.COW to Material.COOKED_BEEF, EntityType.PIG to Material.COOKED_PORKCHOP,
        EntityType.SHEEP to Material.COOKED_MUTTON, EntityType.CHICKEN to Material.COOKED_CHICKEN,
        EntityType.RABBIT to Material.COOKED_RABBIT, EntityType.GOAT to Material.COOKED_MUTTON,
        EntityType.HOGLIN to Material.COOKED_PORKCHOP, EntityType.MOOSHROOM to Material.COOKED_BEEF
    )

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer ?: return

        // ---- Butcher: bone + meat ----
        if (entity.type in livestockTypes) {
            val cp = CareerManager.getPlayer(killer) ?: return
            if (CareerManager.hasBranch(cp, Branch.CHEF_BUTCHER)) {
                entity.world.dropItemNaturally(entity.location, ItemStack(Material.BONE, 1))
                val skill = cp.getSkill(Branch.CHEF_BUTCHER, 0)
                val level = skill?.currentLevel ?: 0
                if (level >= 1 && entity.fireTicks <= 0) {
                    val mat = if (level >= 3) cookedMeatMap[entity.type] else rawMeatMap[entity.type]
                    mat?.let { entity.world.dropItemNaturally(entity.location, ItemStack(it, 1)) }
                }
            }
        }

        // ---- Warrior Hunter Eureka 斩首: head drop chance ----
        val cp2 = CareerManager.getPlayer(killer) ?: return
        if (CareerManager.hasBranch(cp2, Branch.WARRIOR_HUNTER)) {
            val eureka = cp2.chosenEurekas[Branch.WARRIOR_HUNTER]
            if (eureka != null && eureka.eurekaDef.id.contains("hunter_eureka_0")) {
                val headChance: Int? = when (entity.type) {
                    EntityType.WITHER_SKELETON -> 5
                    EntityType.SKELETON -> 2
                    EntityType.ZOMBIE -> 2
                    EntityType.CREEPER -> 2
                    EntityType.PIGLIN, EntityType.PIGLIN_BRUTE -> 1
                    EntityType.ENDER_DRAGON -> 5
                    else -> null
                }
                if (headChance != null && DebugManager.rollChance(killer, headChance, "斩首·${entity.type} 头颅掉落")) {
                    val headMat = when (entity.type) {
                        EntityType.WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL
                        EntityType.SKELETON -> Material.SKELETON_SKULL
                        EntityType.ZOMBIE -> Material.ZOMBIE_HEAD
                        EntityType.CREEPER -> Material.CREEPER_HEAD
                        EntityType.PIGLIN, EntityType.PIGLIN_BRUTE -> Material.PIGLIN_HEAD
                        EntityType.ENDER_DRAGON -> Material.DRAGON_HEAD
                        else -> null
                    }
                    headMat?.let { entity.world.dropItemNaturally(entity.location, ItemStack(it, 1)) }
                }
            }
        }
    }

    // ===== CHEF_MASTER base: food +1 hunger, golden apple full restore =====

    @EventHandler
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val player = event.player
        val type = event.item.type
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.CHEF_MASTER)) return
        if (type.isEdible) player.foodLevel = (player.foodLevel + 1).coerceAtMost(20)
        if (type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE) {
            player.foodLevel = 20; player.saturation = 20f
        }
    }

    // ===== CHEF_MASTER skill 0 (精准火候): furnace food 10/20/30% duplicate =====

    private val cookedFoods: Set<Material> = setOf(
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
        Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.COOKED_SALMON, Material.BAKED_POTATO, Material.DRIED_KELP
    )

    @EventHandler
    fun onFurnaceExtract(event: FurnaceExtractEvent) {
        val player = event.player
        val cp = CareerManager.getPlayer(player) ?: return
        val mat = event.itemType

        // ---- Chef Master 精准火候: 10/20/30% food duplicate ----
        if (CareerManager.hasBranch(cp, Branch.CHEF_MASTER) && mat in cookedFoods) {
            val skill = cp.getSkill(Branch.CHEF_MASTER, 0)
            val level = skill?.currentLevel ?: 0
            if (level >= 1) {
                val chance = level * 10
                if (DebugManager.rollChance(player, chance, "精准火候·Lv.$level ${mat.name} 额外掉落")) {
                    player.world.dropItemNaturally(player.location, ItemStack(mat, event.itemAmount))
                }
            }
        }

        // ---- Worker Smelter 熔炉改良: 10/15/20% extra smelted product ----
        if (CareerManager.hasBranch(cp, Branch.WORKER_SMELTER)) {
            val skill = cp.getSkill(Branch.WORKER_SMELTER, 0)
            val level = skill?.currentLevel ?: 0
            if (level >= 1) {
                val chance = 5 + level * 5 // Lv.1=10%, Lv.2=15%, Lv.3=20%
                if (DebugManager.rollChance(player, chance, "熔炉改良·Lv.$level ${mat.name} 额外产物")) {
                    player.world.dropItemNaturally(player.location, ItemStack(mat, 1))
                }
            }
        }
    }

    // ===== ARCHITECT_FORTRESS skill 0 Lv.3 (缓冲装置): 20% fall damage immunity =====

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return
        val player = event.entity as Player
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.ARCHITECT_FORTRESS)) return
        val skill = cp.getSkill(Branch.ARCHITECT_FORTRESS, 0)
        if (skill?.currentLevel != 3) return
        if (DebugManager.rollChance(player, 20, "缓冲装置·Lv.3 摔落免伤")) {
            event.isCancelled = true
        }
    }

    // ===== SCHOLAR_ENCHANTER skill 0 (注魔宝典): 20/40/60% enchant EXP cost -3 =====

    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        val player = event.enchanter
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_ENCHANTER)) return
        val skill = cp.getSkill(Branch.SCHOLAR_ENCHANTER, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val chance = level * 20 // Lv.1=20%, Lv.2=40%, Lv.3=60%
        if (DebugManager.rollChance(player, chance, "注魔宝典·Lv.$level 附魔经验减免")) {
            val newCost = (event.expLevelCost - 3).coerceAtLeast(1)
            event.expLevelCost = newCost
        }
    }

    // ===== SCHOLAR_REDSTONE skill 0 (电路设计): 4/6/8% no redstone consumption =====

    @EventHandler
    fun onPlaceRedstone(event: BlockPlaceEvent) {
        if (event.block.type != Material.REDSTONE_WIRE && event.block.type != Material.REDSTONE) return
        val player = event.player
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_REDSTONE)) return
        val skill = cp.getSkill(Branch.SCHOLAR_REDSTONE, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val chance = 2 + level * 2 // Lv.1=4%, Lv.2=6%, Lv.3=8%
        if (DebugManager.rollChance(player, chance, "电路设计·Lv.$level 不消耗红石粉")) {
            // Refund the redstone dust to the player's inventory
            val held = player.inventory.itemInMainHand
            if (held.type == Material.REDSTONE) {
                held.amount = (held.amount + 1).coerceAtMost(64)
            } else {
                player.inventory.addItem(ItemStack(Material.REDSTONE, 1))
            }
        }
    }

    // ===== WORKER_TOOLMAKER skill 0 (镀层工艺): 10/15/20% no netherite consumption =====
    // Deferred — PrepareSmithingEvent is read-only; requires InventoryClickEvent tracking.

    // ===== FARMER_RANCHER skill 0 (动物育种): 0.5/1/2% spawn egg on breed =====
    // Note: uses a sub-percent roll. rollChance treats 0.5/1/2 as integer (1%/1%/2% approx).

    @EventHandler
    fun onAnimalBreedForEgg(event: org.bukkit.event.entity.EntityBreedEvent) {
        val breeder = event.breeder as? Player ?: return
        val cp = CareerManager.getPlayer(breeder) ?: return
        if (!CareerManager.hasBranch(cp, Branch.FARMER_RANCHER)) return
        val skill = cp.getSkill(Branch.FARMER_RANCHER, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val chance = when (level) { 1 -> 1; 2 -> 1; 3 -> 2; else -> 0 } // 0.5%→1%, 1%→1%, 2%→2%
        if (!DebugManager.rollChance(breeder, chance, "动物育种·Lv.$level 刷怪蛋(${if(level==1)"~0.5" else if(level==2)"1" else "2"}%)")) return
        val eggMat = when (event.entity.type) {
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

    // ===== WORKER_TOOLMAKER skill 0 (镀层工艺): 10/15/20% netherite refund on smith ====
    // Tracked via InventoryClickEvent on smithing table result slot (slot index 3 in smithing GUI)
    @EventHandler
    fun onSmithingClick(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.SMITHING) return
        if (event.slotType != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) return
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.WORKER_TOOLMAKER)) return
        val skill = cp.getSkill(Branch.WORKER_TOOLMAKER, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        DebugManager.logState(player, "镀层工艺 — 锻造台完成，检测下界合金锭消耗")
        val chance = 5 + level * 5 // 10/15/20%
        if (DebugManager.rollChance(player, chance, "镀层工艺·Lv.$level 不消耗下界合金锭")) {
            // Refund 1 netherite ingot after the smith completes (next tick)
            val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe")
            if (plugin != null) {
                player.server.scheduler.runTask(plugin, Runnable {
                    player.inventory.addItem(ItemStack(Material.NETHERITE_INGOT, 1)).values.forEach {
                        player.world.dropItemNaturally(player.location, it)
                    }
                })
            }
        }
    }

    // ===== FARMER_MERCHANT skill 0 (商业谈判): 15/20/25% emerald return ====
    // Detect actual trade completion via merchant inventory result slot click

    @EventHandler
    fun onTradeCompleted(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.MERCHANT) return
        // The result slot in merchant GUI is slot 2
        if (event.slot != 2) return
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.FARMER_MERCHANT)) return

        // Get the merchant recipe to find actual emerald cost
        val merchantInv = event.inventory as? MerchantInventory ?: return
        val recipe = merchantInv.selectedRecipe ?: return
        val ingredients = recipe.ingredients

        // Count emeralds in the trade cost
        var emeraldCost = 0
        for (ing in ingredients) {
            if (ing.type == Material.EMERALD) emeraldCost += ing.amount
        }
        if (emeraldCost <= 0) return // No emerald cost in this trade

        DebugManager.logState(player, "商业谈判 — 检测到交易: 花费${emeraldCost}绿宝石 (配方: ${recipe.result.type.name})")
        val skill = cp.getSkill(Branch.FARMER_MERCHANT, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val chance = 10 + level * 5 // 15/20/25%
        if (DebugManager.rollChance(player, chance, "商业谈判·Lv.$level 绿宝石返还(花费${emeraldCost}颗)")) {
            val refundPct = 20 + level * 10 // 30/40/50%
            val refund = (emeraldCost * refundPct / 100).coerceAtLeast(1)
            player.inventory.addItem(ItemStack(Material.EMERALD, refund)).values.forEach {
                player.world.dropItemNaturally(player.location, it)
            }
            player.sendMessage("§a商业谈判生效，返还了 $refund 颗绿宝石！")
        }
    }

    // ===== WORKER_LUMBERJACK eureka 2 (抽丝剥茧): 33% extra stem in nether ====
    @EventHandler
    fun onBreakStem(event: org.bukkit.event.block.BlockBreakEvent) {
        val player = event.player
        // Only in nether
        if (event.block.world.environment != org.bukkit.World.Environment.NETHER) return
        val blockType = event.block.type
        if (blockType != Material.CRIMSON_STEM && blockType != Material.WARPED_STEM) return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.WORKER_LUMBERJACK)) return
        val eureka = cp.chosenEurekas[Branch.WORKER_LUMBERJACK]
        if (eureka?.eurekaDef?.id?.contains("lumberjack_eureka_2") != true) return
        DebugManager.logState(player, "抽丝剥茧 — 下界破坏${blockType.name}")
        if (DebugManager.rollChance(player, 33, "抽丝剥茧 ${blockType.name} 额外掉落")) {
            event.block.world.dropItemNaturally(event.block.location, ItemStack(blockType, 1))
        }
    }

    // ===== CHEF_BAKER skill 0 (预热烤箱): 90%/100%/100% craft success ====
    @EventHandler
    fun onCraftBakedGoods(event: org.bukkit.event.inventory.CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.CHEF_BAKER)) return
        val skill = cp.getSkill(Branch.CHEF_BAKER, 0)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val result = event.recipe?.result ?: return
        // Only for baked goods
        val bakedGoods = setOf(Material.BREAD, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE)
        if (result.type !in bakedGoods) return
        DebugManager.logState(player, "预热烤箱 — 合成${result.type.name}")
        val chance = when (level) { 1 -> 90; else -> 100 }
        if (chance >= 100) return // Guaranteed
        if (!DebugManager.rollChance(player, chance, "预热烤箱·Lv.$level ${result.type.name} 合成成功")) {
            event.isCancelled = true
            player.sendMessage("§c合成失败！预热烤箱技能未触发成功。")
        }
    }

    // ===== CHEF_BAKER eureka 0 (法棍): 20% 2 true damage with bread ====
    @EventHandler
    fun onBreadAttack(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val held = damager.inventory.itemInMainHand
        if (held.type != Material.BREAD) return
        val cp = CareerManager.getPlayer(damager) ?: return
        if (!CareerManager.hasBranch(cp, Branch.CHEF_BAKER)) return
        val eureka = cp.chosenEurekas[Branch.CHEF_BAKER]
        if (eureka?.eurekaDef?.id?.contains("baker_eureka_0") != true) return
        if (DebugManager.rollChance(damager, 20, "法棍 — 真实伤害")) {
            // Apply 2 true damage (bypass armor)
            val target = event.entity as? org.bukkit.entity.LivingEntity ?: return
            target.damage(2.0, damager)
            target.addPotionEffect(org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NAUSEA, 20 * 15, 1, false, true
            ))
            target.addPotionEffect(org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 20 * 5, 0, false, true
            ))
        }
    }

    // ===== CHEF_BAKER eureka 2 (幸运曲奇): cookie effects on eat ====
    @EventHandler
    fun onEatCookie(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (event.item.type != Material.COOKIE) return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.CHEF_BAKER)) return
        val eureka = cp.chosenEurekas[Branch.CHEF_BAKER]
        if (eureka?.eurekaDef?.id?.contains("baker_eureka_2") != true) return
        // 20%: Luck I
        if (DebugManager.rollChance(player, 20, "幸运曲奇 — 幸运I(20%)")) {
            player.addPotionEffect(org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.LUCK, 20 * 30, 0, false, true
            ))
        }
        // 5%: Luck II
        if (DebugManager.rollChance(player, 5, "幸运曲奇 — 幸运II(5%)")) {
            player.addPotionEffect(org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.LUCK, 20 * 30, 1, false, true
            ))
        }
        // 0.1%: Diamond
        if (DebugManager.rollChanceFraction(player, 1, 1000, "幸运曲奇 — 钻石(0.1%)")) {
            player.inventory.addItem(ItemStack(Material.DIAMOND, 1)).values.forEach {
                player.world.dropItemNaturally(player.location, it)
            }
        }
    }

    // ===== SCHOLAR_REDSTONE eureka 0 (红色炬火): 25% ignite with redstone torch ====
    @EventHandler
    fun onRedstoneTorchAttack(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (damager.inventory.itemInMainHand.type != Material.REDSTONE_TORCH) return
        val cp = CareerManager.getPlayer(damager) ?: return
        if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_REDSTONE)) return
        val eureka = cp.chosenEurekas[Branch.SCHOLAR_REDSTONE]
        if (eureka?.eurekaDef?.id?.contains("redstone_eureka_0") != true) return
        if (DebugManager.rollChance(damager, 25, "红色炬火 — 点燃")) {
            event.entity.fireTicks = 20 * 5 // 5 seconds of fire
        }
    }

    // ===== SCHOLAR_REDSTONE eureka 2 (雷霆之杖): lightning with lightning rod ====
    @EventHandler
    fun onLightningRodUse(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.LIGHTNING_ROD) return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_REDSTONE)) return
        val eureka = cp.chosenEurekas[Branch.SCHOLAR_REDSTONE]
        if (eureka?.eurekaDef?.id?.contains("redstone_eureka_2") != true) return
        val world = player.world
        val baseChance = 10
        val weatherBonus = if (world.isThundering) 33 else if (world.hasStorm()) 20 else baseChance
        val weatherState = if (world.isThundering) "雷暴" else if (world.hasStorm()) "降雨" else "晴朗"
        DebugManager.logState(player, "雷霆之杖 — 天气:$weatherState 概率:$weatherBonus%")
        val desc = "雷霆之杖($weatherState$weatherBonus%)"
        if (!DebugManager.rollChance(player, weatherBonus, desc)) {
            // On failure, lightning rod disappears
            val held = player.inventory.itemInMainHand
            if (held.type == Material.LIGHTNING_ROD) held.amount -= 1
            return
        }
        // Strike lightning at target location
        val target = player.getTargetBlockExact(50)?.location ?: player.location
        world.strikeLightning(target)
    }

    // ===== WORKER_TOOLMAKER skill 2 (关键结构加固): durability enchant on diamond tool craft ====
    @EventHandler
    fun onCraftDiamondTool(event: org.bukkit.event.inventory.CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.WORKER_TOOLMAKER)) return
        val skill = cp.getSkill(Branch.WORKER_TOOLMAKER, 2)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val result = event.recipe?.result ?: return
        // Only diamond tools
        val diamondTools = setOf(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
            Material.DIAMOND_HOE, Material.DIAMOND_SWORD)
        if (result.type !in diamondTools) return

        // Set durability enchant based on level
        // Lv.1: 100% Unbreaking I -> Lv.2: 75% I / 25% II -> Lv.3: 55% I / 40% II / 5% III
        val enchLevel = when (level) {
            1 -> 1 // Always Unbreaking I
            2 -> {
                if (DebugManager.rollChance(player, 25, "关键结构加固·Lv.2 耐久II(25%)")) 2 else 1
            }
            3 -> {
                when {
                    DebugManager.rollChance(player, 5, "关键结构加固·Lv.3 耐久III(5%)") -> 3
                    DebugManager.rollChance(player, 40, "关键结构加固·Lv.3 耐久II(40%)") -> 2
                    else -> 1
                }
            }
            else -> 1
        }
        // Modify the result to include Unbreaking enchant
        event.inventory.result?.editMeta { meta ->
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, enchLevel, true)
        }
    }

    // ===== SCHOLAR_ENCHANTER skill 1 (魔力虹吸): durability restore on grindstone ====
    @EventHandler
    fun onGrindstoneUse(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.GRINDSTONE) return
        if (event.slot != 2) return // Result slot
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_ENCHANTER)) return
        val skill = cp.getSkill(Branch.SCHOLAR_ENCHANTER, 1)
        val level = skill?.currentLevel ?: 0
        if (level < 1) return
        val chance = level * 10 // 10/20/30%
        if (DebugManager.rollChance(player, chance, "魔力虹吸·Lv.$level 耐久回复")) {
            // Refund some XP and the grindstone removes enchants; we add bonus XP
            player.giveExp(5)
        }
    }

    // ===== Scholar Teacher skill 1 (厚积薄发): handled via command system =====
    // ===== Chef Brewer skill 1 (蒸发控件): ACTIVE, handled via /skill1 =====
}
