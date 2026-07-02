package com.waterful.project.stamina

import kotlin.math.max
import kotlin.math.min

/**
 * Player stamina data — ported from StarLightCore.
 * Range: 0.0 ~ 2000.0, starts at max.
 */
data class StaminaData(var stamina: Double = 2000.0) {

    fun add(amount: Double) {
        stamina = min(stamina + amount, 2000.0)
    }

    fun take(amount: Double) {
        stamina = max(stamina - amount, 0.0)
    }

    fun set(amount: Double) {
        stamina = max(min(amount, 2000.0), 0.0)
    }

    /** Color-coded display string using legacy color codes */
    fun display(): String {
        return when {
            stamina >= 1750.0 -> "§a${"%.1f".format(stamina)}"
            stamina >= 1000.0 -> "§2${"%.1f".format(stamina)}"
            stamina >= 750.0  -> "§e${"%.1f".format(stamina)}"
            stamina >= 500.0  -> "§6${"%.1f".format(stamina)}"
            stamina >= 250.0  -> "§c${"%.1f".format(stamina)}"
            stamina > 0.0     -> "§4${"%.1f".format(stamina)}"
            else              -> "§7${"%.1f".format(stamina)}"
        }
    }
}
