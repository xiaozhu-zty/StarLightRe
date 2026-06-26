package com.waterful.project.career

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.DebugManager
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

// =============================================================================
// Player career extensions — TabooLib-style fluent API
// =============================================================================

/** Get the CareerPlayer data for this player */
fun Player.career(): CareerPlayer? = CareerManager.getPlayer(this)

/** Check if the player meets a branch+skill level requirement */
fun Player.meetRequirement(branch: Branch, skillIndex: Int = 0, level: Int = 1): Boolean {
    val cp = career() ?: return false
    val skill = cp.getSkill(branch, skillIndex) ?: return false
    return cp.unlockedBranches.containsKey(branch) && skill.currentLevel >= level
}

/** Check if player meets a requirement for a named branch (by enum name) */
fun Player.hasBranch(branch: Branch): Boolean {
    val cp = career() ?: return false
    return CareerManager.hasBranch(cp, branch)
}

/** Get the level of a specific skill in a branch */
fun Player.skillLevel(branch: Branch, index: Int): Int {
    val cp = career() ?: return 0
    return cp.getSkill(branch, index)?.currentLevel ?: 0
}

/** Get the total branch level */
fun Player.branchLevel(branch: Branch): Int {
    val cp = career() ?: return 0
    return cp.getBranchLevel(branch)
}

/** Check if player has a eureka unlocked on a branch */
fun Player.hasEureka(branch: Branch, eurekaIndex: Int = -1): Boolean {
    val cp = career() ?: return false
    val eureka = cp.chosenEurekas[branch] ?: return false
    if (eurekaIndex < 0) return true
    return eureka.eurekaDef.id.endsWith("_eureka_${eurekaIndex + 1}")
}

// =============================================================================
// Combat / entity extensions
// =============================================================================

/** Apply true damage that bypasses armor */
fun LivingEntity.realDamage(amount: Double, source: org.bukkit.entity.Entity? = null) {
    health = maxOf(0.1, health - amount)
    if (source != null) damage(0.5, source)
}

/** Apply a potion effect with ticks */
fun LivingEntity.effect(type: PotionEffectType, seconds: Int, amplifier: Int = 0) {
    addPotionEffect(PotionEffect(type, seconds * 20, amplifier, false, true, true))
}

// =============================================================================
// Inventory / Item extensions
// =============================================================================

/** Give an item to player, dropping overflow on the ground */
fun Player.giveItem(item: ItemStack) {
    val leftover = inventory.addItem(item)
    leftover.values.forEach { world.dropItemNaturally(location, it) }
}

/** Check if the main hand item matches a material */
fun Player.holding(mat: Material): Boolean = inventory.itemInMainHand.type == mat

/** Check if main hand is empty/air */
fun Player.handEmpty(): Boolean = inventory.itemInMainHand.type == Material.AIR

// =============================================================================
// Debug / testing extensions
// =============================================================================

/** Check if listening mode is on for this player */
fun Player.isListening(): Boolean = DebugManager.isListening(uniqueId)

/** Roll a chance and output debug if listening */
fun Player.roll(percent: Int, description: String): Boolean =
    DebugManager.rollChance(this, percent, description)

/** Send a formatted success message */
fun Player.msgSuccess(text: String) =
    sendMessage(Component.text("§a$text"))

/** Send a formatted failure message */
fun Player.msgFail(text: String) =
    sendMessage(Component.text("§c$text"))
