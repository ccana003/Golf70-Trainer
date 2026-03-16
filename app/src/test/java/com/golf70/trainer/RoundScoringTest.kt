package com.golf70.trainer

import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.util.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundScoringTest {
    @Test
    fun eighteenHoleParRound_scores72() {
        val holes = (1..18).map { HoleInput(it, 4, 4, true, true, 2, 0) }
        val summary = StatsCalculator.summarizeRound(holes)
        assertEquals(72, summary.totalScore)
        assertEquals(100f, summary.fairwayPercentage)
    }
}
