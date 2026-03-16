package com.golf70.trainer.domain

import java.time.LocalDate

data class DrillDefinition(
    val title: String,
    val instructions: String,
    val timerSeconds: Int,
    val metrics: List<MetricType>
)

enum class MetricType {
    LEFT, CENTER, RIGHT, ATTEMPTS, MADE, SUCCESS
}

data class SessionDefinition(
    val type: String,
    val durationMinutes: Int,
    val drills: List<DrillDefinition>
)

data class HoleInput(
    val holeNumber: Int,
    val par: Int,
    val score: Int,
    val fairwayHit: Boolean,
    val gir: Boolean,
    val putts: Int,
    val penalty: Int
)

data class RoundSummary(
    val totalScore: Int,
    val fairwayPercentage: Float,
    val girPercentage: Float,
    val puttsPerRound: Int
)

data class DashboardStats(
    val fairwayPercent: Float,
    val girPercent: Float,
    val puttsPerRound: Float,
    val scoringAverage: Float
)

data class WeeklyProgress(
    val weekStartEpochMillis: Long,
    val label: String,
    val sessions: Int,
    val rounds: Int,
    val practiceMinutes: Int,
    val fairwayPercent: Float,
    val girPercent: Float,
    val puttsPerRound: Float,
    val scoringAverage: Float
)

data class WeeklyTask(
    val title: String,
    val detail: String,
    val completed: Int,
    val target: Int
) {
    val progress: Float = if (target <= 0) 1f else completed.toFloat() / target
}

data class WeeklyPlan(
    val weekStart: LocalDate,
    val targetSessions: Int,
    val targetMinutes: Int,
    val targetRounds: Int,
    val targetDrillSaves: Int,
    val notes: String = "Build consistency with quality reps.",
    val completedSessions: Int = 0,
    val completedMinutes: Int = 0,
    val completedRounds: Int = 0,
    val completedDrillSaves: Int = 0
) {
    fun tasks(): List<WeeklyTask> = listOf(
        WeeklyTask("Practice sessions", "Structured range / short game sessions", completedSessions, targetSessions),
        WeeklyTask("Practice minutes", "Total focused minutes", completedMinutes, targetMinutes),
        WeeklyTask("Rounds logged", "Play and log scoring rounds", completedRounds, targetRounds),
        WeeklyTask("Drills saved", "Tracked drill outcomes", completedDrillSaves, targetDrillSaves)
    )
}
