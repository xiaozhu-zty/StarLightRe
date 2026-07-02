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
    fun executeSkill(player: Player, skill: SkillInstance, silent: Boolean = false): Boolean {
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
        val handled = executeBuiltinEffect(player, skill, level, silent)
        if (handled) {
            cp.setCooldown(effectId, now)
            return true
        }

        // Get effect from registry
        val effect = SkillRegistry.getSkill(effectId)
        if (effect == null) {
            if (!silent) player.sendMessage(
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

    private fun executeBuiltinEffect(player: Player, skill: SkillInstance, level: Int, silent: Boolean = false): Boolean {
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
            "chef_brewer_skill_2" -> executeShenShangXianSu(player, skill, level, silent)
            "chef_master_skill_1" -> executeQiWeiYiZhen(player, skill, level, silent)
            "chef_master_skill_2" -> executeDuJinMeiZhuan(player, skill, level, silent)
            "chef_baker_skill_1" -> executeWenHuoManDun(player, skill, level)
            "chef_butcher_skill_1" -> executeKaoRouZhuanJia(player, skill, level)
            "scholar_enchanter_skill_2" -> executeZhuShuGongCheng(player, skill, level)
            "scholar_admin_skill_0" -> executeRenLiZiYuan(player, skill, level)
            "scholar_teacher_skill_2" -> executeXueShuShaLong(player, skill, level)
            "scholar_admin_skill_1" -> executeXingZhengYouHua(player, skill, level)
            "scholar_admin_skill_2" -> executeZongHeBaoGao(player, skill, level)
            "scholar_redstone_skill_2" -> executeChaoZai(player, skill, level)
            "farmer_fisherman_skill_2" -> executeDaYangJuanGu(player, skill, level)
            "farmer_fisherman_skill_1" -> executeShouHuoTaoSheng(player, skill, level, silent)
            "farmer_merchant_skill_2" -> executeTePinBaoBiao(player, skill, level)
            "farmer_rancher_skill_2" -> executeMuYuanHuanGe(player, skill, level)
            "farmer_rancher_skill_1" -> executeSiLiaoTiaoPei(player, skill, level)
            "farmer_merchant_skill_1" -> executePoCaiXiaoZai(player, skill, level)
            "farmer_botanist_skill_2" -> executeZhiWuYiChuanXue(player, skill, level)
            "farmer_botanist_skill_1" -> executeKeXueShiFei(player, skill, level)
            "worker_miner_skill_2" -> executeBuMieKuangDeng(player, skill, level)
            "worker_lumberjack_skill_2" -> executeZhanJinZhiGu(player, skill, level)
            "worker_smelter_skill_2" -> executeRanLiaoGuanDao(player, skill, level)
            "worker_lumberjack_skill_1" -> executeQiaoLiYongFu(player, skill, level, silent)
            "worker_miner_skill_1" -> executeQiangLiYunGao(player, skill, level, silent)
            "worker_smelter_skill_1" -> executeRongJinMuJu(player, skill, level)
            "warrior_soldier_skill_1" -> executeYanTiJiDong(player, skill, level)
            "warrior_weapon_skill_2" -> executeQiBingTuXi(player, skill, level)
            "warrior_soldier_skill_2" -> executeJueZhanChongFeng(player, skill, level)
            "warrior_hunter_skill_1" -> executeYuXueQiangGong(player, skill, level)
            "warrior_explorer_skill_1" -> executeTanSuoZheXingNang(player, skill, level, silent)
            "warrior_weapon_skill_1" -> executeXinHaoDan(player, skill, level, silent)
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

    /** 凌空创想: 速度/跳跃/抗性，脚手架上效果增强 */
    data class LingKongData(val level: Int, val expiry: Long)
    val lingKongPlayers = mutableMapOf<java.util.UUID, LingKongData>()

    private fun executeLingKongChuangXiang(p: Player, s: SkillInstance, lv: Int): Boolean {
        val durationMin = when (lv) { 1 -> 3; 2 -> 4; 3 -> 5; else -> 3 }
        val durationTicks = durationMin * 60 * 20
        // Base effects (ground level)
        when (lv) {
            1 -> {
                p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, durationTicks, 0, false, true))
            }
            2 -> {
                p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, durationTicks, 0, false, true))
            }
            3 -> {
                p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, durationTicks, 1, false, true))
                p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, durationTicks, 0, false, true))
            }
        }
        lingKongPlayers[p.uniqueId] = LingKongData(lv, System.currentTimeMillis() + durationMin * 60_000L)
        p.sendMessage(Component.text("§a✦ 凌空创想：持续${durationMin}分钟，脚手架上效果增强"))
        return true
    }

    /** Called from PassiveSkillListener on PlayerMoveEvent: apply scaffolding boosts */
    fun tickLingKongChuangXiang(player: Player): Boolean {
        val data = lingKongPlayers[player.uniqueId] ?: return false
        if (System.currentTimeMillis() > data.expiry) { lingKongPlayers.remove(player.uniqueId); return false }
        val lv = data.level
        val blockUnder = player.location.clone().subtract(0.0, 1.0, 0.0).block.type
        if (blockUnder != org.bukkit.Material.SCAFFOLDING) return true // Not on scaffolding

        // Apply enhanced scaffolding effects (short duration, refreshed each tick)
        val amp = when (lv) { 1 -> 1; 2 -> 1; 3 -> 2; else -> 1 } // speed II/II/III
        val jumpAmp = lv - 1  // jump I/II/III
        val resistAmp = lv - 1 // resist I/II/III
        player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 4 * 20, amp, false, true))
        player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 4 * 20, jumpAmp, false, true))
        player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 4 * 20, resistAmp, false, true))
        return true
    }

    /** 追加动力: 运输工具速度 */
    private fun executeZhuiJiaDongLi(p: Player, s: SkillInstance, lv: Int): Boolean {
        val boost = when (lv) { 1 -> 0.30; 2 -> 0.40; 3 -> 0.50; else -> 0.30 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 5 * 20, (lv * 2) - 1, false, true))
        p.sendMessage(Component.text("§a✦ 追加动力：运输工具速度 +${(boost*100).toInt()}%，持续5秒"))
        return true
    }

    /** 不灭矿灯: 动态光源 + 夜视 */
    data class KuangDengData(val entity: org.bukkit.entity.FallingBlock, val expiry: Long)
    val kuangDengPlayers = mutableMapOf<java.util.UUID, KuangDengData>()

    private fun executeBuMieKuangDeng(p: Player, s: SkillInstance, lv: Int): Boolean {
        // Clean up previous light if any
        kuangDengPlayers.remove(p.uniqueId)?.entity?.remove()

        val durationSec = when (lv) { 1 -> 120; 2 -> 180; 3 -> 240; else -> 120 }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, durationSec * 20, 0, false, true))

        // Spawn invisible dynamic light via FallingBlock (SHROOMLIGHT = light level 15, hardcoded material property)
        // Position at player feet (inside hitbox) to be effectively invisible from first-person
        val loc = p.location.clone()
        val lightBlock = p.world.createEntity(loc, org.bukkit.entity.FallingBlock::class.java)
        lightBlock.blockData = org.bukkit.Material.SHROOMLIGHT.createBlockData()
        lightBlock.isSilent = true
        lightBlock.isInvulnerable = true
        lightBlock.dropItem = false
        lightBlock.velocity = org.bukkit.util.Vector(0, 0, 0)
        p.world.addEntity(lightBlock)

        kuangDengPlayers[p.uniqueId] = KuangDengData(lightBlock, System.currentTimeMillis() + durationSec * 1000L)
        p.sendMessage(Component.text("§a✦ 不灭矿灯：夜视 + 跟随光源，持续${durationSec}秒"))
        return true
    }

    /** Called each tick to teleport light blocks to follow players and clean up expired ones */
    fun tickKuangDeng() {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<java.util.UUID>()
        for ((uuid, data) in kuangDengPlayers) {
            val player = org.bukkit.Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline || now > data.expiry) {
                data.entity.remove()
                expired.add(uuid)
                continue
            }
            // Teleport light to follow player (at feet, inside hitbox = invisible)
            data.entity.teleport(player.location.clone())
            data.entity.ticksLived = 1 // Prevent natural despawn
        }
        expired.forEach { kuangDengPlayers.remove(it) }
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

    /** 大洋眷顾: 幸运效果 with biome-based duration extension */
    private fun executeDaYangJuanGu(p: Player, s: SkillInstance, lv: Int): Boolean {
        val biome = p.location.block.biome.toString().uppercase()
        val isDeepOcean = biome.contains("DEEP_OCEAN")
        val isOcean = isDeepOcean || biome.contains("OCEAN")
        val baseDuration = when (lv) { 1 -> 40; 2 -> 50; 3 -> 60; else -> 40 }
        val durationSeconds = when {
            lv >= 3 && isDeepOcean -> 180  // Lv.3 + deep ocean = 180s
            isOcean -> when (lv) { 1 -> 60; 2 -> 90; 3 -> 120; else -> baseDuration }
            else -> baseDuration
        }
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, durationSeconds * 20, 1, false, true))
        val biomeNote = when {
            lv >= 3 && isDeepOcean -> " (深海加成: 180秒)"
            isOcean -> " (海洋加成: ${durationSeconds}秒)"
            else -> ""
        }
        p.sendMessage(Component.text("§a✦ 大洋眷顾：幸运 II，持续${durationSeconds}秒$biomeNote"))
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

    /** 收获涛声: 钓鱼额外获得 — track for next catch */
    private val shouHuoTaoShengPlayers = mutableMapOf<java.util.UUID, Int>()

    private fun executeShouHuoTaoSheng(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        shouHuoTaoShengPlayers[p.uniqueId] = lv
        if (!silent) p.sendMessage(Component.text("§a✦ 收获涛声：下次钓鱼将额外获得 ${lv} 条鱼！"))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 30 * 20, lv, false, true))
        return true
    }

    /** Called when player catches a fish — returns number of extra fish to give */
    fun onShouHuoTaoShengCatch(player: Player): Int = shouHuoTaoShengPlayers.remove(player.uniqueId) ?: 0

    /** 特聘保镖: 召唤铁傀儡/雪傀儡，60s后消失 */
    private val tePinBaoBiaoEntities = mutableMapOf<java.util.UUID, MutableList<org.bukkit.entity.LivingEntity>>()

    private fun executeTePinBaoBiao(p: Player, s: SkillInstance, lv: Int): Boolean {
        // Clean up previous summons
        tePinBaoBiaoEntities.remove(p.uniqueId)?.forEach { it.remove() }

        val emeralds = p.inventory.all(org.bukkit.Material.EMERALD).values.sumOf { it.amount }
        val spawned = mutableListOf<org.bukkit.entity.LivingEntity>()
        when {
            emeralds <= 0 -> { p.sendMessage(Component.text("§c需要至少1颗绿宝石！")); return false }
            emeralds <= 16 -> {
                repeat(if (lv >= 2) 2 else 1) { spawned.add(p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java)) }
                p.sendMessage(Component.text("§a✦ 召唤了雪傀儡"))
            }
            emeralds <= 64 -> {
                repeat(if (lv >= 2) 2 else 1) { spawned.add(p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java)) }
                spawned.add(p.world.spawn(p.location, org.bukkit.entity.IronGolem::class.java))
                p.sendMessage(Component.text("§a✦ 召唤了雪傀儡+铁傀儡"))
            }
            else -> {
                repeat(if (lv >= 3) 3 else 2) { spawned.add(p.world.spawn(p.location, org.bukkit.entity.Snowman::class.java)) }
                repeat(if (lv >= 3) 3 else 2) { spawned.add(p.world.spawn(p.location, org.bukkit.entity.IronGolem::class.java)) }
                p.sendMessage(Component.text("§a✦ 召唤了铁傀儡军团"))
            }
        }
        tePinBaoBiaoEntities[p.uniqueId] = spawned
        // Schedule removal after 60s
        val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("StarLightRe") ?: return true
        val uuid = p.uniqueId
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            tePinBaoBiaoEntities.remove(uuid)?.forEach { it.remove() }
        }, 60 * 20L)
        return true
    }

    /** 牧原欢歌: 手持剪刀剪周围羊 / 手持空桶挤周围牛和山羊 */
    private fun executeMuYuanHuanGe(p: Player, s: SkillInstance, lv: Int): Boolean {
        val range = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
        val held = p.inventory.itemInMainHand.type
        var count = 0
        when (held) {
            org.bukkit.Material.SHEARS -> {
                for (entity in p.location.getNearbyEntities(range, range, range)) {
                    if (entity is org.bukkit.entity.Sheep && !entity.isSheared) {
                        entity.isSheared = true
                        entity.world.dropItemNaturally(entity.location,
                            org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(
                                entity.color?.name?.uppercase()?.let { "${it}_WOOL" } ?: "WHITE_WOOL"), (1..3).random()))
                        count++
                    }
                }
                p.sendMessage(Component.text("§a✦ 牧原欢歌：为${count}只羊剪毛"))
            }
            org.bukkit.Material.BUCKET -> {
                for (entity in p.location.getNearbyEntities(range, range, range)) {
                    if (entity is org.bukkit.entity.Cow || entity is org.bukkit.entity.Goat) {
                        // Give milk bucket
                        val leftover = p.inventory.addItem(org.bukkit.inventory.ItemStack(org.bukkit.Material.MILK_BUCKET, 1))
                        leftover.values.forEach { p.world.dropItemNaturally(p.location, it) }
                        count++
                    }
                }
                p.sendMessage(Component.text("§a✦ 牧原欢歌：为${count}只牛/山羊挤奶"))
            }
            else -> {
                p.sendMessage(Component.text("§c牧原欢歌：请手持剪刀或空桶！"))
                return false
            }
        }
        return count > 0
    }

    /** 决战冲锋: 伤害加成(非力量药水) + 速度III + 盾牌沉默 */
    private fun executeJueZhanChongFeng(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 60; 2 -> 75; 3 -> 90; else -> 60 }
        val damageBonus = when (lv) { 1 -> 0.30; 2 -> 0.40; 3 -> 0.50; else -> 0.30 }
        val shieldBonus = when (lv) { 1 -> 0.50; 2 -> 0.75; 3 -> 1.00; else -> 0.50 }
        // Speed III for 5 seconds
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 5 * 20, 2, false, true))
        // Track custom damage bonus (not Strength potion!)
        chargeAssaultPlayers[p.uniqueId] = ChargeAssaultData(
            System.currentTimeMillis() + duration * 1000L,
            damageBonus, shieldBonus
        )
        p.sendMessage(Component.text("§a✦ 决战冲锋：伤害+${(damageBonus*100).toInt()}%，盾牌沉默+${(shieldBonus*100).toInt()}%，持续${duration}秒"))
        return true
    }

    // 决战冲锋 active tracking
    data class ChargeAssaultData(val expiry: Long, val damageMultiplier: Double, val shieldSilence: Double)
    private val chargeAssaultPlayers = mutableMapOf<java.util.UUID, ChargeAssaultData>()

    /** Get 决战冲锋 damage multiplier if active, or 0.0 */
    fun getChargeAssaultBonus(player: Player): Double {
        val data = chargeAssaultPlayers[player.uniqueId] ?: return 0.0
        if (System.currentTimeMillis() > data.expiry) { chargeAssaultPlayers.remove(player.uniqueId); return 0.0 }
        return data.damageMultiplier
    }

    /** Get 决战冲锋 shield silence multiplier if active, or 0.0 */
    fun getChargeAssaultShield(player: Player): Double {
        val data = chargeAssaultPlayers[player.uniqueId] ?: return 0.0
        if (System.currentTimeMillis() > data.expiry) { chargeAssaultPlayers.remove(player.uniqueId); return 0.0 }
        return data.shieldSilence
    }

    /** 浴血强攻: 力量效果(低血加成) */
    private fun executeYuXueQiangGong(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 10; 2 -> 12; 3 -> 15; else -> 10 } * 20
        val amp = if (p.health <= 10.0) lv else lv - 1
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, duration, amp, false, true))
        p.sendMessage(Component.text("§a✦ 浴血强攻：力量 ${amp+1}级，持续${duration/20}秒"))
        return true
    }

    /** 探索者的行囊: 若生命/饥饿不满则恢复，均满则获得迅捷 */
    private fun executeTanSuoZheXingNang(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        val maxHp = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
        val hpFull = p.health >= maxHp
        val foodFull = p.foodLevel >= 20

        if (hpFull && foodFull) {
            // Both full → Speed buff
            val (duration, amp) = when (lv) {
                1 -> 60 to 1   // Speed II 60s
                2 -> 75 to 1   // Speed II 75s
                3 -> 75 to 2   // Speed III 75s
                else -> 60 to 1
            }
            p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, duration * 20, amp, false, true))
            if (!silent) p.sendMessage(Component.text("§a✦ 探索者的行囊：获得迅捷${if(amp >= 2) "III" else "II"} ${duration}秒"))
            return true
        }

        val msgs = mutableListOf<String>()
        if (!hpFull) {
            val heal = when (lv) { 1 -> 8.0; 2 -> 12.0; 3 -> 16.0; else -> 8.0 }
            p.health = (p.health + heal).coerceAtMost(maxHp)
            msgs.add("恢复${heal.toInt()}生命值")
        }
        if (!foodFull) {
            val foodItem = when (lv) { 1 -> org.bukkit.Material.BREAD; 2 -> org.bukkit.Material.COOKED_SALMON; 3 -> org.bukkit.Material.PUMPKIN_PIE; else -> org.bukkit.Material.BREAD }
            p.inventory.addItem(org.bukkit.inventory.ItemStack(foodItem, 1))
            msgs.add("获得${when(lv) {1 -> "面包"; 2 -> "熟鲑鱼"; 3 -> "南瓜派"; else -> "面包"}}")
        }
        if (!silent) p.sendMessage(Component.text("§a✦ 探索者的行囊：${msgs.joinToString("，")}"))
        return true
    }

    /** 信号弹: 下次光灵箭不消耗，发光延长+5/10/15秒 */
    private fun executeXinHaoDan(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        // Store the signal flare level for the next spectral arrow shot
        signalFlarePlayers[p.uniqueId] = lv
        if (!silent) p.sendMessage(Component.text("§a✦ 信号弹：下次光灵箭不消耗，目标发光时长+${5+lv*5}秒"))
        return true
    }

    /** Clear all skill tracking data for a player (called on branch forget) */
    fun clearPlayerTracking(uuid: java.util.UUID) {
        qiangLiYunGaoPlayers.remove(uuid)
        qiaoLiYongFuPlayers.remove(uuid)
        qiWeiYiZhenPlayers.remove(uuid)
        duJinMeiZhuanPlayers.remove(uuid)
        wenHuoManDunPlayers.remove(uuid)
        shouHuoTaoShengPlayers.remove(uuid)
        signalFlarePlayers.remove(uuid)
        kuangDengPlayers.remove(uuid)?.entity?.remove()
        ranLiaoGuanDaoPlayers.remove(uuid)
        zhengFaKongJianPlayers.remove(uuid)
        keXueShiFeiPlayers.remove(uuid)
        siLiaoTiaoPeiPlayers.remove(uuid)
        chargeAssaultPlayers.remove(uuid)
        nightmarePlayers.remove(uuid)
        lingKongPlayers.remove(uuid)
        tePinBaoBiaoEntities.remove(uuid)?.forEach { it.remove() }
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

    /** 蒸发控件: 60s内酿造台有概率掉落烈焰粉 */
    private val zhengFaKongJianPlayers = mutableMapOf<java.util.UUID, Pair<Int, Long>>() // UUID -> (bonus%, expiry)

    private fun executeZhengFaKongJian(p: Player, s: SkillInstance, lv: Int): Boolean {
        val bonus = lv * 10 // Lv.1=10%, Lv.2=20%, Lv.3=30%
        zhengFaKongJianPlayers[p.uniqueId] = bonus to (System.currentTimeMillis() + 60_000L)
        p.sendMessage(Component.text("§a✦ 蒸发控件：持续60秒，酿造台有${bonus}%概率掉落烈焰粉"))
        return true
    }

    fun getZhengFaBonus(player: Player): Int {
        val (bonus, expiry) = zhengFaKongJianPlayers[player.uniqueId] ?: return 0
        if (System.currentTimeMillis() > expiry) { zhengFaKongJianPlayers.remove(player.uniqueId); return 0 }
        return bonus
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

    /** 肾上腺素注射: 下次喷溅治疗药水II时buff受影响玩家 */
    private fun executeShenShangXianSu(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        // Require: no totem of undying
        if (p.inventory.itemInMainHand.type == org.bukkit.Material.TOTEM_OF_UNDYING ||
            p.inventory.itemInOffHand.type == org.bukkit.Material.TOTEM_OF_UNDYING) {
            if (!silent) p.sendMessage(Component.text("§c肾上腺素注射：不能携带不死图腾！"))
            return false
        }
        val (fireDur, regenDur, regenAmp, absorbDur, absorbAmp) = when (lv) {
            1 -> listOf(8, 20, 0, 3, 0)    // fire 8s, regen I 20s, absorb I 3s
            2 -> listOf(12, 25, 1, 4, 1)    // fire 12s, regen II 25s, absorb II 4s
            3 -> listOf(20, 30, 1, 5, 2)    // fire 20s, regen II 30s, absorb III 5s
            else -> listOf(8, 20, 0, 3, 0)
        }
        // Apply buffs to self (the YAML says "affected players" via splash potion; for now apply to caster)
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, fireDur * 20, 0, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, regenDur * 20, regenAmp, false, true))
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, absorbDur * 20, absorbAmp, false, true))
        if (!silent) p.sendMessage(Component.text("§a✦ 肾上腺素注射：抗火${fireDur}s + 生命恢复 + 伤害吸收"))
        return true
    }

    /** 奇味异珍: 下一次吃特殊食物时触发效果 */
    private val qiWeiYiZhenPlayers = mutableMapOf<java.util.UUID, Int>()

    private fun executeQiWeiYiZhen(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        qiWeiYiZhenPlayers[p.uniqueId] = lv
        if (!silent) p.sendMessage(Component.text("§a✦ 奇味异珍：下次食用特殊食物触发特效"))
        return true
    }

    /** Check if 奇味异珍 is active (without consuming) */
    fun peekQiWeiYiZhen(player: Player): Int = qiWeiYiZhenPlayers[player.uniqueId] ?: 0
    /** Consume 奇味异珍 flag after matching food */
    fun consumeQiWeiYiZhen(player: Player) { qiWeiYiZhenPlayers.remove(player.uniqueId) }

    /** 镀金美馔: 下一次吃金色食物时分享效果 */
    private val duJinMeiZhuanPlayers = mutableMapOf<java.util.UUID, Int>()

    private fun executeDuJinMeiZhuan(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        duJinMeiZhuanPlayers[p.uniqueId] = lv
        if (!silent) p.sendMessage(Component.text("§a✦ 镀金美馔：下次食用金色食物时分享饱食效果"))
        return true
    }

    /** Check if 镀金美馔 is active (without consuming) */
    fun peekDuJinMeiZhuan(player: Player): Int = duJinMeiZhuanPlayers[player.uniqueId] ?: 0
    /** Consume 镀金美馔 flag after matching food */
    fun consumeDuJinMeiZhuan(player: Player) { duJinMeiZhuanPlayers.remove(player.uniqueId) }

    /** 文火慢炖: 下次合成汤煲时额外获得 */
    private val wenHuoManDunPlayers = mutableMapOf<java.util.UUID, Int>()

    private fun executeWenHuoManDun(p: Player, s: SkillInstance, lv: Int): Boolean {
        wenHuoManDunPlayers[p.uniqueId] = lv
        p.sendMessage(Component.text("§a✦ 文火慢炖：下次合成汤煲食品时额外获得"))
        return true
    }

    /** Called from CraftItemEvent — returns level if active, else 0; consumes flag */
    fun onWenHuoManDunCraft(player: Player): Int = wenHuoManDunPlayers.remove(player.uniqueId) ?: 0

    /** 烤肉专家: 营火立即完成烹饪 */
    private fun executeKaoRouZhuanJia(p: Player, s: SkillInstance, lv: Int): Boolean {
        val maxCampfires = if (lv >= 3) 2 else 1
        var campfireCount = 0
        var itemCount = 0
        for (x in -3..3) for (z in -3..3) for (y in -1..1) {
            val block = p.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
            if (block.type == org.bukkit.Material.CAMPFIRE || block.type == org.bukkit.Material.SOUL_CAMPFIRE) {
                val campfire = block.state as? org.bukkit.block.Campfire ?: continue
                var hadItems = false
                for (i in 0..3) {
                    val item = campfire.getItem(i)
                    if (item != null && item.type != org.bukkit.Material.AIR) {
                        // Drop the cooked result immediately
                        val cooked = getCampfireResult(item.type)
                        if (cooked != null) {
                            block.world.dropItemNaturally(block.location, org.bukkit.inventory.ItemStack(cooked, item.amount))
                        }
                        campfire.setItem(i, null)
                        hadItems = true
                        itemCount++
                    }
                }
                if (hadItems) {
                    campfire.update()
                    campfireCount++
                    if (campfireCount >= maxCampfires) break
                }
            }
        }
        p.sendMessage(Component.text("§a✦ 烤肉专家：${campfireCount}个营火的${itemCount}个物品已烹饪完成"))
        return campfireCount > 0
    }

    private fun getCampfireResult(raw: org.bukkit.Material): org.bukkit.Material? {
        return when (raw) {
            org.bukkit.Material.BEEF -> org.bukkit.Material.COOKED_BEEF
            org.bukkit.Material.PORKCHOP -> org.bukkit.Material.COOKED_PORKCHOP
            org.bukkit.Material.CHICKEN -> org.bukkit.Material.COOKED_CHICKEN
            org.bukkit.Material.MUTTON -> org.bukkit.Material.COOKED_MUTTON
            org.bukkit.Material.RABBIT -> org.bukkit.Material.COOKED_RABBIT
            org.bukkit.Material.COD -> org.bukkit.Material.COOKED_COD
            org.bukkit.Material.SALMON -> org.bukkit.Material.COOKED_SALMON
            org.bukkit.Material.POTATO -> org.bukkit.Material.BAKED_POTATO
            org.bukkit.Material.KELP -> org.bukkit.Material.DRIED_KELP
            else -> null
        }
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

    /** 饲料调配: 下次喂养幼年动物时使其立即成长 */
    private val siLiaoTiaoPeiPlayers = mutableMapOf<java.util.UUID, Int>()

    private fun executeSiLiaoTiaoPei(p: Player, s: SkillInstance, lv: Int): Boolean {
        siLiaoTiaoPeiPlayers[p.uniqueId] = lv
        p.sendMessage(Component.text("§a✦ 饲料调配：下次喂养幼年动物时使其立即成长"))
        return true
    }

    fun onSiLiaoTiaoPeiFeed(player: Player, entity: org.bukkit.entity.Ageable): Boolean {
        val lv = siLiaoTiaoPeiPlayers.remove(player.uniqueId) ?: return false
        if (!entity.isAdult) {
            entity.age = 0
            entity.world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, entity.location.add(0.0, 1.0, 0.0), 5)
            return true
        }
        return false
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

    /** 植物遗传学: 消耗种子获得急迫 */
    private val seedTypes = setOf(
        org.bukkit.Material.WHEAT_SEEDS, org.bukkit.Material.BEETROOT_SEEDS,
        org.bukkit.Material.MELON_SEEDS, org.bukkit.Material.PUMPKIN_SEEDS,
        org.bukkit.Material.TORCHFLOWER_SEEDS, org.bukkit.Material.PITCHER_POD
    )

    private fun executeZhiWuYiChuanXue(p: Player, s: SkillInstance, lv: Int): Boolean {
        // Count total seeds in inventory
        var totalSeeds = 0
        for (item in p.inventory.contents) {
            if (item != null && item.type in seedTypes) totalSeeds += item.amount
        }
        if (totalSeeds <= 0) {
            p.sendMessage(Component.text("§c植物遗传学：背包中没有种子！"))
            return false
        }
        val consumed = totalSeeds.coerceAtMost(640)
        // Remove seeds from inventory
        var remaining = consumed
        for (item in p.inventory.contents) {
            if (item != null && item.type in seedTypes && remaining > 0) {
                val take = item.amount.coerceAtMost(remaining)
                item.amount -= take
                remaining -= take
            }
        }
        val durationSec = consumed / 2
        val amp = if (lv >= 3) 2 else 1 // Haste II / III
        p.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, durationSec * 20, amp, false, true))
        p.sendMessage(Component.text("§a✦ 植物遗传学：消耗${consumed}个种子，获得急迫${if(amp>=2) "III" else "II"} ${durationSec}秒"))
        return true
    }

    /** 科学施肥: 持续时间内骨粉不消耗 */
    private val keXueShiFeiPlayers = mutableMapOf<java.util.UUID, Pair<Int, Long>>() // UUID -> (chance%, expiry)

    private fun executeKeXueShiFei(p: Player, s: SkillInstance, lv: Int): Boolean {
        val (durationSec, chance) = when (lv) { 1 -> 20 to 10; 2 -> 25 to 20; 3 -> 30 to 30; else -> 20 to 10 }
        keXueShiFeiPlayers[p.uniqueId] = chance to (System.currentTimeMillis() + durationSec * 1000L)
        p.sendMessage(Component.text("§a✦ 科学施肥：持续${durationSec}秒，骨粉不消耗概率${chance}%"))
        return true
    }

    /** Returns save chance % if active, else 0 */
    fun getKeXueShiFeiChance(player: Player): Int {
        val (chance, expiry) = keXueShiFeiPlayers[player.uniqueId] ?: return 0
        if (System.currentTimeMillis() > expiry) { keXueShiFeiPlayers.remove(player.uniqueId); return 0 }
        return chance
    }

    /** 燃料管道: 周围熔炉不消耗燃料持续燃烧 */
    data class RanLiaoData(val range: Double, val expiry: Long)
    val ranLiaoGuanDaoPlayers = mutableMapOf<java.util.UUID, RanLiaoData>()

    private fun executeRanLiaoGuanDao(p: Player, s: SkillInstance, lv: Int): Boolean {
        val durationSec = when (lv) { 1 -> 30; 2 -> 35; 3 -> 40; else -> 30 }
        val range = when (lv) { 1 -> 4.0; 2 -> 6.0; 3 -> 8.0; else -> 4.0 }
        ranLiaoGuanDaoPlayers[p.uniqueId] = RanLiaoData(range, System.currentTimeMillis() + durationSec * 1000L)
        p.sendMessage(Component.text("§a✦ 燃料管道：周围${range.toInt()}格熔炉不消耗燃料，持续${durationSec}秒"))
        return true
    }

    /** Called each tick: keep nearby furnaces burning without fuel */
    fun tickRanLiaoGuanDao() {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<java.util.UUID>()
        for ((uuid, data) in ranLiaoGuanDaoPlayers) {
            val player = org.bukkit.Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline || now > data.expiry) {
                expired.add(uuid)
                continue
            }
            val range = data.range
            for (x in -range.toInt()..range.toInt()) {
                for (y in -2..2) {
                    for (z in -range.toInt()..range.toInt()) {
                        val block = player.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                        if (block.type == org.bukkit.Material.FURNACE || block.type == org.bukkit.Material.BLAST_FURNACE) {
                            val furnace = block.state as? org.bukkit.block.Furnace ?: continue
                            furnace.burnTime = 200.toShort() // Start / keep burning
                            furnace.update()
                        }
                    }
                }
            }
        }
        expired.forEach { ranLiaoGuanDaoPlayers.remove(it) }
    }

    /** 巧力用斧: 下次用斧破坏木方块不消耗耐久 */
    private val qiaoLiYongFuPlayers = mutableMapOf<java.util.UUID, Boolean>()

    private fun executeQiaoLiYongFu(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        qiaoLiYongFuPlayers[p.uniqueId] = true
        if (!silent) p.sendMessage(Component.text("§a✦ 巧力用斧：下次用斧破坏木方块不消耗耐久"))
        return true
    }

    /** Called from BlockBreakEvent — prevent durability damage if 巧力用斧 active */
    fun onQiaoLiYongFuBreak(player: Player): Boolean = qiaoLiYongFuPlayers.remove(player.uniqueId) ?: false

    /** 强力运镐: 下次用镐破坏石方块不消耗耐久 */
    private val qiangLiYunGaoPlayers = mutableMapOf<java.util.UUID, Boolean>()

    private fun executeQiangLiYunGao(p: Player, s: SkillInstance, lv: Int, silent: Boolean = false): Boolean {
        qiangLiYunGaoPlayers[p.uniqueId] = true
        if (!silent) p.sendMessage(Component.text("§a✦ 强力运镐：下次用镐破坏石方块不消耗耐久"))
        return true
    }

    /** Called from BlockBreakEvent — prevent durability damage if 强力运镐 active */
    fun onQiangLiYunGaoBreak(player: Player): Boolean {
        val has = qiangLiYunGaoPlayers.remove(player.uniqueId) ?: false
        return has
    }

    /** 熔金模具: 高炉额外经验 */
    private fun executeRongJinMuJu(p: Player, s: SkillInstance, lv: Int): Boolean {
        p.giveExp(2 + lv)
        p.sendMessage(Component.text("§a✦ 熔金模具：获得${2+lv}点额外经验"))
        return true
    }

    /** 我即梦魇: 怪物发光+缓慢 + 伤害加成(非力量) */
    private val nightmarePlayers = mutableMapOf<java.util.UUID, NightmareData>()
    data class NightmareData(val expiry: Long, val monsterRange: Double, val monsterSlowness: Int, val damageBonus: Double)

    private fun executeWoJiEMengYan(p: Player, s: SkillInstance, lv: Int): Boolean {
        val duration = when (lv) { 1 -> 60; 2 -> 75; 3 -> 90; else -> 60 }
        val range = when (lv) { 1 -> 16.0; 2 -> 24.0; 3 -> 32.0; else -> 16.0 }
        val slowness = if (lv >= 3) 1 else 0 // 缓慢I or 缓慢II
        val bonus = when (lv) { 1 -> 0.20; 2 -> 0.30; 3 -> 0.40; else -> 0.20 }
        nightmarePlayers[p.uniqueId] = NightmareData(
            System.currentTimeMillis() + duration * 1000L, range, slowness, bonus
        )
        // Immediately apply glow+slowness to nearby monsters
        val count = applyNightmareAura(p, range, slowness, duration)
        p.sendMessage(Component.text("§a✦ 我即梦魇：${count}只怪物被标记，对怪物伤害+${(bonus*100).toInt()}%，持续${duration}秒"))
        return true
    }

    /** Apply nightmare aura to nearby monsters, returns count */
    private fun applyNightmareAura(p: Player, range: Double, slowness: Int, durationSeconds: Int): Int {
        var count = 0
        for (entity in p.location.getNearbyEntities(range, range, range)) {
            if (entity is org.bukkit.entity.Monster) {
                entity.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, durationSeconds * 20, 0, false, true))
                entity.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, durationSeconds * 20, slowness, false, true))
                count++
            }
        }
        return count
    }

    /** Get 我即梦魇 damage bonus against monsters if active, or 0.0 */
    fun getNightmareBonus(player: Player): Double {
        val data = nightmarePlayers[player.uniqueId] ?: return 0.0
        if (System.currentTimeMillis() > data.expiry) { nightmarePlayers.remove(player.uniqueId); return 0.0 }
        return data.damageBonus
    }

    /** Reapply nightmare aura on move (called by PassiveSkillListener) */
    fun reapplyNightmareAura(player: Player): Boolean {
        val data = nightmarePlayers[player.uniqueId] ?: return false
        if (System.currentTimeMillis() > data.expiry) { nightmarePlayers.remove(player.uniqueId); return false }
        applyNightmareAura(player, data.monsterRange, data.monsterSlowness, ((data.expiry - System.currentTimeMillis()) / 1000L).toInt())
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
