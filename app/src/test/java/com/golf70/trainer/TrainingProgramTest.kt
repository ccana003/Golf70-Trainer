package com.golf70.trainer

import com.golf70.trainer.domain.TrainingProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingProgramTest {
    @Test
    fun `session structure remains fixed for all phases`() {
        val week1 = TrainingProgram.weekPlan(1)
        val week4 = TrainingProgram.weekPlan(4)
        val week7 = TrainingProgram.weekPlan(7)
        val week10 = TrainingProgram.weekPlan(10)

        val expectedOrder = listOf("Warm-up", "Full Swing", "Short Game", "Putting", "Pressure")

        listOf(week1, week4, week7, week10).forEach { week ->
            val titles = week.drills.map { it.title }
            assertEquals(5, titles.size)
            assertTrue(titles[0].startsWith(expectedOrder[0]))
            assertTrue(titles[1].startsWith(expectedOrder[1]))
            assertTrue(titles[2].startsWith(expectedOrder[2]))
            assertTrue(titles[3].startsWith(expectedOrder[3]))
            assertTrue(titles[4].startsWith(expectedOrder[4]))
        }
    }

    @Test
    fun `phase labels and focus map to required ranges`() {
        assertTrue(TrainingProgram.weekPlan(1).phase.contains("Phase 1"))
        assertTrue(TrainingProgram.weekPlan(3).phase.contains("Phase 2"))
        assertTrue(TrainingProgram.weekPlan(6).phase.contains("Phase 3"))
        assertTrue(TrainingProgram.weekPlan(9).phase.contains("Phase 4"))
    }
}
