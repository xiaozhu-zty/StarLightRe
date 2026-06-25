package com.waterful.project.career.listener

import com.waterful.project.career.gui.BranchGUI
import com.waterful.project.career.gui.CareerGUI
import com.waterful.project.career.gui.ClassGUI
import com.waterful.project.career.gui.DelCareerGUI
import com.waterful.project.career.gui.EurekaGUI
import com.waterful.project.career.gui.ResonanceGUI
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class CareerGUIListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inv = event.inventory ?: return
        val title = event.view.title()

        // Convert Component title to plain text
        val titleText = PlainTextComponentSerializer.plainText().serialize(title)

        // Always cancel clicks in career GUIs to prevent item stealing
        if (!titleText.contains("生涯") && !titleText.contains("职业") && !titleText.contains("分支")
            && !titleText.contains("顿悟") && !titleText.contains("共鸣") && !titleText.contains("重置")) {
            return
        }

        event.isCancelled = true

        if (event.currentItem == null) return

        val slot = event.slot

        // Dispatch based on title
        when {
            titleText.startsWith(CareerGUI.TITLE) -> {
                CareerGUI.handleClick(player, slot, inv)
            }
            titleText.startsWith(ClassGUI.TITLE_PREFIX) -> {
                val className = titleText.removePrefix(ClassGUI.TITLE_PREFIX).trim()
                val careerClass = findClassByName(className) ?: return
                ClassGUI.handleClick(player, slot, inv, careerClass)
            }
            titleText.startsWith(BranchGUI.TITLE_PREFIX) -> {
                val branchName = titleText.removePrefix(BranchGUI.TITLE_PREFIX).trim()
                val branch = findBranchByName(branchName) ?: return
                BranchGUI.handleClick(player, slot, inv, branch)
            }
            titleText.startsWith(EurekaGUI.TITLE_PREFIX) -> {
                val branchName = titleText.removePrefix(EurekaGUI.TITLE_PREFIX).trim()
                val branch = findBranchByName(branchName) ?: return
                EurekaGUI.handleClick(player, slot, inv, branch)
            }
            titleText.startsWith(ResonanceGUI.TITLE_PREFIX) -> {
                val branchName = titleText.removePrefix(ResonanceGUI.TITLE_PREFIX).trim()
                val branch = findBranchByName(branchName) ?: return
                ResonanceGUI.handleClick(player, slot, inv, branch)
            }
            titleText.startsWith("重置管理") -> {
                DelCareerGUI.handleClick(player, slot, inv)
            }
        }
    }

    private fun findBranchByName(displayName: String): Branch? {
        return Branch.entries.find { it.displayName == displayName }
    }

    private fun findClassByName(displayName: String): CareerClass? {
        return CareerClass.entries.find { it.displayName == displayName }
    }
}
