package com.waterful.project.career.listener

import com.waterful.project.career.*
import com.waterful.project.career.model.Branch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Complete Teacher (教师) branch — based on StarLightCore reference.
 *
 * Mechanics:
 * - Skill Book: custom head item, right-click → +1 skill point (teachers can't use)
 * - Lectern: Shift+Right Click with book → skill book (1 SP + exp levels)
 * - Bookshelf: Shift+Right Click with book → skill book (2/1 SP with 厚积薄发)
 * - 厚积薄发: passive 10/20/30% chance to reduce bookshelf SP cost to 1
 * - Gates: teachers can place/craft bookshelf/lectern; non-teachers can't
 */
class TeacherListener(private val plugin: JavaPlugin) : Listener {

    companion object {
        const val SKILL_BOOK_NBT = "career_skill_book"
        var instance: TeacherListener? = null

        private fun bookKey(plugin: JavaPlugin) =
            NamespacedKey(plugin, SKILL_BOOK_NBT)

        fun createSkillBook(plugin: JavaPlugin): ItemStack {
            val item = ItemStack(Material.PLAYER_HEAD)
            item.editMeta { meta ->
                if (meta is SkullMeta) {
                    meta.displayName(Component.text("技能之书", NamedTextColor.AQUA))
                    meta.lore(listOf(
                        Component.text("右击消耗可获得 1 技能点", NamedTextColor.GRAY),
                        Component.text("注意: 教师分支的玩家无法使用", NamedTextColor.RED)
                    ))
                }
                meta.persistentDataContainer.set(
                    bookKey(plugin), PersistentDataType.STRING, SKILL_BOOK_NBT
                )
            }
            return item
        }

        fun isSkillBook(plugin: JavaPlugin, item: ItemStack): Boolean {
            val meta = item.itemMeta ?: return false
            return meta.persistentDataContainer.get(
                bookKey(plugin), PersistentDataType.STRING
            ) == SKILL_BOOK_NBT
        }
    }

    // ===== GATE: teacher-only blocks =====
    private val teacherBlocks = setOf(Material.BOOKSHELF, Material.CHISELED_BOOKSHELF, Material.LECTERN)

    @EventHandler fun onPlaceTeacherBlock(e: BlockPlaceEvent) {
        if (e.block.type !in teacherBlocks) return
        if (e.player.hasBranch(Branch.SCHOLAR_TEACHER)) return
        e.isCancelled = true; e.player.msgFail("需要解锁【教师】分支！")
    }

    @EventHandler fun onCraftTeacherBlock(e: CraftItemEvent) {
        val r = e.recipe?.result ?: return
        if (r.type !in teacherBlocks) return
        (e.whoClicked as? Player)?.let { p ->
            if (!p.hasBranch(Branch.SCHOLAR_TEACHER)) {
                e.isCancelled = true; p.msgFail("需要解锁【教师】分支！")
            }
        }
    }

    @EventHandler fun onBreakLectern(e: BlockBreakEvent) {
        if (e.block.type != Material.LECTERN) return
        if (e.player.hasBranch(Branch.SCHOLAR_TEACHER)) return
        e.isDropItems = false; e.isCancelled = true
        e.player.msgFail("需要解锁【教师】分支！")
    }

    // ===== SKILL BOOK: consume for skill point =====

    @EventHandler
    fun onUseSkillBook(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_AIR && e.action != Action.RIGHT_CLICK_BLOCK) return
        val p = e.player; val item = p.inventory.itemInMainHand
        if (!isSkillBook(plugin, item)) return
        e.isCancelled = true

        if (p.hasBranch(Branch.SCHOLAR_TEACHER)) {
            p.sendMessage(Component.text("教师不能使用技能之书，请分享给其他玩家！", NamedTextColor.RED)); return
        }
        item.amount -= 1
        p.career()?.let { it.skillPoints += 1 }
        p.msgSuccess("从技能之书中获得了 1 技能点！")
    }

    // ===== LECTERN: Shift+Right Click with book → skill book =====

    @EventHandler
    fun onLecternCraft(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return // Prevent offhand double-fire
        val p = e.player
        if (!p.isSneaking || e.clickedBlock?.type != Material.LECTERN) return
        if (!p.holding(Material.BOOK)) return
        if (!p.hasBranch(Branch.SCHOLAR_TEACHER)) return
        e.isCancelled = true
        val cp = p.career() ?: return

        val eduLv = p.skillLevel(Branch.SCHOLAR_TEACHER, 0)
        val levelCost = when (eduLv) { 1 -> 37; 2 -> 34; 3 -> 30; else -> 40 }

        if (p.level < levelCost || cp.skillPoints < 1) {
            p.msgFail("缺少技能点或经验（需要${levelCost}级经验 + 1技能点）"); return
        }
        cp.skillPoints -= 1; p.level -= levelCost
        p.inventory.itemInMainHand.amount -= 1
        p.giveItem(createSkillBook(plugin))
        p.msgSuccess("消耗 1 技能点和 ${levelCost} 级经验制作了一本技能之书！")
    }

    // ===== BOOKSHELF: Shift+Right Click with book → skill book =====

    @EventHandler
    fun onBookshelfCraft(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return // Prevent offhand double-fire
        val p = e.player; if (!p.isSneaking) return
        val block = e.clickedBlock ?: return
        if (block.type != Material.BOOKSHELF && block.type != Material.CHISELED_BOOKSHELF) return
        if (!p.holding(Material.BOOK)) return
        if (!p.hasBranch(Branch.SCHOLAR_TEACHER)) return
        e.isCancelled = true
        val cp = p.career() ?: return

        var cost = 2
        val thickLv = p.skillLevel(Branch.SCHOLAR_TEACHER, 1)
        if (thickLv >= 1 && p.roll(thickLv * 10, "厚积薄发·Lv.$thickLv 技能点减免")) cost = 1

        if (cp.skillPoints < cost) { p.msgFail("缺少技能点（需要${cost}技能点）"); return }
        cp.skillPoints -= cost
        p.inventory.itemInMainHand.amount -= 1
        p.giveItem(createSkillBook(plugin))
        p.msgSuccess("消耗 $cost 技能点制作了一本技能之书！")
    }

    // ===== 诲人不倦 EUREKA: 20 exp if near lectern =====

    init { instance = this }

    fun castHaiRenBuJuan(player: Player): Boolean {
        val found = (-3..3).any { x -> (-3..3).any { y -> (-3..3).any { z ->
            player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type == Material.LECTERN
        }}}
        return if (found) {
            player.giveExp(20); player.msgSuccess("诲人不倦：获得 20 点经验！"); true
        } else {
            player.msgFail("诲人不倦释放失败，周围没有讲台"); false
        }
    }
}
