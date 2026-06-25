package com.waterful.project.career.manager

import com.waterful.project.career.model.CareerPlayer

object SkillPointManager {

    fun getPoints(player: CareerPlayer): Int = player.skillPoints

    fun addPoints(player: CareerPlayer, amount: Int) {
        player.skillPoints += amount
    }

    fun spendPoints(player: CareerPlayer, amount: Int): Boolean {
        if (player.skillPoints < amount) return false
        player.skillPoints -= amount
        return true
    }

    fun hasEnough(player: CareerPlayer, amount: Int): Boolean =
        player.skillPoints >= amount
}
