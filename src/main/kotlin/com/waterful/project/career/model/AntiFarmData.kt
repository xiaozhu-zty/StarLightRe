package com.waterful.project.career.model

data class AntiFarmData(
    var totalDeaths: Int = 0,
    var lastNaturalRebirthTime: Long = 0L,
    var suicideCount: Int = 0,
    var consecutiveDeaths: Int = 0,
    val oneHourDeathTimestamps: MutableList<Long> = mutableListOf()
) {
    companion object {
        const val NATURAL_REBIRTH_COOLDOWN_MS: Long = 30L * 24 * 60 * 60 * 1000  // 30 days
        const val SUICIDE_PENALTY_THRESHOLD: Int = 6     // deaths per hour
        const val SUICIDE_PENALTY_DURATION_MS: Long = 24L * 60 * 60 * 1000  // 24 hours
        const val SUICIDE_PENALTY_WINDOW_MS: Long = 60L * 60 * 1000  // 1 hour
    }

    /** Check if natural rebirth (30-day cooldown) is available */
    fun canNaturalRebirth(now: Long): Boolean =
        (lastNaturalRebirthTime == 0L || now - lastNaturalRebirthTime >= NATURAL_REBIRTH_COOLDOWN_MS)
            && consecutiveDeaths == 0

    /** Record a death event */
    fun recordDeath(now: Long, isSuicide: Boolean) {
        totalDeaths++
        consecutiveDeaths++
        oneHourDeathTimestamps.add(now)
        // Clean up old entries outside the 1-hour window
        oneHourDeathTimestamps.removeAll { now - it > SUICIDE_PENALTY_WINDOW_MS }
        if (isSuicide) suicideCount++
    }

    /** Check if suicide penalty is active */
    fun isSuicidePenaltyActive(now: Long): Boolean {
        val recentDeaths = oneHourDeathTimestamps.count { now - it <= SUICIDE_PENALTY_DURATION_MS }
        return recentDeaths >= SUICIDE_PENALTY_THRESHOLD
    }

    /** Called on clean respawn */
    fun onRespawn() {
        consecutiveDeaths = 0
    }

    /** Mark natural rebirth as granted */
    fun markNaturalRebirth(now: Long) {
        lastNaturalRebirthTime = now
        consecutiveDeaths = 0
    }

    /** Get how many random classes a player gets on respawn (2 normally, 1 during penalty) */
    fun getRandomClassCount(now: Long): Int =
        if (isSuicidePenaltyActive(now)) 1 else 2
}
