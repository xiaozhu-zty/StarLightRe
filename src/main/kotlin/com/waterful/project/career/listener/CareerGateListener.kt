package com.waterful.project.career.listener

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
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.player.*

/**
 * Permission-gating listener: blocks actions that require specific career branches.
 * "可以XXX" in the design doc means WITHOUT that branch, you CANNOT do it.
 * When DebugManager.isListening is on for a player, ALL checks output pass/fail messages.
 */
class CareerGateListener : Listener {

    // ========== RECIPE GATING ==========

    private val redstoneGatedRecipes: Set<Material> = setOf(
        Material.HOPPER, Material.DISPENSER, Material.DROPPER
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

    @EventHandler(priority = EventPriority.LOW)
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val result = event.recipe?.result ?: return
        val cp = CareerManager.getPlayer(player) ?: return
        val resultType = result.type
        val debug = DebugManager.isListening(player.uniqueId)

        // Check redstone-engineered items
        if (resultType in redstoneGatedRecipes) {
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

        // Check demolitionist items
        if (resultType in demoGatedRecipes) {
            if (CareerManager.hasBranch(cp, Branch.ARCHITECT_DEMOLITION)) {
                if (debug) player.sendMessage(debugPass("爆破师", resultType.name))
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
    }

    // ========== FURNACE FUEL GATING (烧炼师) ==========

    private val smelterGatedFuels: Set<Material> = setOf(
        Material.COAL, Material.COAL_BLOCK, Material.CHARCOAL,
        Material.LAVA_BUCKET, Material.BLAZE_ROD
    )

    @EventHandler
    fun onFurnaceBurn(event: FurnaceBurnEvent) {
        if (event.block.type != Material.FURNACE && event.block.type != Material.BLAST_FURNACE) return
        val fuel = event.fuel
        if (fuel.type !in smelterGatedFuels) return

        val loc = event.block.location
        val nearbyPlayers = loc.getNearbyPlayers(5.0)
        for (player in nearbyPlayers) {
            val cp = CareerManager.getPlayer(player) ?: continue
            val debug = DebugManager.isListening(player.uniqueId)
            if (!CareerManager.hasBranch(cp, Branch.WORKER_SMELTER)) {
                event.isCancelled = true
                player.sendMessage(if (debug) debugFail("烧炼师", fuel.type.name) else gateMsg("烧炼师"))
                return
            } else {
                if (debug) player.sendMessage(debugPass("烧炼师", fuel.type.name))
            }
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

        // Milking
        if (held.type == Material.BUCKET && (entity.type == EntityType.COW || entity.type == EntityType.GOAT)) {
            if (!checkRancherGate(player, "挤奶")) event.isCancelled = true
            return
        }

        // Dyeing sheep
        if (held.type.name.endsWith("_DYE") && entity.type == EntityType.SHEEP) {
            if (!checkRancherGate(player, "染色")) event.isCancelled = true
            return
        }

        // Breeding
        if (isBreedingItem(entity.type, held.type)) {
            if (!checkRancherGate(player, "繁殖")) event.isCancelled = true
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
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL -> {
                if (!CareerManager.hasBranch(cp, Branch.SCHOLAR_ENCHANTER)) {
                    val held = player.inventory.itemInMainHand
                    if (held.type != Material.AIR) {
                        event.isCancelled = true
                        player.sendMessage(if (debug) debugFail("附魔师", "铁砧") else gateMsg("附魔师"))
                        return
                    }
                } else {
                    if (debug) player.sendMessage(debugPass("附魔师", "铁砧"))
                }
            }
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
