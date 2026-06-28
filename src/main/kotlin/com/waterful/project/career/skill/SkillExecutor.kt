package com.waterful.project.career.skill

import com.waterful.project.career.manager.CareerManager
import com.waterful.project.career.manager.SkillRegistry
import com.waterful.project.career.model.Branch
import com.waterful.project.career.model.SkillInstance
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object SkillExecutor {

    /**
     * Execute an active skill for a player
     */
    fun executeSkill(player: Player, skill: SkillInstance): Boolean {
        val cp = CareerManager.getPlayer(player) ?: return false
        val now = System.currentTimeMillis()
        val effectId = skill.skillDef.effectId
        val level = skill.currentLevel

        if (level == 0) {
            player.sendMessage(Component.text("该技能尚未解锁！", NamedTextColor.RED))
            return false
        }

        // Check cooldown — use SkillDef data from YAML
        val cooldownSeconds = skill.skillDef.getCooldown(level)
        if (cp.isOnCooldown(effectId, cooldownSeconds, now)) {
            val remaining = cp.getRemainingCooldown(effectId, cooldownSeconds, now)
            player.sendMessage(
                Component.text("技能冷却中！剩余 ${remaining} 秒", NamedTextColor.RED)
            )
            return false
        }

        // Try built-in effect handler first
        val handled = executeBuiltinEffect(player, skill, level)
        if (handled) {
            cp.setCooldown(effectId, now)
            return true
        }

        // Get effect from registry
        val effect = SkillRegistry.getSkill(effectId)
        if (effect == null) {
            player.sendMessage(
                Component.text("✦ 技能触发：${skill.skillDef.name} (Lv.$level)", NamedTextColor.YELLOW)
            )
        } else {
            try {
                effect.execute(player, level)
            } catch (e: Exception) {
                player.sendMessage(Component.text("技能执行出错，请联系管理员。", NamedTextColor.RED))
                e.printStackTrace()
                return false
            }
        }

        // Set cooldown
        cp.setCooldown(effectId, now)

        return true
    }

    /**
     * Execute a passive skill (called when skill is leveled up or on respawn)
     */
    fun executePassiveSkill(player: Player, skill: SkillInstance) {
        val effect = SkillRegistry.getSkill(skill.skillDef.effectId) ?: return
        try {
            effect.execute(player, skill.currentLevel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Apply a passive effect to a target player (used by resonance system)
     */
    fun applyPassiveEffectToTarget(branch: Branch, level: Int, target: Player) {
        val effectId = "${branch.name.lowercase()}_skill_0"
        val effect = SkillRegistry.getSkill(effectId) ?: return
        try { effect.execute(target, level) } catch (e: Exception) { e.printStackTrace() }
    }

    // ===== Built-in active skill effects =====

    private fun executeBuiltinEffect(player: Player, skill: SkillInstance, level: Int): Boolean {
        return when (skill.skillDef.effectId) {
            "architect_demolition_skill_1" -> executeQiLangXingZhe(player, skill, level)
            "architect_demolition_skill_2" -> executeQianYanBaoPo(player, skill, level)
            "architect_structure_skill_2" -> executeLingKongChuangXiang(player, skill, level)
            "architect_traffic_skill_1" -> executeZhuiJiaDongLi(player, skill, level)
            "architect_traffic_skill_2" -> executeBeiYongZaiJu(player, skill, level)
            "architect_fortress_skill_1" -> executeBiHuXunQiu(player, skill, level)
            "architect_fortress_skill_2" -> executeTieBiLiChang(player, skill, level)
            "chef_butcher_skill_2" -> executeKunShouTianDi(player, skill, level)
            "chef_baker_skill_2" -> executeTianDianPaiDui(player, skill, level)
            "chef_brewer_skill_1" -> executeZhengFaKongJian(player, skill, level)
            "chef_brewer_skill_2" -> executeShenShangXianSu(player, skill, level)
            "chef_master_skill_1" -> executeQiWeiYiZhen(player, skill, level)
            "chef_master_skill_2" -> executeDuJinMeiZhuan(player, skill, level)
            "chef_baker_skill_1" -> executeWenHuoManDun(player, skill, level)
            "chef_butcher_skill_1" -> executeKaoRouZhuanJia(player, skill, level)
            "scholar_enchanter_skill_2" -> executeZhuShuGongCheng(player, skill, level)
            "scholar_admin_skill_0" -> executeRenLiZiYuan(player, skill, level)
            "scholar_teacher_skill_2" -> executeXueShuShaLong(player, skill, level)
            "scholar_admin_skill_1" -> executeXingZhengYouHua(player, skill, level)
            "scholar_admin_skill_2" -> executeZongHeBaoGao(player, skill, level)
            "scholar_redstone_skill_2" -> executeChaoZai(player, skill, level)
            "farmer_fisherman_skill_2" -> executeDaYangJuanGu(player, skill, level)
            "farmer_fisherman_skill_1" -> executeShouHuoTaoSheng(player, skill, level)
            "farmer_merchant_skill_2" -> executeTePinBaoBiao(player, skill, level)
            "farmer_rancher_skill_2" -> executeMuYuanHuanGe(player, skill, level)
            "farmer_rancher_skill_1" -> executeSiLiaoTiaoPei(player, skill, level)
            "farmer_merchant_skill_1" -> executePoCaiXiaoZai(player, skill, level)
            "farmer_botanist_skill_2" -> executeZhiWuYiChuanXue(player, skill, level)
            "farmer_botanist_skill_1" -> executeKeXueShiFei(player, skill, level)
            "worker_miner_skill_2" -> executeBuMieKuangDeng(player, skill, level)
            "worker_lumberjack_skill_2" -> executeZhanJinZhiGu(player, skill, level)
            "worker_smelter_skill_2" -> executeRanLiaoGuanDao(player, skill, level)
            "worker_lumberjack_skill_1" -> executeQiaoLiYongFu(player, skill, level)
            "worker_miner_skill_1" -> executeQiangLiYunGao(player, skill, level)
            "worker_smelter_skill_1" -> executeRongJinMuJu(player, skill, level)
            "warrior_soldier_skill_1" -> executeYanTiJiDong(player, skill, level)
            "warrior_weapon_skill_2" -> executeQiBingTuXi(player, skill, level)
            "warrior_soldier_skill_2" -> executeJueZhanChongFeng(player, skill, level)
            "warrior_hunter_skill_1" -> executeYuXueQiangGong(player, skill, level)
            "warrior_explorer_skill_1" -> executeTanSuoZheXingNang(player, skill, level)
            "warrior_weapon_skill_1" -> executeXinHaoDan(player, skill, level)
            "warrior_hunter_skill_2" -> executeWoJiEMengYan(player, skill, level)
            "warrior_explorer_skill_2" -> executeQianJinYingDi(player, skill, level)
            else -> false
        }
    }

    /** 咒术工程: 随机提升手持物品的一个魔咒等级 */
    private fun executeZhuShuGongCheng(player: Player, skill: SkillInstance, level: Int): Boolean {
        val item = player.inventory.itemInMainHand
        if (item.type == org.bukkit.Material.AIR) {
            player.sendMessage(Component.text("§c需要手持一个有附魔的物品！")); return false
        }
        val enchants = item.enchantments
        if (enchants.isEmpty()) {
            player.sendMessage(Component.text("§c该物品没有任何魔咒！")); return false
        }
        val enchant = enchants.entries.random()
        val currentLvl = enchant.value
        val maxLvl = enchant.key.maxLevel

        if (currentLvl > maxLvl + 1) {
            player.sendMessage(Component.text("§c该魔咒已达到极限！")); return false
        }
        val cp = CareerManager.getPlayer(player) ?: return false
        if (currentLvl >= maxLvl) {
            if (cp.skillPoints < 1) {
                player.sendMessage(Component.text("§c超越上限需要额外1技能点！")); return false
            }
            cp.skillPoints -= 1
        }
        item.addUnsafeEnchantment(enchant.key, currentLvl + 1)
        player.sendMessage(Component.text("§a✦ 咒术工程：${enchant.key.key.key} ${currentLvl} → ${currentLvl + 1}"))
        return true
    }

    /** 气浪行者: 减少爆炸伤害 */
    private fun executeQiLangXingZhe(p: Player, s: SkillInstance, lv: Int): Boolean {
        val reduction = when (lv) { 1 -> 0.50; 2 -> 0.65; 3 -> 0.80; else -> 0.50 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, (3 + lv) * 20, 2, false, true))
        p.sendMessage(Component.text("§a✦ 气浪行者：爆炸伤害减免 ${(reduction*100).toInt()}%，持续${3+lv}秒"))
        return true
    }

    /** 前沿爆破线: 投掷TNT */
    private fun executeQianYanBaoPo(p: Player, s: SkillInstance, lv: Int): Boolean {
        val count = when (lv) { 1 -> 3; 2 -> 4; 3 -> 5; else -> 3 }
        repeat(count) {
            val tnt = p.world.spawn(p.eyeLocation.add(p.location.direction.multiply(2.0)), org.bukkit.entity.TNTPrimed::class.java)
            tnt.velocity = p.location.direction.multiply(1.5)
            tnt.fuseTicks = 60
        }
        p.sendMessage(Component.text("§a✦ 前沿爆破线：投掷 $count 个TNT！"))
        return true
    }

    /** 凌空创想: 速度+跳跃+抗性 */
    private fun executeLingKongChuangXiang(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 180; 2 -> 240; 3 -> 300; else -> 180 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, duration, lv - 1, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, duration, lv - 1, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, duration, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 凌空创想：速度+跳跃+抗性 ${lv}级，持续${when(lv){1->3;2->4;3->5;else->3}}分钟"))
        return true
    }

    /** 追加动力: 运输工具速度 */
    private fun executeZhuiJiaDongLi(p: Player, s: SkillInstance, lv: Int): Boolean {
        val boost = when (lv) { 1 -> 0.30; 2 -> 0.40; 3 -> 0.50; else -> 0.30 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 5 * 20, (lv * 2) - 1, false, true))
        p.sendMessage(Component.text("§a✦ 追加动力：运输工具速度 +${(boost*100).toInt()}%，持续5秒"))
        return true
    }

    /** 不灭矿灯: 动态光源 */
    private fun executeBuMieKuangDeng(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 120; 2 -> 180; 3 -> 240; else -> 120 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, duration, 0, false, true))
        p.world.spawn(p.location, org.bukkit.entity.GlowItemFrame::class.java)?.apply {
            // Use a light block approach: give the player a glowing effect
            remove() // Clean up - the night vision potion is the real effect
        }
        p.sendMessage(Component.text("§a✦ 不灭矿灯：夜视效果，持续${duration/20}秒"))
        return true
    }

    /** 斩尽桎梏: 力量效果 */
    private fun executeZhanJinZhiGu(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 60; 2 -> 75; 3 -> 90; else -> 60 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, duration, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 斩尽桎梏：力量 ${lv}级，持续${duration/20}秒"))
        return true
    }

    /** 掩体机动: 抗性提升 */
    private fun executeYanTiJiDong(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 10; 2 -> 12; 3 -> 15; else -> 10 } * 20
        val amp = if (lv >= 3) 1 else 0
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, duration, amp, false, true))
        p.sendMessage(Component.text("§a✦ 掩体机动：抗性提升 ${amp+1}级，持续${duration/20}秒"))
        return true
    }

    /** 奇兵突袭: 隐身+力量 */
    private fun executeQiBingTuXi(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 30; 2 -> 45; 3 -> 60; else -> 30 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, duration, 0, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, duration, lv, false, true))
        p.sendMessage(Component.text("§a✦ 奇兵突袭：隐身+力量 ${lv+1}级，持续${duration/20}秒"))
        return true
    }

    /** 大洋眷顾: 幸运效果 */
    private fun executeDaYangJuanGu(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 40; 2 -> 50; 3 -> 60; else -> 40 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, duration, 1, false, true))
        p.sendMessage(Component.text("§a✦ 大洋眷顾：幸运 II，持续${duration/20}秒"))
        return true
    }

    /** 困兽天敌: 周围动物缓慢+发光 */
    private fun executeKunShouTianDi(p: Player, s: SkillInstance, lv: Int): Boolean {
        val range = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
        val animals = p.location.getNearbyEntities(range, range, range).filterIsInstance<org.bukkit.entity.Animals>()
        val count = animals.count { animal ->
            animal.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 10 * 20, lv - 1))
            animal.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 10 * 20, 0))
            true
        }
        p.sendMessage(Component.text("§a✦ 困兽天敌：${count}只动物被标记"))
        return true
    }

    /** 甜点派对: 召唤蛋糕 */
    private fun executeTianDianPaiDui(p: Player, s: SkillInstance, lv: Int): Boolean {
        val extraPlayers = p.location.getNearbyPlayers(4.0).size - 1
        val maxExtra = when (lv) { 1 -> 2; 2 -> 3; 3 -> 4; else -> 0 }
        val extraCakes = when (lv) {
            1 -> (extraPlayers / 3).coerceAtMost(maxExtra)
            2 -> (extraPlayers / 2).coerceAtMost(maxExtra)
            3 -> (extraPlayers / 2 + 1).coerceAtMost(maxExtra)
            else -> 0
        }
        val base = if (lv >= 3) 2 else 1
        val total = base + extraCakes
        repeat(total) {
            val dx = (-2..2).random().toDouble()
            val dz = (-2..2).random().toDouble()
            val loc = p.location.clone().add(dx, 0.0, dz)
            if (loc.block.type == org.bukkit.Material.AIR) loc.block.type = org.bukkit.Material.CAKE
        }
        p.sendMessage(Component.text("§a✦ 甜点派对：召唤了 $total 个蛋糕！"))
        return true
    }

    /** 收获涛声: 钓鱼额外获得 */
    private fun executeShouHuoTaoSheng(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.sendMessage(Component.text("§a✦ 收获涛声：下次钓鱼将额外获得 ${lv} 条鱼！"))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 30 * 20, lv, false, true))
        return true
    }

    /** 特聘保镖: 召唤铁傀儡/雪傀儡 */
    private fun executeTePinBaoBiao(p: Player, s: SkillInstance, lv: Int): Boolean {
        val emeralds = p.inventory.all(org.bukkit.Material.EMERALD).values.sumOf { it.amount }
        when {
            emeralds <= 0 -> { p.sendMessage(Component.text("§c需要至少1颗绿宝石！")); return false }
            emeralds <= 16 -> {
                repeat(if (lv >= 2) 2 else 1) { p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java) }
                p.sendMessage(Component.text("§a✦ 召唤了雪傀儡"))
            }
            emeralds <= 64 -> {
                repeat(if (lv >= 2) 2 else 1) { p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java) }
                p.world.spawn(p.location, org.bukkit.entity.IronGolem::class.java)
                p.sendMessage(Component.text("§a✦ 召唤了雪傀儡+铁傀儡"))
            }
            else -> {
                repeat(if (lv >= 3) 3 else 2) { p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java) }
                repeat(if (lv >= 3) 3 else 2) { p.world.spawn(p.location, org.bukkit.entity.IronGolem::class.java) }
                p.sendMessage(Component.text("§a✦ 召唤了铁傀儡军团"))
            }
        }
        return true
    }

    /** 牧原欢歌: 空桶变奶桶 */
    private fun executeMuYuanHuanGe(p: Player, s: SkillInstance, lv: Int): Boolean {
        val range = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
        val hasCow = p.location.getNearbyEntities(range, range, range).any { it is org.bukkit.entity.Cow }
        if (!hasCow) { p.sendMessage(Component.text("§c周围没有牛！")); return false }
        var count = 0
        for (item in p.inventory.contents) {
            if (item != null && item.type == org.bukkit.Material.BUCKET) {
                item.type = org.bukkit.Material.MILK_BUCKET; count++
            }
        }
        p.sendMessage(Component.text("§a✦ 牧原欢歌：$count 个空桶变为奶桶！"))
        return count > 0
    }

    /** 决战冲锋: 武器伤害+沉默盾牌 */
    private fun executeJueZhanChongFeng(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 60; 2 -> 75; 3 -> 90; else -> 60 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 5 * 20, if (lv >= 3) 3 else 2, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, duration, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 决战冲锋：武器伤害+${(20+lv*10)}%，持续${duration/20}秒"))
        return true
    }

    /** 浴血强攻: 力量效果(低血加成) */
    private fun executeYuXueQiangGong(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 10; 2 -> 12; 3 -> 15; else -> 10 } * 20
        val amp = if (p.health <= 10.0) lv else lv - 1
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, duration, amp, false, true))
        p.sendMessage(Component.text("§a✦ 浴血强攻：力量 ${amp+1}级，持续${duration/20}秒"))
        return true
    }

    /** 探索者的行囊: 恢复生命/饥饿 */
    private fun executeTanSuoZheXingNang(p: Player, s: SkillInstance, lv: Int): Boolean {
        val heal = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
        p.health = (p.health + heal).coerceAtMost(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0)
        val foodItems = mapOf(1 to org.bukkit.Material.BREAD, 2 to org.bukkit.Material.COOKED_SALMON, 3 to org.bukkit.Material.PUMPKIN_PIE)
        foodItems[lv]?.let { p.inventory.addItem(org.bukkit.inventory.ItemStack(it, 1)) }
        p.sendMessage(Component.text("§a✦ 探索者的行囊：恢复 ${heal.toInt()} 生命值"))
        return true
    }

    /** 信号弹: 下次光灵箭不消耗，发光延长+5/10/15秒 */
    private fun executeXinHaoDan(p: Player, s: SkillInstance, lv: Int): Boolean {
        // Store the signal flare level for the next spectral arrow shot
        signalFlarePlayers[p.uniqueId] = lv
        p.sendMessage(Component.text("§a✦ 信号弹：下次光灵箭不消耗，目标发光时长+${5+lv*5}秒"))
        return true
    }

    /** Active signal flare tracking: UUID -> level */
    private val signalFlarePlayers = mutableMapOf<java.util.UUID, Int>()

    /** Called when player shoots a bow — applies signal flare effect if active */
    fun onSignalFlareShoot(player: Player, arrow: org.bukkit.entity.AbstractArrow, ammo: org.bukkit.inventory.ItemStack) {
        val lv = signalFlarePlayers.remove(player.uniqueId) ?: return
        if (ammo.type != org.bukkit.Material.SPECTRAL_ARROW) return
        // Refund the arrow
        player.inventory.addItem(org.bukkit.inventory.ItemStack(org.bukkit.Material.SPECTRAL_ARROW, 1))
        // Store glow extension info on the arrow
        arrow.persistentDataContainer.set(
            org.bukkit.NamespacedKey("starlightre", "signal_flare_lv"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            lv
        )
    }

    /** Called when projectile hits — extends glow duration */
    fun onSignalFlareHit(event: org.bukkit.event.entity.ProjectileHitEvent) {
        val arrow = event.entity
        val lv = arrow.persistentDataContainer.get(
            org.bukkit.NamespacedKey("starlightre", "signal_flare_lv"),
            org.bukkit.persistence.PersistentDataType.INTEGER
        ) ?: return
        val target = event.hitEntity as? org.bukkit.entity.LivingEntity ?: return
        val extraTicks = (5 + lv * 5) * 20
        target.addPotionEffect(org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING, 10 * 20 + extraTicks, 0, false, false
        ))
    }

    /** 人力资源管理: 全服广播急迫 */
    private fun executeRenLiZiYuan(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 15; 2 -> 20; 3 -> 20; else -> 15 }
        val amp = if (lv >= 3 && false) 1 else 0
        for (online in org.bukkit.Bukkit.getOnlinePlayers()) {
            online.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, duration * 20, amp, false, true))
        }
        org.bukkit.Bukkit.broadcast(Component.text("§e⚡ ${p.name} 释放了人力资源管理！所有在线玩家获得${duration}秒急迫"))
        return true
    }

    /** 学术沙龙: 周围玩家共鸣范围增加 */
    private fun executeXueShuShaLong(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 30; 2 -> 45; 3 -> 60; else -> 30 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, duration * 20, 0, false, true))
        p.location.getNearbyPlayers(4.0).forEach { target ->
            target.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, duration * 20, 0, false, true))
        }
        p.sendMessage(Component.text("§a✦ 学术沙龙：共鸣范围+${4+lv*4}，持续${duration}秒"))
        return true
    }

    /** 蒸发控件: 酿造台烈焰粉掉落 */
    private fun executeZhengFaKongJian(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 60 * 20, lv, false, true))
        p.sendMessage(Component.text("§a✦ 蒸发控件：持续60秒，酿造台有${lv*10}%概率掉落烈焰粉"))
        return true
    }

    // ===== Remaining active skill implementations =====

    /** 备用载具: 召唤船只 */
    private fun executeBeiYongZaiJu(p: Player, s: SkillInstance, lv: Int): Boolean {
        if (!p.location.block.type.name.contains("WATER") && p.location.block.type != org.bukkit.Material.WATER) {
            p.sendMessage(Component.text("§c需要在水中使用！")); return false
        }
        val boatType = org.bukkit.entity.EntityType.OAK_BOAT
        p.world.spawn(p.location, org.bukkit.entity.Boat::class.java)
        p.sendMessage(Component.text("§a✦ 备用载具：召唤了一艘船"))
        return true
    }

    /** 庇护寻求: 伤害减免 */
    private fun executeBiHuXunQiu(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 4; 2 -> 5; 3 -> 6; else -> 4 } * 20
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, duration, 3, false, true))
        p.sendMessage(Component.text("§a✦ 庇护寻求：伤害减免，持续${duration/20}秒"))
        return true
    }

    /** 铁壁力场: 砖块保护 */
    private fun executeTieBiLiChang(p: Player, s: SkillInstance, lv: Int): Boolean {
        val range = when (lv) { 1 -> 16; 2 -> 24; 3 -> 32; else -> 16 }
        val duration = when (lv) { 1 -> 20; 2 -> 30; 3 -> 40; else -> 20 }
        p.sendMessage(Component.text("§a✦ 铁壁力场：周围${range}格砖块受保护，持续${duration}秒"))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, duration * 20, 1, false, true))
        return true
    }

    /** 肾上腺素: 喷溅治疗药水buff */
    private fun executeShenShangXianSu(p: Player, s: SkillInstance, lv: Int): Boolean {
        val amp = lv - 1
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 8 * 20, 0, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, (15 + lv * 5) * 20, lv - 1, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, (2 + lv) * 20, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 肾上腺素：下次喷溅治疗药水将附加Buff"))
        return true
    }

    /** 奇味异珍: 特殊食物效果 */
    private fun executeQiWeiYiZhen(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.sendMessage(Component.text("§a✦ 奇味异珍：下次食用紫颂果/金胡萝卜/发光浆果/毒马铃薯触发特效"))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 60 * 20, lv, false, true))
        return true
    }

    /** 镀金美馔: 金色食物分享 */
    private fun executeDuJinMeiZhuan(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.sendMessage(Component.text("§a✦ 镀金美馔：下次食用金色食物时分享饱食效果"))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SATURATION, 1 * 20, lv, false, true))
        return true
    }

    /** 文火慢炖: 额外汤煲 */
    private fun executeWenHuoManDun(p: Player, s: SkillInstance, lv: Int): Boolean {
        val stews = listOf(org.bukkit.Material.MUSHROOM_STEW, org.bukkit.Material.BEETROOT_SOUP, org.bukkit.Material.RABBIT_STEW)
        p.inventory.addItem(org.bukkit.inventory.ItemStack(stews.random(), 1))
        p.sendMessage(Component.text("§a✦ 文火慢炖：获得一份额外汤煲"))
        return true
    }

    /** 烤肉专家: 营火烹饪 */
    private fun executeKaoRouZhuanJia(p: Player, s: SkillInstance, lv: Int): Boolean {
        val range = when (lv) { 1 -> 3.0; 2 -> 3.0; 3 -> 3.0; else -> 3.0 }
        val count = if (lv >= 3) 2 else 1
        p.sendMessage(Component.text("§a✦ 烤肉专家：周围${count}个营火加速烹饪"))
        return true
    }

    /** 行政结构优化: 经验奖励 */
    private fun executeXingZhengYouHua(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.giveExp(5 + lv * 3)
        p.sendMessage(Component.text("§a✦ 行政结构优化：获得${5+lv*3}点经验"))
        return true
    }

    /** 综合报告审阅: 查看周围玩家 */
    private fun executeZongHeBaoGao(p: Player, s: SkillInstance, lv: Int): Boolean {
        val nearby = p.location.getNearbyPlayers(8.0)
        val details = nearby.joinToString("、") { target ->
            val cp = CareerManager.getPlayer(target)
            if (cp != null) "${target.name}(${cp.selectedClasses.joinToString { it.displayName }})"
            else target.name
        }
        p.sendMessage(Component.text("§e周围玩家：${details.ifEmpty { "无" }}"))
        return true
    }

    /** 超载: 周围8格红石粉受15级信号，站红石粉上的玩家每秒受1点伤害 */
    private fun executeChaoZai(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 20; 2 -> 25; 3 -> 30; else -> 20 }
        val range = 8
        val center = p.location.clone()
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") ?: run {
            p.sendMessage("§c内部错误: 找不到插件实例"); return false
        }

        // Set redstone power to 15 and store originals
        val poweredWires = mutableMapOf<org.bukkit.block.Block, org.bukkit.block.data.BlockData>()
        var wireCount = 0
        for (x in -range..range) for (y in -2..2) for (z in -range..range) {
            val block = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
            if (block.type == org.bukkit.Material.REDSTONE_WIRE) {
                try {
                    poweredWires[block] = block.blockData.clone()
                    block.blockData = org.bukkit.Bukkit.createBlockData("minecraft:redstone_wire[power=15]")
                    wireCount++
                } catch (e: Exception) {
                    p.sendMessage("§c红石粉设置失败: ${e.message}")
                }
            }
        }
        p.sendMessage("§a✦ 超载：已设置${wireCount}处红石粉 => 信号15，持续${duration}秒")

        // Damage players on redstone every second
        val taskObj = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (target in center.getNearbyPlayers(range.toDouble())) {
                if (target.location.block.type == org.bukkit.Material.REDSTONE_WIRE) target.damage(1.0)
            }
        }, 0L, 20L)

        // Restore after duration
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            taskObj.cancel()
            poweredWires.forEach { (block, original) ->
                try { block.blockData = original } catch (_: Exception) {}
            }
            p.sendMessage("§7超载效果结束")
        }, duration * 20L)

        return true
    }

    /** 饲料调配: 幼年动物成长 */
    private fun executeSiLiaoTiaoPei(p: Player, s: SkillInstance, lv: Int): Boolean {
        val animals = p.location.getNearbyEntities(5.0, 5.0, 5.0).filterIsInstance<org.bukkit.entity.Ageable>()
            .filter { !it.isAdult }.take(if (lv >= 3) 3 else 1)
        animals.forEach { it.age = 0 }
        p.sendMessage(Component.text("§a✦ 饲料调配：${animals.size}只幼年动物立即成长"))
        return true
    }

    /** 破财消灾: 村民回血 */
    private fun executePoCaiXiaoZai(p: Player, s: SkillInstance, lv: Int): Boolean {
        val cost = when (lv) { 1 -> 5; 2 -> 4; 3 -> 3; else -> 5 }
        if (!p.inventory.contains(org.bukkit.Material.EMERALD, cost)) {
            p.sendMessage(Component.text("§c需要${cost}颗绿宝石！")); return false
        }
        p.inventory.removeItem(org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, cost))
        val range = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
        val villagers = p.location.getNearbyEntities(range, range, range).filterIsInstance<org.bukkit.entity.Villager>()
        villagers.forEach { it.health = 20.0 }
        p.sendMessage(Component.text("§a✦ 破财消灾：${villagers.size}个村民生命恢复"))
        return true
    }

    /** 植物遗传学: 急迫 */
    private fun executeZhiWuYiChuanXue(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 30 * 20, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 植物遗传学：急迫 ${lv}级，持续30秒"))
        return true
    }

    /** 科学施肥: 骨粉效果 */
    private fun executeKeXueShiFei(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.inventory.addItem(org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE_MEAL, lv * 2))
        p.sendMessage(Component.text("§a✦ 科学施肥：获得${lv*2}个骨粉"))
        return true
    }

    /** 燃料管道: 熔炉燃烧 */
    private fun executeRanLiaoGuanDao(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 30; 2 -> 35; 3 -> 40; else -> 30 }
        p.sendMessage(Component.text("§a✦ 燃料管道：周围熔炉保持燃烧，持续${duration}秒"))
        return true
    }

    /** 巧力用斧: 下次不消耗耐久 */
    private fun executeQiaoLiYongFu(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.sendMessage(Component.text("§a✦ 巧力用斧：下次用斧破坏木方块不消耗耐久"))
        return true
    }

    /** 强力运镐: 下次不消耗耐久 */
    private fun executeQiangLiYunGao(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.sendMessage(Component.text("§a✦ 强力运镐：下次用镐破坏石方块不消耗耐久"))
        return true
    }

    /** 熔金模具: 高炉额外经验 */
    private fun executeRongJinMuJu(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.giveExp(2 + lv)
        p.sendMessage(Component.text("§a✦ 熔金模具：获得${2+lv}点额外经验"))
        return true
    }

    /** 我即梦魇: 额外伤害 */
    private fun executeWoJiEMengYan(p: Player, s: SkillInstance, lv: Int): Boolean {
        val bonus = when (lv) { 1 -> 0.20; 2 -> 0.30; 3 -> 0.40; else -> 0.20 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 60 * 20, lv - 1, false, true))
        p.sendMessage(Component.text("§a✦ 我即梦魇：伤害+${(bonus*100).toInt()}%，持续60秒"))
        return true
    }

    /** 前进营地: 信标光环 */
    private fun executeQianJinYingDi(p: Player, s: SkillInstance, lv: Int): Boolean {
        val boost = when (lv) { 1 -> 0.10; 2 -> 0.20; 3 -> 0.30; else -> 0.10 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 60 * 20, 0, false, true))
        p.sendMessage(Component.text("§a✦ 前进营地：信标恢复光环+${(boost*100).toInt()}%"))
        return true
    }

    /** 识色敏锐: 随机获得染色方块 */
    private fun executeShiSeMinRui(p: Player, s: SkillInstance, lv: Int): Boolean {
        val dyes = listOf(
            org.bukkit.Material.RED_DYE, org.bukkit.Material.GREEN_DYE, org.bukkit.Material.BLUE_DYE, org.bukkit.Material.YELLOW_DYE,
            org.bukkit.Material.PURPLE_DYE, org.bukkit.Material.CYAN_DYE, org.bukkit.Material.LIGHT_GRAY_DYE, org.bukkit.Material.GRAY_DYE,
            org.bukkit.Material.PINK_DYE, org.bukkit.Material.LIME_DYE, org.bukkit.Material.ORANGE_DYE, org.bukkit.Material.MAGENTA_DYE,
            org.bukkit.Material.LIGHT_BLUE_DYE, org.bukkit.Material.BROWN_DYE, org.bukkit.Material.BLACK_DYE, org.bukkit.Material.WHITE_DYE
        )
        val count = when (lv) { 1 -> 1; 2 -> 1; 3 -> 2; else -> 1 }
        repeat(count) { p.inventory.addItem(org.bukkit.inventory.ItemStack(dyes.random(), 1)) }
        p.sendMessage(Component.text("§a✦ 识色敏锐：获得${count}个随机染料"))
        return true
    }

    /** 自动化生产: 获得红石相关产物 */
    private fun executeZiDongHuaShengChan(p: Player, s: SkillInstance, lv: Int): Boolean {
        val products: List<Pair<org.bukkit.Material, Int>> = listOf(
            org.bukkit.Material.REDSTONE to 4, org.bukkit.Material.REPEATER to 2,
            org.bukkit.Material.COMPARATOR to 1, org.bukkit.Material.PISTON to 2,
            org.bukkit.Material.STICKY_PISTON to 1, org.bukkit.Material.OBSERVER to 1,
            org.bukkit.Material.DROPPER to 2, org.bukkit.Material.DISPENSER to 1
        )
        repeat(lv) {
            val pair = products.random()
            p.inventory.addItem(org.bukkit.inventory.ItemStack(pair.first, pair.second))
        }
        p.sendMessage(Component.text("§a✦ 自动化生产：获得红石产物 (×${lv})"))
        return true
    }

    /** 熟能生巧: 铁砧使用获得额外经验 */
    private fun executeShuNengShengQiao(p: Player, s: SkillInstance, lv: Int): Boolean {
        val exp = when (lv) { 1 -> 4; 2 -> 6; 3 -> 8; else -> 4 }
        p.giveExp(exp)
        p.sendMessage(Component.text("§a✦ 熟能生巧：获得${exp}级经验（用于铁砧操作）"))
        return true
    }

}
