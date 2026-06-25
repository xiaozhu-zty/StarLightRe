package com.waterful.project.career.model

enum class ResonanceMode(
    val displayName: String,
    val description: String
) {
    DISABLED("关闭", "不进行共鸣，仅保留共鸣分支的前缀显示"),
    GLOBAL("全局共鸣", "共鸣给范围内的所有人（包括敌对势力）"),
    FRIENDLY("友善共鸣", "共鸣给范围内非敌对的所有人"),
    INTERNAL("内部共鸣", "共鸣给范围内属于己方势力的所有人"),
    SOLO_ECHO("独奏回响", "不共鸣他人，自身共鸣效果翻倍");

    companion object {
        fun fromName(name: String): ResonanceMode? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}
