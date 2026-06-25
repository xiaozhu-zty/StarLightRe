package com.waterful.project.career.model

import org.bukkit.Material

enum class CareerClass(
    val displayName: String,
    val description: String,
    val material: Material,
    val color: String
) {
    ARCHITECT(
        "建筑师",
        "作为汇聚起新大陆上每一片永恒的创造者，建筑师背负着建造的使命。他们熟悉装饰类方块的特点，在脚手架上来去自如，并对工程中的伤害具有一定防护能力。",
        Material.BRICKS,
        "#0070c0"
    ),
    CHEF(
        "厨师",
        "民以食为天。来到新大陆的人们早不再是茹毛饮血的野蛮人，他们需要那熟悉的味道来满足自己的基本需求——那些擅长屠宰、烹饪与酿造的厨师也就不可或缺。",
        Material.COOKED_BEEF,
        "#ffc000"
    ),
    SCHOLAR(
        "学者",
        "奇迹的从来不是星域何等广阔，奇迹的是人们竟用知识丈量了无垠星空。学者们或是深谙魔咒力量，或是熟稔电路架构，用他们的知识为新大陆作出独特贡献。",
        Material.ENCHANTING_TABLE,
        "#9933ff"
    ),
    FARMER(
        "农夫",
        "农业是生产方式最基础的产业，也是整个社会依赖程度最大的产业。农夫们活跃在广袤的田野中，在海滨的滩涂上，只为将新一季收成或又一尾鲜鱼放上餐桌。",
        Material.WHEAT,
        "#70AD47"
    ),
    WORKER(
        "工人",
        "万仞高塔拔地而起，靠的不是什么神力，而是一个个工人长期以来的辛勤付出。伐木、挖矿、烧炼、工具制造——一线的工人们总能精通于那么几项技能。",
        Material.IRON_PICKAXE,
        "#ED7D31"
    ),
    WARRIOR(
        "战士",
        "新大陆并不是一片宁静而和平之地，来自荒野的威胁使得有些勇者拿起了剑与弓。而当自然的挑战式微，聚落之间的利益冲突愈发白热化，战争终将破碎和平……",
        Material.DIAMOND_SWORD,
        "#FF0000"
    );

    companion object {
        fun fromName(name: String): CareerClass? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}
