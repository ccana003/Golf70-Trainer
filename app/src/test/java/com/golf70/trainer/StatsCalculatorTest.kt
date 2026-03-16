package com.golf70.trainer

import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.util.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {
    @Test
    fun summarizeRound_returnsExpectedValues() {
        val holes = listOf(
            HoleInput(1, 4, true, true, 2, 0),
            HoleInput(2, 5, false, false, 2, 1)
        )

        val summary = StatsCalculator.summarizeRound(holes)

        assertEquals(9, summary.totalScore)
        assertEquals(50f, summary.fairwayPercentage)
        assertEquals(50f, summary.girPercentage)
        assertEquals(4, summary.puttsPerRound)
    }
}
