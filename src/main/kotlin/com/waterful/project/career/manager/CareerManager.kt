package com.waterful.project.career.manager

import com.waterful.project.career.data.ConfigManager
import com.waterful.project.career.data.PlayerDataStore
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.CareerClass
import com.waterful.project.career.model.CareerPlayer
import com.waterful.project.career.model.EurekaDef
import com.waterful.project.career.model.EurekaInstance
import com.waterful.project.career.model.ResonanceMode
import com.waterful.project.career.skill.SkillExecutor
import com.waterful.project.career.model.SkillDef
import com.waterful.project.career.model.SkillInstance
import com.waterful.project.career.model.SkillType
import org.bukkit.entity.Player
import java.util.UUID

object CareerManager {

    private var dataStore: PlayerDataStore? = null
    private var config: ConfigManager? = null

    fun init(dataStore: PlayerDataStore, configManager: ConfigManager) {
        this.dataStore = dataStore
        this.config = configManager
    }

    // ===== Player Management =====

    fun loadPlayer(uuid: UUID, name: String): CareerPlayer =
        dataStore!!.loadPlayer(uuid, name)

    fun savePlayer(player: CareerPlayer) {
        dataStore!!.savePlayer(player)
    }

    fun getPlayer(uuid: UUID): CareerPlayer? =
        dataStore!!.getCachedPlayer(uuid)

    fun getPlayer(player: Player): CareerPlayer? =
        dataStore!!.getCachedPlayer(player.uniqueId)

    fun unloadPlayer(uuid: UUID) {
        dataStore!!.unloadPlayer(uuid)
    }

    // ===== Class Operations =====

    /** Assign random classes to a player on join/respawn */
    fun assignRandomClasses(player: Player): List<CareerClass> {
        val cp = getPlayer(player) ?: return emptyList()

        // If already selected, return current classes
        if (cp.isCareerSelected) return cp.selectedClasses.toList()

        val now = System.currentTimeMillis()
        val classCount = cp.antiFarmData.getRandomClassCount(now)
        val available = CareerClass.entries.toMutableList()
        available.shuffle()

        val assigned = available.take(classCount).toMutableList()
        cp.selectedClasses.clear()
        cp.selectedClasses.addAll(assigned)

        // First time = natural rebirth bonus
        if (cp.antiFarmData.lastNaturalRebirthTime == 0L) {
            cp.skillPoints += 3
            cp.antiFarmData.markNaturalRebirth(now)
        }

        return assigned
    }

    /** Player selects their 3rd class (after 2 random ones assigned) */
    fun selectThirdClass(player: Player, chosen: CareerClass): Boolean {
        val cp = getPlayer(player) ?: return false

        if (cp.isCareerSelected) {
            player.sendMessage("§c你已经完成了职业选择！")
            return false
        }

        if (cp.selectedClasses.contains(chosen)) {
            player.sendMessage("§c该职业已被分配给你，请选择其他职业！")
            return false
        }

        cp.selectedClasses.add(chosen)

        // First-time natural rebirth skill points
        if (cp.skillPoints == 0) {
            cp.skillPoints += 3
        }

        player.sendMessage("§a✦ 已选择职业：${chosen.displayName}！")
        player.sendMessage("§a你的职业组合：${cp.selectedClasses.joinToString("、") { it.displayName }}")
        return true
    }

    /** Get classes not yet selected by the player */
    fun getAvailableClasses(cp: CareerPlayer): List<CareerClass> =
        CareerClass.entries.filter { !cp.selectedClasses.contains(it) }

    /** Get classes that have been randomly assigned (not the 3rd one yet) */
    fun getAssignedClasses(cp: CareerPlayer): List<CareerClass> =
        cp.selectedClasses.toList()

    // ===== Branch Operations =====

    /** Check if a branch can be unlocked */
    fun canUnlockBranch(cp: CareerPlayer, branch: Branch): Boolean {
        if (!cp.selectedClasses.contains(branch.careerClass)) return false
        if (cp.unlockedBranches.containsKey(branch)) return false
        if (!cp.canUnlockMoreBranches()) return false
        if (!SkillPointManager.hasEnough(cp, 1)) return false
        return true
    }

    /** Unlock a branch (costs 1 skill point, gives lvl.0) */
    fun unlockBranch(player: Player, branch: Branch): Boolean {
        val cp = getPlayer(player) ?: return false

        if (!canUnlockBranch(cp, branch)) {
            val reasons = mutableListOf<String>()
            if (!cp.selectedClasses.contains(branch.careerClass)) reasons.add("未拥有该职业")
            if (cp.unlockedBranches.containsKey(branch)) reasons.add("该分支已解锁")
            if (!cp.canUnlockMoreBranches()) reasons.add("已达分支上限(${CareerPlayer.MAX_BRANCHES}个)")
            if (!SkillPointManager.hasEnough(cp, 1)) reasons.add("技能点不足")
            player.sendMessage("§c无法解锁分支：${reasons.joinToString("，")}")
            return false
        }

        if (!SkillPointManager.spendPoints(cp, 1)) return false

        // Create skill instances for this branch — load defs from career_data.yml
        val skills = mutableListOf<SkillInstance>()
        for (i in 0 until Branch.SKILLS_PER_BRANCH) {
            val skillId = SkillDef.makeId(branch, i)
            val loadedDef = com.waterful.project.career.data.CareerDataLoader.getSkill(skillId)
            val skillDef = loadedDef ?: SkillDef(
                id = skillId,
                name = "${branch.displayName}技能${i + 1}",
                branch = branch,
                index = i,
                skillType = if (i == Branch.getPassiveSkillIndex()) SkillType.PASSIVE else SkillType.ACTIVE,
                effectId = skillId
            )
            skills.add(SkillInstance(skillDef = skillDef, currentLevel = 0))
        }
        cp.unlockedBranches[branch] = skills
        cp.resonanceModes[branch] = ResonanceMode.DISABLED

        player.sendMessage("§a✦ 已解锁分支：${branch.displayName}（${branch.careerClass.displayName}）")
        player.sendMessage("§7获得基础效果：${branch.baseEffect}")
        return true
    }

    /** Forget a branch (costs 2*level + 2 skill points) */
    fun forgetBranch(player: Player, branch: Branch): Boolean {
        val cp = getPlayer(player) ?: return false
        val branchSkills = cp.unlockedBranches[branch] ?: run {
            player.sendMessage("§c你尚未解锁该分支！")
            return false
        }

        val cost = cp.getForgetCost(branch)
        if (!SkillPointManager.hasEnough(cp, cost)) {
            player.sendMessage("§c遗忘该分支需要${cost}技能点（当前：${cp.skillPoints}）")
            return false
        }

        if (!SkillPointManager.spendPoints(cp, cost)) return false

        cp.unlockedBranches.remove(branch)
        cp.chosenEurekas.remove(branch)
        cp.resonanceModes.remove(branch)
        // Clean up auto-cast and cooldowns for this branch
        cp.autoCastSkills.keys.removeAll { it.startsWith(branch.name) }
        cp.cooldowns.keys.removeAll { it.startsWith(branch.name) }

        player.sendMessage("§a✦ 已遗忘分支：${branch.displayName}（花费${cost}技能点）")
        return true
    }

    // ===== Skill Operations =====

    /** Get upgrade cost for a skill level */
    fun getSkillUpgradeCost(currentLevel: Int): Int = 1

    /** Level up a skill in a branch */
    fun levelUpSkill(player: Player, branch: Branch, skillIndex: Int): Boolean {
        val cp = getPlayer(player) ?: return false
        val skill = cp.getSkill(branch, skillIndex) ?: run {
            player.sendMessage("§c找不到该技能！")
            return false
        }

        if (skill.isMaxed) {
            player.sendMessage("§c该技能已满级！")
            return false
        }

        val cost = config?.skillLevelUpCost ?: 1
        if (!SkillPointManager.hasEnough(cp, cost)) {
            player.sendMessage("§c技能点不足，升级需要${cost}技能点！")
            return false
        }

        if (!SkillPointManager.spendPoints(cp, cost)) return false

        skill.currentLevel++
        val newLevel = skill.currentLevel

        player.sendMessage("§a✦ ${skill.skillDef.name} 升级至 §eLv.$newLevel")

        // Apply passive skill immediately if it just became active
        if (skill.skillDef.skillType == SkillType.PASSIVE && newLevel >= 1) {
            SkillExecutor.executePassiveSkill(player, skill)
        }

        return true
    }

    /** Check if player can unlock a eureka (all skills at lvl.3) */
    fun canChooseEureka(cp: CareerPlayer, branch: Branch): Boolean {
        if (!cp.isBranchMaxed(branch)) return false
        if (cp.chosenEurekas.containsKey(branch)) return false
        return cp.getBranchLevel(branch) >= (Branch.SKILLS_PER_BRANCH * cp.currentSkillMaxLevel())
    }

    private fun CareerPlayer.currentSkillMaxLevel(): Int = 3

    /** Get eureka options for a branch — loaded from career_data.yml */
    fun getEurekaOptions(branch: Branch): List<EurekaDef> {
        val loaded = com.waterful.project.career.data.CareerDataLoader.getEurekasForBranch(branch)
        if (loaded.isNotEmpty()) return loaded
        // Fallback: generate placeholder defs
        return (1..Branch.EUREKAS_PER_BRANCH).map { i ->
            EurekaDef(
                id = "${branch.name.lowercase()}_eureka_$i",
                name = "${branch.displayName}顿悟$i",
                displayName = "${branch.displayName}顿悟$i",
                description = "顿悟描述待配置",
                branch = branch,
                effectId = "${branch.name.lowercase()}_eureka_$i",
                skillType = SkillType.PASSIVE
            )
        }
    }

    /** Choose a eureka for a branch */
    fun chooseEureka(player: Player, branch: Branch, eurekaIndex: Int): Boolean {
        val cp = getPlayer(player) ?: return false

        if (!cp.isBranchMaxed(branch)) {
            player.sendMessage("§c需要该分支所有技能达到满级才能解锁顿悟！")
            return false
        }

        if (cp.chosenEurekas.containsKey(branch)) {
            player.sendMessage("§c该分支已解锁过顿悟！遗忘分支后才可重新选择。")
            return false
        }

        val eurekaOption = getEurekaOptions(branch).getOrNull(eurekaIndex) ?: return false

        // Special eurekas: grant 3 skill points instead of costing, require another branch in same class
        if (cp.isSpecialEureka(eurekaOption.name)) {
            if (!cp.hasOtherBranchInClass(branch)) {
                player.sendMessage("§c请先解锁同职业下的另一个分支！")
                return false
            }
            SkillPointManager.addPoints(cp, 3)
            cp.chosenEurekas[branch] = EurekaInstance(eurekaDef = eurekaOption)
            player.sendMessage("§5✦ 已解锁顿悟：${eurekaOption.name}！§a额外获得 3 技能点！")
            return true
        }

        // Normal eurekas: costs 1 skill point
        val cost = config?.skillLevelUpCost ?: 1
        if (!SkillPointManager.hasEnough(cp, cost)) {
            player.sendMessage("§c技能点不足，解锁顿悟需要${cost}技能点！")
            return false
        }

        if (!SkillPointManager.spendPoints(cp, cost)) return false
        cp.chosenEurekas[branch] = EurekaInstance(eurekaDef = eurekaOption)

        player.sendMessage("§5✦ 已解锁顿悟：${eurekaOption.name}！")
        player.sendMessage("§7分支等级提升至：Lv.${cp.getBranchLevel(branch)}")
        return true
    }

    // ===== Resonance Operations =====

    /** Set resonance mode for a branch */
    fun setResonanceMode(player: Player, branch: Branch, mode: ResonanceMode): Boolean {
        val cp = getPlayer(player) ?: return false

        if (!cp.unlockedBranches.containsKey(branch)) {
            player.sendMessage("§c你尚未解锁该分支！")
            return false
        }

        cp.resonanceModes[branch] = mode
        player.sendMessage("§a✦ 分支 ${branch.displayName} 的共鸣模式已设置为：§e${mode.displayName}")
        return true
    }

    /** Get all unlocked branches with their levels */
    fun getUnlockedBranches(cp: CareerPlayer): Map<Branch, Int> =
        cp.unlockedBranches.mapValues { cp.getBranchLevel(it.key) }

    /** Check if player owns a specific branch */
    fun hasBranch(cp: CareerPlayer, branch: Branch): Boolean =
        cp.unlockedBranches.containsKey(branch)

    /** Get resonance-enabled branches for a player */
    fun getResonanceBranches(cp: CareerPlayer): List<Branch> =
        cp.resonanceModes
            .filter { it.value != ResonanceMode.DISABLED }
            .keys
            .toList()
}
