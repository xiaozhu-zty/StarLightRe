package com.waterful.project.career.manager

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionType

/**
 * Registers custom crafting/brewing recipes granted by eurekas and branch passives.
 */
object RecipeManager {

    private val registeredKeys = mutableListOf<NamespacedKey>()

    fun init(plugin: JavaPlugin) {
        registeredKeys.clear()

        // === Brewer Eureka: 下界原住民 — 玻璃瓶 + 下界疣×3 → 粗制的药水 ===
        registerPotionRecipe(
            plugin, "brewer_nether_native", PotionType.AWKWARD, false,
            Material.GLASS_BOTTLE to 1,
            Material.NETHER_WART to 3
        )

        // === Brewer Eureka: 战地药剂师 — 玻璃瓶 + 下界疣×2 + 火药 → 粗制喷溅药水 ===
        registerPotionRecipe(
            plugin, "brewer_field_medic", PotionType.AWKWARD, true,
            Material.GLASS_BOTTLE to 1,
            Material.NETHER_WART to 2,
            Material.GUNPOWDER to 1
        )

        // === Master Eureka: 地狱厨房 — 8金块+1苹果→附魔金苹果 ===
        registerShaped(
            plugin, "master_hell_kitchen", Material.ENCHANTED_GOLDEN_APPLE, 1,
            listOf("GGG", "GAG", "GGG"),
            mapOf('G' to Material.GOLD_BLOCK, 'A' to Material.APPLE)
        )

        // === Scholar Enchanter Eureka: 青金术师 — 8青金石+1玻璃瓶→附魔之瓶 ===
        registerShapeless(
            plugin, "enchanter_lapis_master", Material.EXPERIENCE_BOTTLE, 1,
            Material.LAPIS_LAZULI to 8,
            Material.GLASS_BOTTLE to 1
        )

        // === Scholar Redstone Eureka: 红色炬火 — 火把→红石火把 ===
        registerShapeless(
            plugin, "redstone_red_torch", Material.REDSTONE_TORCH, 1,
            Material.TORCH to 1
        )

        // === Architect Demolition Eureka: 末影硝酸甘油 — 6玻璃+3TNT→末地水晶 ===
        registerShapeless(
            plugin, "demo_nitro_glycerin", Material.END_CRYSTAL, 1,
            Material.GLASS to 6,
            Material.TNT to 3
        )

        // === Worker Smelter Eureka: 煤液化 — 8煤炭+1铁桶→岩浆桶 ===
        registerShapeless(
            plugin, "smelter_coal_liquefy", Material.LAVA_BUCKET, 1,
            Material.COAL to 8,
            Material.BUCKET to 1
        )

        // === Worker Smelter Eureka: 制锭机床 — 3铁块+3金块+3下界合金锭→4下界合金锭 ===
        registerShaped(
            plugin, "smelter_ingot_machine", Material.NETHERITE_INGOT, 4,
            listOf("III", "GGG", "NNN"),
            mapOf('I' to Material.IRON_BLOCK, 'G' to Material.GOLD_BLOCK, 'N' to Material.NETHERITE_INGOT)
        )

        // === Architect Traffic Eureka: 精炼钢轨 ①4铁锭+1木棍→16铁轨 ===
        registerShaped(
            plugin, "traffic_refined_rail_1", Material.RAIL, 16,
            listOf("I I", "ISI", "I I"),
            mapOf('I' to Material.IRON_INGOT, 'S' to Material.STICK)
        )

        // === Architect Traffic Eureka: 精炼钢轨 ②6铜锭+1木棍→16铁轨 ===
        registerShaped(
            plugin, "traffic_refined_rail_2", Material.RAIL, 16,
            listOf("C C", "CSC", "C C"),
            mapOf('C' to Material.COPPER_INGOT, 'S' to Material.STICK)
        )

        // === Architect Structure Eureka: 凝固剂 — concrete powder → concrete (all 16 colors) ===
        val concreteMap = mapOf(
            Material.WHITE_CONCRETE_POWDER to Material.WHITE_CONCRETE,
            Material.ORANGE_CONCRETE_POWDER to Material.ORANGE_CONCRETE,
            Material.MAGENTA_CONCRETE_POWDER to Material.MAGENTA_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE_POWDER to Material.LIGHT_BLUE_CONCRETE,
            Material.YELLOW_CONCRETE_POWDER to Material.YELLOW_CONCRETE,
            Material.LIME_CONCRETE_POWDER to Material.LIME_CONCRETE,
            Material.PINK_CONCRETE_POWDER to Material.PINK_CONCRETE,
            Material.GRAY_CONCRETE_POWDER to Material.GRAY_CONCRETE,
            Material.LIGHT_GRAY_CONCRETE_POWDER to Material.LIGHT_GRAY_CONCRETE,
            Material.CYAN_CONCRETE_POWDER to Material.CYAN_CONCRETE,
            Material.PURPLE_CONCRETE_POWDER to Material.PURPLE_CONCRETE,
            Material.BLUE_CONCRETE_POWDER to Material.BLUE_CONCRETE,
            Material.BROWN_CONCRETE_POWDER to Material.BROWN_CONCRETE,
            Material.GREEN_CONCRETE_POWDER to Material.GREEN_CONCRETE,
            Material.RED_CONCRETE_POWDER to Material.RED_CONCRETE,
            Material.BLACK_CONCRETE_POWDER to Material.BLACK_CONCRETE
        )
        concreteMap.forEach { (powder, concrete) ->
            registerShapeless(plugin, "structure_concrete_${powder.name.lowercase()}", concrete, 1,
                powder to 1)
        }

        plugin.logger.info("[StarLightRe] RecipeManager: registered ${registeredKeys.size} eureka recipes")
    }

    /** Register a potion recipe with correct PotionMeta data */
    private fun registerPotionRecipe(
        plugin: JavaPlugin, name: String, type: PotionType, splash: Boolean,
        vararg ingredients: Pair<Material, Int>
    ) {
        val material = if (splash) Material.SPLASH_POTION else Material.POTION
        val result = ItemStack(material, 1)
        result.editMeta { meta ->
            if (meta is PotionMeta) {
                meta.basePotionType = type
            }
        }

        val key = NamespacedKey(plugin, name)
        val recipe = ShapelessRecipe(key, result)
        for ((mat, count) in ingredients) {
            val choice = RecipeChoice.MaterialChoice(mat)
            repeat(count) { recipe.addIngredient(choice) }
        }
        Bukkit.addRecipe(recipe)
        registeredKeys.add(key)
    }

    private fun registerShapeless(
        plugin: JavaPlugin, name: String, result: Material, amount: Int,
        vararg ingredients: Pair<Material, Int>
    ) {
        val key = NamespacedKey(plugin, name)
        val recipe = ShapelessRecipe(key, ItemStack(result, amount))
        for ((mat, count) in ingredients) {
            val choice = RecipeChoice.MaterialChoice(mat)
            repeat(count) { recipe.addIngredient(choice) }
        }
        Bukkit.addRecipe(recipe)
        registeredKeys.add(key)
    }

    private fun registerShaped(
        plugin: JavaPlugin, name: String, result: Material, amount: Int,
        shape: List<String>, ingredients: Map<Char, Material>
    ) {
        val key = NamespacedKey(plugin, name)
        val recipe = ShapedRecipe(key, ItemStack(result, amount))
        recipe.shape(*shape.toTypedArray())
        for ((char, mat) in ingredients) {
            recipe.setIngredient(char, mat)
        }
        Bukkit.addRecipe(recipe)
        registeredKeys.add(key)
    }

    fun unregister() {
        for (key in registeredKeys) {
            Bukkit.removeRecipe(key)
        }
        registeredKeys.clear()
    }
}
