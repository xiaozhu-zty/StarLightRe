package com.waterful.project.career

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/** Special career items with persistent NBT tags */
object CareerItems {

    const val SCROLL_NBT = "career_scroll"

    fun scrollKey(plugin: JavaPlugin) = NamespacedKey(plugin, SCROLL_NBT)

    /** Create the career skill scroll item */
    fun createScroll(plugin: JavaPlugin): ItemStack {
        val item = ItemStack(Material.ENCHANTED_BOOK)
        item.editMeta { meta ->
            meta.displayName(Component.text("技能卷轴", NamedTextColor.GOLD, TextDecoration.BOLD))
            meta.lore(listOf(
                Component.text("右击打开职业生涯面板", NamedTextColor.GRAY),
                Component.text("", NamedTextColor.WHITE),
                Component.text("Shift+F 也可快速打开", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC)
            ))
            meta.persistentDataContainer.set(scrollKey(plugin), PersistentDataType.STRING, SCROLL_NBT)
        }
        return item
    }

    fun isScroll(plugin: JavaPlugin, item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.get(scrollKey(plugin), PersistentDataType.STRING) == SCROLL_NBT
    }
}
