package com.golf70.trainer

import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.util.AdaptiveCoaching
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveCoachingTest {
    @Test
    fun lowFairway_generatesDriverRecommendation() {
        val recs = AdaptiveCoaching.recommendations(DashboardStats(40f, 55f, 32f, 80f))
        assertTrue(recs.any { it.contains("driver", ignoreCase = true) })
    }
}
