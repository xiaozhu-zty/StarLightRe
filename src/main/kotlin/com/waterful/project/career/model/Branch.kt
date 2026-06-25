package com.waterful.project.career.model

enum class Branch(
    val careerClass: CareerClass,
    val displayName: String,
    val description: String,
    val baseEffect: String,
    val branchMaterial: String  // Material name for branch icon from spreadsheet
) {
    // ===== 建筑师 Architect (4) =====
    ARCHITECT_STRUCTURE(
        CareerClass.ARCHITECT, "结构工程师",
        "明察建筑骨体，设计庞然巨构",
        "打火石时必定成功；减少20%爆炸伤害",
        "scaffolding"
    ),
    ARCHITECT_FORTRESS(
        CareerClass.ARCHITECT, "堡垒工程师",
        "壁墙固若磐石，筑垒坚不可摧",
        "减少20%摔落伤害",
        "stone_bricks"
    ),
    ARCHITECT_TRAFFIC(
        CareerClass.ARCHITECT, "交通工程师",
        "穿行街头巷尾，连通千家万户",
        "乘坐矿车、各类船、各类运输船时以上运输工具的速度+20%",
        "minecart"
    ),
    ARCHITECT_DEMOLITION(
        CareerClass.ARCHITECT, "爆破师",
        "点燃火药造物，畅享解构美学",
        "合成TNT、TNT矿车时必定成功",
        "tnt"
    ),

    // ===== 厨师 Chef (4) =====
    CHEF_BREWER(
        CareerClass.CHEF, "药剂师",
        "萃取自然精华，酿造魔幻体验",
        "合成谜之炖菜时必定成功（无药剂师时有50%概率失败）",
        "potion"
    ),
    CHEF_BAKER(
        CareerClass.CHEF, "烘焙师",
        "烘烤喷香美食，敬献甜蜜滋味",
        "合成烘焙食品和汤煲食品时成功概率提升",
        "bread"
    ),
    CHEF_BUTCHER(
        CareerClass.CHEF, "屠夫",
        "透视技经肯綮，执刀游刃有余",
        "击杀动物时额外获得1个骨头",
        "leather"
    ),
    CHEF_MASTER(
        CareerClass.CHEF, "主厨",
        "掌握顶级厨艺，造就极品珍馐",
        "食用任意食物后额外获得1饥饿值；食用金苹果后立即恢复所有饥饿值",
        "golden_apple"
    ),

    // ===== 学者 Scholar (4) =====
    SCHOLAR_ENCHANTER(
        CareerClass.SCHOLAR, "附魔师",
        "沉吟古老魔咒，赋予崭新奥秘",
        "可以使用砂轮的所有功能；可在铁砧上结合物品与附魔",
        "enchanting_table"
    ),
    SCHOLAR_REDSTONE(
        CareerClass.SCHOLAR, "红石工程师",
        "任使电路交错，自有逻辑纵横",
        "可以合成漏斗、发射器、投掷器",
        "repeater"
    ),
    SCHOLAR_ADMIN(
        CareerClass.SCHOLAR, "行政专家",
        "用人举贤任能，理事算无遗策",
        "（暂不实装，待更新）可以担任所属势力的领导人；可以创建势力",
        "white_banner"
    ),
    SCHOLAR_TEACHER(
        CareerClass.SCHOLAR, "教师",
        "传递世代智慧，见证桃李成蹊",
        "右键使用知识之书获得1技能点（教师分支玩家无法使用）",
        "lectern"
    ),

    // ===== 农夫 Farmer (4) =====
    FARMER_FISHERMAN(
        CareerClass.FARMER, "渔夫",
        "搏击怒海狂涛，追猎鲜美海味",
        "始终获得幸运I效果；处于水中时获得潮涌能量效果",
        "fishing_rod"
    ),
    FARMER_BOTANIST(
        CareerClass.FARMER, "植物学家",
        "观测植株成长，培植新生绿意",
        "收获作物时有概率额外获得1个相应作物",
        "wheat_seeds"
    ),
    FARMER_RANCHER(
        CareerClass.FARMER, "牧场主",
        "放眼辽阔天地，奏响和美牧歌",
        "可以剪羊毛、挤奶；可以喂养、繁殖、染色动物；剪羊毛/挤奶获得2经验；喂养/染色获得4经验",
        "egg"
    ),
    FARMER_MERCHANT(
        CareerClass.FARMER, "行商",
        "游历山河阡陌，达成公平交易",
        "可以与村民交易；处于村庄中时获得生命恢复I；达成购买交易后有10%概率返还20%绿宝石",
        "emerald"
    ),

    // ===== 工人 Worker (4) =====
    WORKER_LUMBERJACK(
        CareerClass.WORKER, "伐木工",
        "挥动锋利巨斧，裂断树木纤维",
        "破坏木方块时获得急迫I",
        "iron_axe"
    ),
    WORKER_MINER(
        CareerClass.WORKER, "矿工",
        "开凿深层矿道，发掘瑰丽矿石",
        "破坏自然石方块时获得急迫I",
        "iron_pickaxe"
    ),
    WORKER_SMELTER(
        CareerClass.WORKER, "烧炼师",
        "点燃炽热火炉，成就百炼之钢",
        "可以将煤炭/煤炭块/岩浆桶/烈焰棒放入熔炉或高炉；取出烧炼产物时有5%概率额外获得1个产物",
        "furnace"
    ),
    WORKER_TOOLMAKER(
        CareerClass.WORKER, "工具制造商",
        "打造趁手器具，接续不熄匠魂",
        "可以合成钻石工具、指南针、钟表、望远镜、剪刀、栓绳",
        "crafting_table"
    ),

    // ===== 战士 Warrior (4) =====
    WARRIOR_WEAPON(
        CareerClass.WARRIOR, "武器专家",
        "使得百般兵器，身法进退自如",
        "主手持剑时受到的伤害降低8%",
        "diamond_sword"
    ),
    WARRIOR_SOLDIER(
        CareerClass.WARRIOR, "士兵",
        "进攻侵掠如火，防御不动如山",
        "生命值低于20时获得抗性提升I效果",
        "shield"
    ),
    WARRIOR_HUNTER(
        CareerClass.WARRIOR, "怪物猎人",
        "剿除魑魅魍魉，守望黑夜微光",
        "对精英怪物的伤害+50%；获得0.75体力修正；始终获得迅捷I效果",
        "bow"
    ),
    WARRIOR_EXPLORER(
        CareerClass.WARRIOR, "探险家",
        "探索未知领域，拓展文明边界",
        "在夜晚的室外时黑暗环境耐受时间+10s；生命值低于5时获得力量I效果",
        "compass"
    );

    /** Potion effect associated with this branch (for resonance) */
    val resonanceEffect: String?
        get() = when (this) {
            // Architect - Haste / Speed
            ARCHITECT_STRUCTURE -> "HASTE"
            ARCHITECT_FORTRESS -> "RESISTANCE"
            ARCHITECT_TRAFFIC -> "SPEED"
            ARCHITECT_DEMOLITION -> "RESISTANCE"
            // Chef - Regeneration / Resistance / Strength
            CHEF_BREWER -> "REGENERATION"
            CHEF_BAKER -> "REGENERATION"
            CHEF_BUTCHER -> "STRENGTH"
            CHEF_MASTER -> "REGENERATION"
            // Scholar - various
            SCHOLAR_ENCHANTER -> "LUCK"
            SCHOLAR_REDSTONE -> "HASTE"
            SCHOLAR_ADMIN -> "SPEED"
            SCHOLAR_TEACHER -> "LUCK"
            // Farmer
            FARMER_FISHERMAN -> "LUCK"
            FARMER_BOTANIST -> "HASTE"
            FARMER_RANCHER -> "REGENERATION"
            FARMER_MERCHANT -> "REGENERATION"
            // Worker
            WORKER_LUMBERJACK -> "HASTE"
            WORKER_MINER -> "HASTE"
            WORKER_SMELTER -> "HASTE"
            WORKER_TOOLMAKER -> "LUCK"
            // Warrior
            WARRIOR_WEAPON -> "STRENGTH"
            WARRIOR_SOLDIER -> "RESISTANCE"
            WARRIOR_HUNTER -> "STRENGTH"
            WARRIOR_EXPLORER -> "SPEED"
        }

    companion object {
        const val SKILLS_PER_BRANCH = 3
        const val EUREKAS_PER_BRANCH = 3

        fun fromCareerClass(careerClass: CareerClass): List<Branch> =
            entries.filter { it.careerClass == careerClass }

        fun fromName(name: String): Branch? =
            entries.find { it.name.equals(name, ignoreCase = true) }

        fun getPassiveSkillIndex(): Int = 0
        fun getActiveSkill2Index(): Int = 1
        fun getActiveSkill3Index(): Int = 2
    }
}
