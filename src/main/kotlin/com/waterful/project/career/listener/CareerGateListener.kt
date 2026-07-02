package com.waterful.project.career.listener

import com.waterful.project.career.CareerItems
import com.waterful.project.career.hasEureka
import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.DebugManager
import com.waterful.project.career.model.Branch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*

/**
 * Permission-gating listener: blocks actions that require specific career branches.
 * "可以XXX" in the design doc means WITHOUT that branch, you CANNOT do it.
 * When DebugManager.isListening is on for a player, ALL checks output pass/fail messages.
 */
class CareerGateListener : Listener {

    // ========== RECIPE GATING ==========

    /** All advanced redstone components — only Redstone Engineer can craft/place */
    private val redstoneGated: Set<Material> = setOf(
        Material.HOPPER, Material.DISPENSER, Material.DROPPER,
        Material.REPEATER, Material.COMPARATOR, Material.PISTON, Material.STICKY_PISTON,
        Material.OBSERVER, Material.REDSTONE_BLOCK, Material.REDSTONE_LAMP,
        Material.DAYLIGHT_DETECTOR, Material.TARGET, Material.NOTE_BLOCK
    )

    private val toolmakerGatedRecipes: Set<Material> = setOf(
        Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
        Material.DIAMOND_HOE, Material.DIAMOND_SWORD,
        Material.COMPASS, Material.CLOCK, Material.SPYGLASS,
        Material.SHEARS, Material.LEAD
    )

    private val demoGatedRecipes: Set<Material> = setOf(
        Material.TNT, Material.TNT_MINECART
    )

    private val bakerGatedRecipes: Set<Material> = setOf(
        Material.BREAD, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE,
        Material.MUSHROOM_STEW, Material.BEETROOT_SOUP, Material.RABBIT_STEW,
        Material.SUSPICIOUS_STEW
    )

    private fun isScrollProtected(event: org.bukkit.event.inventory.InventoryClickEvent): Boolean {
        val player = event.whoClicked as? Player ?: return false
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") as? org.bukkit.plugin.java.JavaPlugin ?: return false
        // Check all items in the crafting/smithing/etc grid for the scroll
        for (item in event.inventory.contents) {
            if (item != null && CareerItems.isScroll(plugin, item)) return true
        }
        // Also check cursor
        event.cursor?.let { if (CareerItems.isScroll(plugin, it)) return true }
        return false
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        // Block scroll from crafting
        if (isScrollProtected(event)) {
            event.isCancelled = true
            player.sendMessage("§c技能卷轴无法用于合成！")
            return
        }
        val result = event.recipe?.result ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        val resultType = result.type
        val debug = DebugManager.isListening(player.uniqueId)

        // Check redstone-engineered items (craft + place)
        if (resultType in redstoneGated) {
            if (CareerManager.hasBranch(cp, Branch.SCHOLAR_REDSTONE)) {
                if (debug) player.sendMessage(debugPass("红石工程师", resultType.name))
            } else {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("红石工程师", resultType.name) else gateMsg("红石工程师"))
                return
            }
        }

        // Check toolmaker items
        if (resultType in toolmakerGatedRecipes) {
            if (CareerManager.hasBranch(cp, Branch.WORKER_TOOLMAKER)) {
                if (debug) player.sendMessage(debugPass("工具制造商", resultType.name))
            } else {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("工具制造商", resultType.name) else gateMsg("工具制造商"))
                return
            }
        }

        // Check demolitionist items — gate + failure rate
        if (resultType in demoGatedRecipes) {
            if (CareerManager.hasBranch(cp, Branch.ARCHITECT_DEMOLITION)) {
                if (debug) player.sendMessage(debugPass("爆破师", resultType.name))
                // Apply failure/explosion mechanic based on 稳定三硝基甲苯 level
                val skillLv = cp.getSkill(Branch.ARCHITECT_DEMOLITION, 0)?.currentLevel ?: 0
                val failRate = when (skillLv) { 0 -> 0.50; 1 -> 0.25; 2 -> 0.10; 3 -> 0.0; else -> 0.50 }
                if (failRate > 0 && DebugManager.rollChance(player, (failRate * 100).toInt(), "稳定三硝基甲苯 — 合成失败率")) {
                    event.isCancelled = true
                    player.sendMessage("§c合成失败！TNT不稳定...")
                    // Explosion chance on failure
                    val explodeRate = when (skillLv) { 0 -> 0.50; 1 -> 0.25; 2 -> 0.10; else -> 0.50 }
                    if (DebugManager.rollChance(player, (explodeRate * 100).toInt(), "稳定三硝基甲苯 — 合成爆炸")) {
                        player.world.createExplosion(player.location, 2.0f, false, false)
                        player.sendMessage("§c⚠ TNT在合成台中爆炸了！")
                    }
                }
            } else {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("爆破师", resultType.name) else gateMsg("爆破师"))
                return
            }
        }

        // Check baker food items
        if (resultType in bakerGatedRecipes) {
            if (CareerManager.hasBranch(cp, Branch.CHEF_BAKER)) {
                if (debug) player.sendMessage(debugPass("烘焙师", resultType.name))
            } else {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("烘焙师", resultType.name) else gateMsg("烘焙师"))
                return
            }
        }

        // 凝固剂 (Architect Structure eureka 1): only players with the eureka can craft concrete from powder
        if (resultType.name.contains("CONCRETE") && !resultType.name.contains("POWDER")) {
            val hasEureka = cp.chosenEurekas[Branch.ARCHITECT_STRUCTURE]?.eurekaDef?.id?.contains("structure_eureka_1") == true
            if (!hasEureka) {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("结构工程师·凝固剂", resultType.name) else gateMsg("结构工程师·凝固剂"))
                return
            }
        }

        // 地狱厨房 (Chef Master eureka 0): only players with the eureka can craft enchanted golden apples
        if (resultType == Material.ENCHANTED_GOLDEN_APPLE) {
            val hasEureka = cp.chosenEurekas[Branch.CHEF_MASTER]?.eurekaDef?.id?.contains("chef_master_eureka_0") == true
            if (!hasEureka) {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("主厨·地狱厨房", "附魔金苹果") else gateMsg("主厨·地狱厨房"))
                return
            }
        }
    }

    // ========== STRUCTURE ENGINEER GATING (装饰方块/染色方块/玻璃) ==========

    private val structureGated: Set<Material> = setOf(
        // 装饰性方块
        Material.BELL, Material.CHISELED_BOOKSHELF, Material.FLETCHING_TABLE,
        Material.JUKEBOX, Material.LECTERN, Material.LODESTONE, Material.RESPAWN_ANCHOR,
        Material.END_ROD, Material.LOOM,
        // 染色方块
        Material.WHITE_CONCRETE_POWDER, Material.WHITE_CONCRETE, Material.WHITE_TERRACOTTA,
        Material.WHITE_GLAZED_TERRACOTTA, Material.WHITE_WOOL, Material.WHITE_CARPET,
        // 玻璃
        Material.GLASS, Material.GLASS_PANE, Material.TINTED_GLASS,
        Material.WHITE_STAINED_GLASS, Material.WHITE_STAINED_GLASS_PANE,
        // 其他
        Material.LANTERN, Material.SOUL_LANTERN, Material.BARREL,
        Material.ENDER_CHEST, Material.SHULKER_BOX, Material.ITEM_FRAME,
        Material.GLOW_ITEM_FRAME, Material.WHITE_BANNER, Material.BOOKSHELF,
        Material.SCAFFOLDING
    )

    @EventHandler(priority = EventPriority.LOW)
    fun onCraftStructureBlock(event: CraftItemEvent) {
        val result = event.recipe?.result ?: return
        if (result.type !in structureGated) return
        val p = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(p) ?: return
        if (CareerManager.hasBranch(cp, Branch.ARCHITECT_STRUCTURE)) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("结构工程师", result.type.name) else gateMsg("结构工程师"))
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceStructureBlock(event: org.bukkit.event.block.BlockPlaceEvent) {
        if (event.block.type !in structureGated) return
        val p = event.player
        val cp = CareerManager.getPlayer(p) ?: return
        if (CareerManager.hasBranch(cp, Branch.ARCHITECT_STRUCTURE)) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("结构工程师", event.block.type.name) else gateMsg("结构工程师"))
    }

    // ========== DEMOLITIONIST GATING (打火石) ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onFlintAndSteel(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        val p = event.player
        if (p.inventory.itemInMainHand.type != Material.FLINT_AND_STEEL &&
            p.inventory.itemInOffHand.type != Material.FLINT_AND_STEEL) return
        if (CareerManager.getPlayer(p)?.let { CareerManager.hasBranch(it, Branch.ARCHITECT_DEMOLITION) } == true) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("爆破师", "打火石") else gateMsg("爆破师"))
    }

    // ========== LUMBERJACK GATING: 非伐木工破坏木头仅50%掉落 ==========

    private val woodTypes = setOf(
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
        Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE
    )

    @EventHandler(priority = EventPriority.LOW)
    fun onBreakWood(event: org.bukkit.event.block.BlockBreakEvent) {
        if (event.block.type !in woodTypes) return
        val p = event.player
        if (CareerManager.getPlayer(p)?.let { CareerManager.hasBranch(it, Branch.WORKER_LUMBERJACK) } == true) return
        // Non-lumberjack: 50% drop chance
        if (DebugManager.rollChance(p, 50, "非伐木工 — 木头掉落50%")) return
        event.isDropItems = false
    }

    // ========== MINER GATING: 非矿工挖石头仅50%掉落 + 高级镐/斧限制 ==========

    private val naturalStone = setOf(
        Material.STONE, Material.DEEPSLATE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
        Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE,
        Material.SANDSTONE, Material.RED_SANDSTONE,
        Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE, Material.END_STONE,
        Material.COBBLESTONE, Material.COBBLED_DEEPSLATE,
        Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.CRYING_OBSIDIAN
    )
    private val highTierPickaxes = setOf(Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)
    private val highTierAxes = setOf(Material.DIAMOND_AXE, Material.NETHERITE_AXE)

    @EventHandler(priority = EventPriority.LOW)
    fun onBreakStone(event: org.bukkit.event.block.BlockBreakEvent) {
        val p = event.player
        val tool = p.inventory.itemInMainHand.type
        val cp = CareerManager.getPlayer(p)

        // Non-miner using diamond/netherite pickaxe → cannot break at all
        if (tool in highTierPickaxes && cp?.let { CareerManager.hasBranch(it, Branch.WORKER_MINER) } != true) {
            event.isCancelled = true
            if (DebugManager.isListening(p.uniqueId))
                p.sendMessage("§7[Debug] 非矿工无法使用高级镐破坏方块")
            return
        }

        // Non-lumberjack using diamond/netherite axe → cannot break at all
        if (tool in highTierAxes && cp?.let { CareerManager.hasBranch(it, Branch.WORKER_LUMBERJACK) } != true) {
            event.isCancelled = true
            if (DebugManager.isListening(p.uniqueId))
                p.sendMessage("§7[Debug] 非伐木工无法使用高级斧破坏方块")
            return
        }

        // Non-miner breaking natural stone → 50% drop
        if (event.block.type in naturalStone && cp?.let { CareerManager.hasBranch(it, Branch.WORKER_MINER) } != true) {
            if (DebugManager.rollChance(p, 50, "非矿工 — 石头掉落50%")) return
            event.isDropItems = false
        }
    }

    // ========== REDSTONE PLACEMENT GATING ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceRedstoneComponent(event: org.bukkit.event.block.BlockPlaceEvent) {
        if (event.block.type !in redstoneGated) return
        val player = event.player
        val cp = CareerManager.getPlayer(player) ?: return
        if (CareerManager.hasBranch(cp, Branch.SCHOLAR_REDSTONE)) return
        event.isCancelled = true
        val debug = DebugManager.isListening(player.uniqueId)
        player.sendMessage(if (debug) debugFail("红石工程师", event.block.type.name) else gateMsg("红石工程师"))
    }

    // ========== SCROLL PROTECTION (grindstone, smithing) ==========

    @EventHandler(priority = EventPriority.LOWEST)
    fun onGrindstoneScroll(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.GRINDSTONE) return
        if (event.slot != 2) return
        if (isScrollProtected(event)) {
            event.isCancelled = true
            (event.whoClicked as? Player)?.sendMessage("§c技能卷轴无法在砂轮中使用！")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onSmithingScroll(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.SMITHING) return
        if (event.slot != 2 && event.slot != 3) return // Result slots
        if (isScrollProtected(event)) {
            event.isCancelled = true
            (event.whoClicked as? Player)?.sendMessage("§c技能卷轴无法在锻造台中使用！")
        }
    }

    // ========== ANVIL ENCHANT COMBINING GATING (附魔师) ==========

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAnvilResult(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.ANVIL) return
        if (event.slot != 2) return // Result slot
        // Block scroll usage
        if (isScrollProtected(event)) {
            event.isCancelled = true
            (event.whoClicked as? Player)?.sendMessage("§c技能卷轴无法在铁砧中使用！")
            return
        }
        val player = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        if (CareerManager.hasBranch(cp, Branch.SCHOLAR_ENCHANTER)) return

        // Check if the anvil operation involves enchant combining (has second item or enchanted book)
        val anvilInv = event.inventory
        val firstItem = anvilInv.getItem(0)
        val secondItem = anvilInv.getItem(1)

        val isEnchantOp = (secondItem != null && secondItem.type != Material.AIR) &&
            (firstItem?.enchantments?.isNotEmpty() == true || secondItem.type == Material.ENCHANTED_BOOK)
        if (!isEnchantOp) return // Allow renaming and simple repairs

        event.isCancelled = true
        val debug = DebugManager.isListening(player.uniqueId)
        player.sendMessage(if (debug) debugFail("附魔师", "铁砧附魔组合") else gateMsg("附魔师"))
    }

    // ========== FURNACE FUEL GATING (烧炼师) ==========

    private val smelterGatedFuels: Set<Material> = setOf(
        Material.COAL, Material.COAL_BLOCK, Material.CHARCOAL,
        Material.LAVA_BUCKET, Material.BLAZE_ROD
    )

    /** Check whether a player is a smelter (returns true if smelter, false with message if not) */
    private fun checkSmelterGate(player: Player, itemName: String): Boolean {
        val cp = CareerManager.getPlayer(player)
        if (cp != null && CareerManager.hasBranch(cp, Branch.WORKER_SMELTER)) return true
        val debug = DebugManager.isListening(player.uniqueId)
        player.sendMessage(if (debug) debugFail("烧炼师", itemName) else gateMsg("烧炼师"))
        return false
    }

    /** Check if player can use this furnace type: smelter→furnace/blast, chef_master→smoker */
    private fun canOperateFurnace(player: Player, invType: org.bukkit.event.inventory.InventoryType, itemName: String): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        return when (invType) {
            org.bukkit.event.inventory.InventoryType.SMOKER -> CareerManager.hasBranch(cp, Branch.CHEF_MASTER)
            else -> CareerManager.hasBranch(cp, Branch.WORKER_SMELTER) // FURNACE, BLAST_FURNACE
        }
    }

    /** Block non-career players from placing fuel into furnaces */
    @EventHandler(priority = EventPriority.LOW)
    fun onFurnaceFuelPlace(event: org.bukkit.event.inventory.InventoryClickEvent) {
        val inv = event.inventory
        val invType = inv.type
        if (invType != org.bukkit.event.inventory.InventoryType.FURNACE &&
            invType != org.bukkit.event.inventory.InventoryType.BLAST_FURNACE &&
            invType != org.bukkit.event.inventory.InventoryType.SMOKER) return
        val player = event.whoClicked as? Player ?: return

        val cursorType = event.cursor?.type
        val currentType = event.currentItem?.type
        val clickedSlot = event.slot

        // Shift-click from player inventory → item moves to appropriate furnace slot
        // If the item is gated fuel, it would go to the fuel slot (slot 1) — block it
        if (event.isShiftClick && clickedSlot != 1) {
            // Shift-click from a non-fuel slot — check if the item is gated fuel
            val shiftedItem = currentType
            if (shiftedItem != null && shiftedItem != Material.AIR && shiftedItem in smelterGatedFuels) {
                if (!canOperateFurnace(player, invType, shiftedItem.name)) {
                    event.isCancelled = true
                    val label = if (invType == org.bukkit.event.inventory.InventoryType.SMOKER) "主厨" else "烧炼师"
                    gateMsg(player, label)
                }
            }
            return
        }

        // Only fuel slot clicks beyond this point
        if (clickedSlot != 1) return

        // Shift-click ON fuel slot → moving fuel OUT (always allow)
        if (event.isShiftClick) return

        // Cursor is empty → taking fuel OUT (always allow)
        if (cursorType == null || cursorType == Material.AIR) return

        // Cursor has gated fuel — placing INTO the fuel slot
        if (cursorType in smelterGatedFuels) {
            // Right-click on same item = split stack (taking half out) → allow
            if (event.isRightClick && currentType == cursorType) return
            // Otherwise = placing fuel → check career
            if (!canOperateFurnace(player, invType, cursorType.name)) {
                event.isCancelled = true
                val label = if (invType == org.bukkit.event.inventory.InventoryType.SMOKER) "主厨" else "烧炼师"
                gateMsg(player, label)
            }
        }
    }

    /** Block non-career players from dragging fuel onto furnace fuel slot */
    @EventHandler(priority = EventPriority.LOW)
    fun onFurnaceFuelDrag(event: org.bukkit.event.inventory.InventoryDragEvent) {
        val inv = event.inventory
        val invType = inv.type
        if (invType != org.bukkit.event.inventory.InventoryType.FURNACE &&
            invType != org.bukkit.event.inventory.InventoryType.BLAST_FURNACE &&
            invType != org.bukkit.event.inventory.InventoryType.SMOKER) return
        if (1 !in event.inventorySlots) return
        if (event.oldCursor.type !in smelterGatedFuels) return
        val player = event.whoClicked as? Player ?: return
        if (!canOperateFurnace(player, invType, event.oldCursor.type.name)) {
            event.isCancelled = true
            val label = if (invType == org.bukkit.event.inventory.InventoryType.SMOKER) "主厨" else "烧炼师"
            gateMsg(player, label)
        }
    }

    /** Block non-career players from right-clicking a furnace while holding fuel */
    @EventHandler(priority = EventPriority.LOW)
    fun onFurnaceInteract(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val isSmoker = block.type == Material.SMOKER
        val isFurnace = block.type == Material.FURNACE || block.type == Material.BLAST_FURNACE
        if (!isSmoker && !isFurnace) return
        val player = event.player
        val held = player.inventory.itemInMainHand
        if (held.type !in smelterGatedFuels) return
        val cp = CareerManager.getPlayer(player)
        val allowed = if (isSmoker) cp?.let { CareerManager.hasBranch(it, Branch.CHEF_MASTER) } == true
            else cp?.let { CareerManager.hasBranch(it, Branch.WORKER_SMELTER) } == true
        if (!allowed) {
            event.isCancelled = true
            val label = if (isSmoker) "主厨" else "烧炼师"
            gateMsg(player, label)
        }
    }

    private fun gateMsg(player: Player, branch: String) {
        val debug = DebugManager.isListening(player.uniqueId)
        player.sendMessage(if (debug) debugFail(branch, "燃料") else gateMsg(branch))
    }

    // ========== BAKER GATING: only bakers craft baked goods & stews ==========

    private val bakerGatedCrafts: Set<Material> = setOf(
        Material.BREAD, Material.CAKE, Material.COOKIE, Material.PUMPKIN_PIE,
        Material.MUSHROOM_STEW, Material.BEETROOT_SOUP, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCraftBakerOnly(event: CraftItemEvent) {
        val result = event.recipe?.result ?: return
        if (result.type !in bakerGatedCrafts) return
        val p = event.whoClicked as? Player ?: return
        val cp = CareerManager.getPlayer(p) ?: return
        if (CareerManager.hasBranch(cp, Branch.CHEF_BAKER)) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("烘焙师", result.type.name) else gateMsg("烘焙师"))
    }

    // ========== 现代化牧业 (Farmer Rancher eureka 1): can place dispensers ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceDispenser(event: org.bukkit.event.block.BlockPlaceEvent) {
        if (event.block.type != Material.DISPENSER) return
        val p = event.player
        val cp = CareerManager.getPlayer(p) ?: return
        if (CareerManager.hasBranch(cp, Branch.FARMER_RANCHER) &&
            cp.chosenEurekas[Branch.FARMER_RANCHER]?.eurekaDef?.id?.contains("rancher_eureka_1") == true) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("牧场主·现代化牧业", "发射器") else gateMsg("牧场主·现代化牧业"))
    }

    // ========== 同行浪迹 (Farmer Merchant eureka 0): wander trader glow + 2 free trades ==========

    private val wanderTraderTradeCount = mutableMapOf<org.bukkit.entity.WanderingTrader, MutableMap<java.util.UUID, Int>>()

    @EventHandler
    fun onWanderTraderNearby(event: org.bukkit.event.player.PlayerMoveEvent) {
        val p = event.player
        if (!p.hasEureka(Branch.FARMER_MERCHANT, 0)) return
        if (event.from.blockX == event.to?.blockX && event.from.blockZ == event.to?.blockZ) return
        for (entity in p.location.getNearbyEntities(16.0, 16.0, 16.0)) {
            if (entity is org.bukkit.entity.WanderingTrader) {
                entity.addPotionEffect(org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, 40, 0, false, false))
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWanderTraderTrade(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.inventory.type != org.bukkit.event.inventory.InventoryType.MERCHANT) return
        if (event.slot != 2) return // result slot
        val p = event.whoClicked as? Player ?: return
        if (!p.hasEureka(Branch.FARMER_MERCHANT, 0)) return
        val merchantInv = event.inventory as? org.bukkit.inventory.MerchantInventory ?: return
        val trader = merchantInv.merchant as? org.bukkit.entity.WanderingTrader ?: return
        val counts = wanderTraderTradeCount.getOrPut(trader) { mutableMapOf() }
        val playerTrades = counts.getOrDefault(p.uniqueId, 0)
        if (playerTrades < 2) {
            // Refund emerald cost for first 2 trades
            val recipe = merchantInv.selectedRecipe ?: return
            val emeraldCost = recipe.ingredients.filter { it.type == Material.EMERALD }.sumOf { it.amount }
            if (emeraldCost > 0) {
                p.inventory.addItem(org.bukkit.inventory.ItemStack(Material.EMERALD, emeraldCost))
                p.sendMessage("§a✦ 同行浪迹：第${playerTrades + 1}次交易不消耗绿宝石")
            }
            counts[p.uniqueId] = playerTrades + 1
        }
    }

    // Note: hoppers / droppers / dispensers are intentionally NOT gated —
    // only manual player placement is restricted to smelters.

    // ========== TOOL DAMAGE GATING: non-career tool users deal half damage ==========

    private val pickaxes = setOf(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)
    private val axes = setOf(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE)
    private val hoes = setOf(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
        Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE)
    private val swords = setOf(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
        Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD)
    private val bows = setOf(Material.BOW, Material.CROSSBOW)

    private fun isWarrior(cp: com.waterful.project.career.model.CareerPlayer): Boolean =
        CareerManager.hasBranch(cp, Branch.WARRIOR_WEAPON) || CareerManager.hasBranch(cp, Branch.WARRIOR_SOLDIER) ||
        CareerManager.hasBranch(cp, Branch.WARRIOR_HUNTER) || CareerManager.hasBranch(cp, Branch.WARRIOR_EXPLORER)

    private fun isWeaponSpecialist(cp: com.waterful.project.career.model.CareerPlayer): Boolean =
        CareerManager.hasBranch(cp, Branch.WARRIOR_WEAPON)

    @EventHandler(priority = EventPriority.LOW)
    fun onToolDamage(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val tool = damager.inventory.itemInMainHand.type
        val cp = CareerManager.getPlayer(damager) ?: return
        val debug = DebugManager.isListening(damager.uniqueId)

        // 武器专家 → all weapons full damage
        if (isWeaponSpecialist(cp)) return

        val gated = when (tool) {
            in pickaxes -> !CareerManager.hasBranch(cp, Branch.WORKER_MINER)
            in axes -> !CareerManager.hasBranch(cp, Branch.WORKER_LUMBERJACK) && !isWarrior(cp)
            in swords -> !isWarrior(cp)
            in bows -> !isWarrior(cp)
            in hoes -> !CareerManager.hasBranch(cp, Branch.FARMER_BOTANIST) &&
                       !CareerManager.hasBranch(cp, Branch.FARMER_RANCHER)
            else -> false
        }
        if (gated) {
            event.damage = event.damage * 0.5
            if (debug) damager.sendMessage("§7[Debug] 非对应职业使用${tool.name}，伤害减半")
        }
    }

    // ========== VILLAGER TRADING (行商) ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onVillagerTrade(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        if (entity.type != EntityType.VILLAGER && entity.type != EntityType.WANDERING_TRADER) return

        val cp = CareerManager.getPlayer(player) ?: return
        val debug = DebugManager.isListening(player.uniqueId)
        if (!CareerManager.hasBranch(cp, Branch.FARMER_MERCHANT)) {
            event.isCancelled = true
            player.sendMessage(if (debug) debugFail("行商", "交易") else gateMsg("行商"))
        } else {
            if (debug) player.sendMessage(debugPass("行商", "交易"))
            // 投资远见 tracking: record emerald spent on trade completion
            // Actual tracking happens on InventoryClickEvent MERCHANT result slot
        }
    }

    // ========== BOTANIST GATING: 树苗/骨粉/堆肥桶 ==========

    private val saplings = setOf(
        Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
        Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
        Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE, Material.BAMBOO_SAPLING,
        Material.AZALEA, Material.FLOWERING_AZALEA
    )

    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceSapling(event: org.bukkit.event.block.BlockPlaceEvent) {
        if (event.block.type !in saplings) return
        val p = event.player
        if (CareerManager.getPlayer(p)?.let { CareerManager.hasBranch(it, Branch.FARMER_BOTANIST) } == true) return
        event.isCancelled = true
        val debug = DebugManager.isListening(p.uniqueId)
        p.sendMessage(if (debug) debugFail("植物学家", "放置树苗") else gateMsg("植物学家"))
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onUseBoneMeal(event: org.bukkit.event.block.BlockPlaceEvent) {
        // Bone meal usage is handled via PlayerInteractEvent, not BlockPlaceEvent.
        // The actual bone meal interaction is a right-click that applies the item.
        // We catch this in the interact handler below.
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onBotanistInteract(event: org.bukkit.event.player.PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val block = event.clickedBlock ?: return
        val held = player.inventory.itemInMainHand
        val cp = CareerManager.getPlayer(player) ?: return
        val debug = DebugManager.isListening(player.uniqueId)

        // Using bone meal on crops/saplings/plants
        if (held.type == Material.BONE_MEAL) {
            if (!CareerManager.hasBranch(cp, Branch.FARMER_BOTANIST)) {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("植物学家", "使用骨粉") else gateMsg("植物学家"))
                return
            }
        }

        // Using composter
        if (block.type == Material.COMPOSTER) {
            if (!CareerManager.hasBranch(cp, Branch.FARMER_BOTANIST)) {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("植物学家", "堆肥桶") else gateMsg("植物学家"))
                return
            }
        }
    }

    // ========== ANIMAL INTERACTION (牧场主) ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onShearEntity(event: PlayerShearEntityEvent) {
        val player = event.player
        val cp = CareerManager.getPlayer(player) ?: return
        val debug = DebugManager.isListening(player.uniqueId)
        if (!CareerManager.hasBranch(cp, Branch.FARMER_RANCHER)) {
            event.isCancelled = true
            player.sendMessage(if (debug) debugFail("牧场主", "剪羊毛") else gateMsg("牧场主"))
        } else {
            if (debug) player.sendMessage(debugPass("牧场主", "剪羊毛"))
            player.giveExp(2) // 剪毛得2经验
        }
    }

    private fun checkRancherGate(player: Player, action: String): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val debug = DebugManager.isListening(player.uniqueId)
        return if (!CareerManager.hasBranch(cp, Branch.FARMER_RANCHER)) {
            player.sendMessage(if (debug) debugFail("牧场主", action) else gateMsg("牧场主"))
            false
        } else {
            if (debug) player.sendMessage(debugPass("牧场主", action))
            true
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onAnimalInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        if (entity !is Animals) return
        if (entity.type == EntityType.VILLAGER || entity.type == EntityType.WANDERING_TRADER) return

        val held = player.inventory.itemInMainHand

        // Milking — 牧场主得2经验
        if (held.type == Material.BUCKET && (entity.type == EntityType.COW || entity.type == EntityType.GOAT)) {
            if (!checkRancherGate(player, "挤奶")) event.isCancelled = true
            else player.giveExp(2)
            return
        }

        // Dyeing sheep — 牧场主得4经验
        if (held.type.name.endsWith("_DYE") && entity.type == EntityType.SHEEP) {
            if (!checkRancherGate(player, "染色")) event.isCancelled = true
            else player.giveExp(4)
            return
        }

        // Breeding — 牧场主得4经验
        if (isBreedingItem(entity.type, held.type)) {
            if (!checkRancherGate(player, "繁殖")) event.isCancelled = true
            else {
                player.giveExp(4)
                // 饲料调配: next feeding → baby grows instantly
                if (entity is org.bukkit.entity.Ageable) {
                    com.waterful.project.career.skill.SkillExecutor.onSiLiaoTiaoPeiFeed(player, entity)
                }
            }
            return
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onAnimalBreed(event: EntityBreedEvent) {
        val breeder = event.breeder as? Player ?: return
        if (!checkRancherGate(breeder, "繁殖")) event.isCancelled = true
    }

    // ========== BLOCK USAGE GATING ==========

    @EventHandler(priority = EventPriority.LOW)
    fun onBlockUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val block = event.clickedBlock ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        val debug = DebugManager.isListening(player.uniqueId)

        when (block.type) {
            // 酿造师 (Chef Brewer) gates brewing stand
            Material.BREWING_STAND -> {
                if (!CareerManager.hasBranch(cp, Branch.CHEF_BREWER)) {
                    event.isCancelled = true
                    player.sendMessage(if (debug) debugFail("药剂师", "酿造台") else gateMsg("药剂师"))
                    return
                } else {
                    if (debug) player.sendMessage(debugPass("药剂师", "酿造台"))
                }
            }
            // 附魔师 (Scholar Enchanter)
            Material.GRINDSTONE -> {
                if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_ENCHANTER)) {
                    event.isCancelled = true
                    player.sendMessage(if (debug) debugFail("附魔师", "砂轮") else gateMsg("附魔师"))
                    return
                } else {
                    if (debug) player.sendMessage(debugPass("附魔师", "砂轮"))
                }
            }
            // Anvil: allow opening for all, gating done on result click (see onAnvilResult)
            // 堡垒/结构工程师
            Material.STONECUTTER -> {
                if (!CareerManager.hasBranch(cp, Branch.ARCHITECT_FORTRESS) &&
                    !CareerManager.hasBranch(cp, Branch.ARCHITECT_STRUCTURE)) {
                    event.isCancelled = true
                    player.sendMessage(if (debug) debugFail("堡垒/结构工程师", "切石机") else gateMsg("堡垒工程师"))
                    return
                } else {
                    if (debug) player.sendMessage(debugPass("堡垒/结构工程师", "切石机"))
                }
            }
            else -> {}
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun gateMsg(branchName: String): Component =
        Component.text("⚠ 需要解锁【$branchName】分支才能执行此操作！", NamedTextColor.RED)

    private fun debugPass(branch: String, action: String): Component =
        Component.text("✓ [$branch] $action — 通过", NamedTextColor.GREEN)

    private fun debugFail(branch: String, action: String): Component =
        Component.text("✗ [$branch] $action — 拒绝（未解锁分支）", NamedTextColor.RED)

    private fun isBreedingItem(entityType: EntityType, item: Material): Boolean {
        return when (entityType) {
            EntityType.COW, EntityType.GOAT, EntityType.MOOSHROOM,
            EntityType.SHEEP -> item == Material.WHEAT
            EntityType.PIG -> item == Material.CARROT || item == Material.POTATO || item == Material.BEETROOT
            EntityType.CHICKEN -> item == Material.WHEAT_SEEDS ||
                item == Material.PUMPKIN_SEEDS || item == Material.MELON_SEEDS ||
                item == Material.BEETROOT_SEEDS
            EntityType.RABBIT -> item == Material.CARROT || item == Material.GOLDEN_CARROT ||
                item == Material.DANDELION
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE -> item == Material.GOLDEN_APPLE ||
                item == Material.ENCHANTED_GOLDEN_APPLE || item == Material.GOLDEN_CARROT
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> item == Material.HAY_BLOCK
            EntityType.WOLF -> item == Material.BEEF || item == Material.COOKED_BEEF ||
                item == Material.PORKCHOP || item == Material.COOKED_PORKCHOP ||
                item == Material.CHICKEN || item == Material.COOKED_CHICKEN ||
                item == Material.MUTTON || item == Material.COOKED_MUTTON ||
                item == Material.RABBIT || item == Material.COOKED_RABBIT ||
                item == Material.ROTTEN_FLESH
            EntityType.CAT, EntityType.OCELOT -> item == Material.COD || item == Material.SALMON
            EntityType.TURTLE -> item == Material.SEAGRASS
            EntityType.PANDA -> item == Material.BAMBOO
            EntityType.FOX -> item == Material.SWEET_BERRIES || item == Material.GLOW_BERRIES
            EntityType.BEE -> item.name.contains("FLOWER") || item == Material.DANDELION ||
                item == Material.POPPY || item == Material.BLUE_ORCHID
            EntityType.HOGLIN -> item == Material.CRIMSON_FUNGUS
            EntityType.STRIDER -> item == Material.WARPED_FUNGUS
            EntityType.AXOLOTL -> item == Material.TROPICAL_FISH_BUCKET
            EntityType.FROG -> item == Material.SLIME_BALL
            EntityType.CAMEL -> item == Material.CACTUS
            EntityType.SNIFFER -> item == Material.TORCHFLOWER_SEEDS
            else -> false
        }
    }
}
