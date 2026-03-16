package com.golf70.trainer.util

import com.golf70.trainer.domain.DashboardStats

object AdaptiveCoaching {
    fun recommendations(stats: DashboardStats): List<String> {
        val recs = mutableListOf<String>()
        if (stats.fairwayPercent < 50f) recs += "Focus on driver accuracy drills this week."
        if (stats.girPercent < 50f) recs += "Add approach ladder and distance control practice."
        if (stats.puttsPerRound > 34f) recs += "Prioritize 3-6ft circle putting drills."
        if (recs.isEmpty()) recs += "Great trend. Maintain balanced practice mix."
        return recs
    }
}
