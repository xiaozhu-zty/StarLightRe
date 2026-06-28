package com.waterful.project.career.model

enum class Branch(
    val careerClass: CareerClass,
    val displayName: String,
    val description: String,
    val baseEffect: String,
    val branchMaterial: String
) {
    // ===== 建筑师 Architect =====
    ARCHITECT_STRUCTURE(CareerClass.ARCHITECT, "结构工程师",
        "明察建筑骨体，设计庞然巨构",
        "可合成并放置多种装饰性方块(钟/雕纹书架等)、染色方块、玻璃、灯笼、木桶、末影箱、潜影盒、展示框、旗帜、地毯、书架；可放置脚手架；可使用织布机",
        "scaffolding"),
    ARCHITECT_FORTRESS(CareerClass.ARCHITECT, "堡垒工程师",
        "壁墙固若磐石，筑垒坚不可摧",
        "可合成各类墙和铁栏杆；放置脚手架；使用切石机；破坏各类砖块可得掉落物；减少20%摔落伤害",
        "stone_bricks"),
    ARCHITECT_TRAFFIC(CareerClass.ARCHITECT, "交通工程师",
        "穿行街头巷尾，连通千家万户",
        "可合成并放置各类铁轨、矿车及各种船、浮冰和蓝冰；破坏铁轨可得掉落物；乘坐矿车/船速度+20%",
        "minecart"),
    ARCHITECT_DEMOLITION(CareerClass.ARCHITECT, "爆破师",
        "点燃火药造物，畅享解构美学",
        "可合成TNT/TNT矿车但50%失败(失败有50%爆炸)；可放置末地水晶；可点燃TNT和苦力怕；合成火焰弹；打火石必定成功；减20%爆炸伤害",
        "tnt"),

    // ===== 厨师 Chef =====
    CHEF_BREWER(CareerClass.CHEF, "药剂师",
        "萃取自然精华，酿造魔幻体验",
        "可使用酿造台；临近6格可放漏斗/发射器/投掷器；合成谜之炖菜必定成功",
        "potion"),
    CHEF_BAKER(CareerClass.CHEF, "烘焙师",
        "烘烤喷香美食，敬献甜蜜滋味",
        "合成烘焙/汤煲食品成功率75%；食用后额外+2饥饿值；合成蛋糕额外+1个",
        "bread"),
    CHEF_BUTCHER(CareerClass.CHEF, "屠夫",
        "透视技经肯綮，执刀游刃有余",
        "使用斧全额伤害；食用熟肉额外+4饥饿值；用斧击杀动物额外掉1骨头",
        "leather"),
    CHEF_MASTER(CareerClass.CHEF, "主厨",
        "掌握顶级厨艺，造就极品珍馐",
        "可使用烟熏炉；临近可放漏斗等；可合成金苹果/金胡萝卜/闪烁西瓜；食用任意食物额外+1饥饿值；食用金苹果立即回满饥饿值",
        "golden_apple"),

    // ===== 学者 Scholar =====
    SCHOLAR_ENCHANTER(CareerClass.SCHOLAR, "附魔师",
        "沉吟古老魔咒，赋予崭新奥秘",
        "可进行≥16级附魔；放置附魔台；使用砂轮；铁砧上结合附魔书/物品；经验等级提升时获得10经验",
        "enchanting_table"),
    SCHOLAR_REDSTONE(CareerClass.SCHOLAR, "红石工程师",
        "任使电路交错，自有逻辑纵横",
        "可合成并放置进阶红石器械(侦测器/比较器/活塞等)；合成激活/探测铁轨；破坏红石粉和进阶器械可得掉落物",
        "repeater"),
    SCHOLAR_ADMIN(CareerClass.SCHOLAR, "行政专家",
        "用人举贤任能，理事算无遗策",
        "可使用人事目录查看周围32格玩家职业；可担任势力领导人/创建势力(暂未实装)",
        "white_banner"),
    SCHOLAR_TEACHER(CareerClass.SCHOLAR, "教师",
        "传递世代智慧，见证桃李成蹊",
        "从生存里程碑额外+1技能点；可合成讲台/书架；Shift+右键讲台消耗经验+技能点获技能之书；右键书架消耗技能点获书(教师本人无法使用)",
        "lectern"),

    // ===== 农夫 Farmer =====
    FARMER_FISHERMAN(CareerClass.FARMER, "渔夫",
        "搏击怒海狂涛，追猎鲜美海味",
        "始终获得幸运I；处于水中时获得潮涌能量",
        "fishing_rod"),
    FARMER_BOTANIST(CareerClass.FARMER, "植物学家",
        "观测植株成长，培植新生绿意",
        "可放置各类树苗；使用骨粉和堆肥桶；堆肥桶旁放漏斗；收割作物有10%额外获得",
        "wheat_seeds"),
    FARMER_RANCHER(CareerClass.FARMER, "牧场主",
        "放眼辽阔天地，奏响和美牧歌",
        "可剪羊毛、挤奶；可喂养/繁殖/染色动物；剪毛/挤奶得2经验，喂养/染色得4经验",
        "egg"),
    FARMER_MERCHANT(CareerClass.FARMER, "行商",
        "游历山河阡陌，达成公平交易",
        "可与村民交易；处于村庄中获得生命恢复I；购买交易有10%返还20%绿宝石",
        "emerald"),

    // ===== 工人 Worker =====
    WORKER_LUMBERJACK(CareerClass.WORKER, "伐木工",
        "挥动锋利巨斧，裂断树木纤维",
        "斧全额伤害；可用钻石/下界合金斧；破坏原木/菌柄必定掉落；破坏木方块获得急迫I",
        "iron_axe"),
    WORKER_MINER(CareerClass.WORKER, "矿工",
        "开凿深层矿道，发掘瑰丽矿石",
        "镐/锹全额伤害；可用钻石/下界合金镐/锹；破坏矿石必定掉落；破坏自然石方块获急迫I",
        "iron_pickaxe"),
    WORKER_SMELTER(CareerClass.WORKER, "烧炼师",
        "点燃炽热火炉，成就百炼之钢",
        "可使用高炉；临近可放漏斗等；可使用煤炭/岩浆/烈焰棒作燃料；从熔炉/高炉取出物品有5%概率额外+1",
        "furnace"),
    WORKER_TOOLMAKER(CareerClass.WORKER, "工具制造商",
        "打造趁手器具，接续不熄匠魂",
        "斧/镐/锹全额伤害；可使用锻造台；使用砂轮；铁砧修复/重命名工具；合成钻石工具/指南针/钟/望远镜/剪刀/栓绳",
        "crafting_table"),

    // ===== 战士 Warrior =====
    WARRIOR_WEAPON(CareerClass.WARRIOR, "武器专家",
        "使得百般兵器，身法进退自如",
        "任意武器全额伤害；三叉戟伤害+2；弩25%不消耗普通箭；合成钻石武器/防具；可使用锻造台/铁砧/砂轮操作武器防具",
        "diamond_sword"),
    WARRIOR_SOLDIER(CareerClass.WARRIOR, "士兵",
        "进攻侵掠如火，防御不动如山",
        "剑/斧/弓全额伤害；穿全套盔甲受伤-2；对玩家伤害+10%；生命<10获抗性提升I",
        "shield"),
    WARRIOR_HUNTER(CareerClass.WARRIOR, "怪物猎人",
        "剿除魑魅魍魉，守望黑夜微光",
        "剑/斧/弓全额伤害；对怪物伤害+30%；生命<10获力量I",
        "bow"),
    WARRIOR_EXPLORER(CareerClass.WARRIOR, "探险家",
        "探索未知领域，拓展文明边界",
        "剑/斧/弓全额伤害；对巨兽生物伤害+50%；体力修正0.75；始终迅捷I",
        "compass");

    val resonanceEffect: String?
        get() = when (this) {
            ARCHITECT_STRUCTURE -> "HASTE"; ARCHITECT_FORTRESS -> "RESISTANCE"
            ARCHITECT_TRAFFIC -> "SPEED"; ARCHITECT_DEMOLITION -> "RESISTANCE"
            CHEF_BREWER -> "REGENERATION"; CHEF_BAKER -> "REGENERATION"
            CHEF_BUTCHER -> "STRENGTH"; CHEF_MASTER -> "REGENERATION"
            SCHOLAR_ENCHANTER -> "LUCK"; SCHOLAR_REDSTONE -> "HASTE"
            SCHOLAR_ADMIN -> "SPEED"; SCHOLAR_TEACHER -> "LUCK"
            FARMER_FISHERMAN -> "LUCK"; FARMER_BOTANIST -> "HASTE"
            FARMER_RANCHER -> "REGENERATION"; FARMER_MERCHANT -> "REGENERATION"
            WORKER_LUMBERJACK -> "HASTE"; WORKER_MINER -> "HASTE"
            WORKER_SMELTER -> "HASTE"; WORKER_TOOLMAKER -> "LUCK"
            WARRIOR_WEAPON -> "STRENGTH"; WARRIOR_SOLDIER -> "RESISTANCE"
            WARRIOR_HUNTER -> "STRENGTH"; WARRIOR_EXPLORER -> "SPEED"
        }

    companion object {
        const val SKILLS_PER_BRANCH = 3; const val EUREKAS_PER_BRANCH = 3
        fun fromCareerClass(c: CareerClass) = entries.filter { it.careerClass == c }
        fun fromName(name: String) = entries.find { it.name.equals(name, ignoreCase = true) }
        fun getPassiveSkillIndex() = 0
    }
}
