package com.golf70.trainer.util

import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.domain.RoundSummary

object StatsCalculator {
    fun summarizeRound(holes: List<HoleInput>): RoundSummary {
        if (holes.isEmpty()) return RoundSummary(0, 0f, 0f, 0)
        val totalScore = holes.sumOf { it.score }
        val fairways = holes.count { it.fairwayHit }
        val gir = holes.count { it.gir }
        val putts = holes.sumOf { it.putts }

        return RoundSummary(
            totalScore = totalScore,
            fairwayPercentage = fairways * 100f / holes.size,
            girPercentage = gir * 100f / holes.size,
            puttsPerRound = putts
        )
    }
}
