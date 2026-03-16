package com.golf70.trainer.domain

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
