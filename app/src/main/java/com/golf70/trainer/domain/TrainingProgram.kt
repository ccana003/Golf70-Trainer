package com.golf70.trainer.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TrainingWeek(
    val weekNumber: Int,
    val phase: String,
    val focus: String,
    val drills: List<DrillDefinition>
)

object TrainingProgram {
    private const val WARMUP_SECONDS = 300
    private const val SEGMENT_SECONDS = 600

    fun resolveWeek(programStartDate: LocalDate, today: LocalDate = LocalDate.now()): TrainingWeek {
        val elapsedDays = ChronoUnit.DAYS.between(programStartDate, today).coerceAtLeast(0)
        val weekNumber = (elapsedDays / 7).toInt() + 1
        return weekPlan(weekNumber)
    }

    fun weekPlan(weekNumber: Int): TrainingWeek {
        return when (weekNumber) {
            in 1..2 -> trainingWeek(
                weekNumber = weekNumber,
                phase = "Phase 1 (Weeks 1–2)",
                focus = "Contact + Fundamentals",
                fullSwing = DrillDefinition(
                    title = "Full Swing: Contact + Face Control",
                    instructions = "Strike center-face shots and track start direction quality.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.LEFT, MetricType.CENTER, MetricType.RIGHT)
                ),
                shortGame = DrillDefinition(
                    title = "Short Game: Basic Chips",
                    instructions = "Chip from simple lies and focus on solid contact.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                ),
                putting = DrillDefinition(
                    title = "Putting: 3–6ft Make Zone",
                    instructions = "Build confidence by repeating short putts from 3–6 feet.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.MADE)
                ),
                pressure = DrillDefinition(
                    title = "Pressure: Fundamentals Consequence Ladder",
                    instructions = "Complete each station cleanly before moving to the next.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                )
            )

            in 3..5 -> trainingWeek(
                weekNumber = weekNumber,
                phase = "Phase 2 (Weeks 3–5)",
                focus = "Consistency + Dispersion",
                fullSwing = DrillDefinition(
                    title = "Full Swing: Start Line + Dispersion",
                    instructions = "Hit to a start-line gate and score dispersion windows.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.LEFT, MetricType.CENTER, MetricType.RIGHT)
                ),
                shortGame = DrillDefinition(
                    title = "Short Game: Distance Control",
                    instructions = "Land chips/pitches in controlled carry windows.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                ),
                putting = DrillDefinition(
                    title = "Putting: Lag Putting",
                    instructions = "Focus on speed control and leave putts in a tight finish zone.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                ),
                pressure = DrillDefinition(
                    title = "Pressure: Dispersion Challenge",
                    instructions = "Consequence scoring for misses outside target windows.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                )
            )

            in 6..8 -> trainingWeek(
                weekNumber = weekNumber,
                phase = "Phase 3 (Weeks 6–8)",
                focus = "Scoring",
                fullSwing = DrillDefinition(
                    title = "Full Swing: Fairways + Targets",
                    instructions = "Choose fairway targets and track hit percentage.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.LEFT, MetricType.CENTER, MetricType.RIGHT)
                ),
                shortGame = DrillDefinition(
                    title = "Short Game: Up-and-Down %",
                    instructions = "Simulate scoring lies and track conversion percentage.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                ),
                putting = DrillDefinition(
                    title = "Putting: 6–10ft Make %",
                    instructions = "Track make percentage from scoring range putts.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.MADE)
                ),
                pressure = DrillDefinition(
                    title = "Pressure: Scoring Simulation",
                    instructions = "Play consequence-based stations and post a final score.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                )
            )

            else -> trainingWeek(
                weekNumber = weekNumber,
                phase = "Phase 4 (Week 9+ / July ramp-up)",
                focus = "Pressure + Tournament Prep",
                fullSwing = DrillDefinition(
                    title = "Full Swing: Tournament Targets",
                    instructions = "Every rep is target-based with miss penalties.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.LEFT, MetricType.CENTER, MetricType.RIGHT)
                ),
                shortGame = DrillDefinition(
                    title = "Short Game: Consequence Up-and-Down",
                    instructions = "Score every ball with consequence for failed saves.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                ),
                putting = DrillDefinition(
                    title = "Putting: Pressure Make Matrix",
                    instructions = "Track make percentage under target and consequence rules.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.MADE)
                ),
                pressure = DrillDefinition(
                    title = "Pressure: Tournament Simulation",
                    instructions = "Run a scoring-focused simulation with no free misses.",
                    timerSeconds = SEGMENT_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)
                )
            )
        }
    }

    private fun trainingWeek(
        weekNumber: Int,
        phase: String,
        focus: String,
        fullSwing: DrillDefinition,
        shortGame: DrillDefinition,
        putting: DrillDefinition,
        pressure: DrillDefinition
    ): TrainingWeek {
        return TrainingWeek(
            weekNumber = weekNumber,
            phase = phase,
            focus = focus,
            drills = listOf(
                DrillDefinition(
                    title = "Warm-up",
                    instructions = "Loosen up, mobility, and strike easy wedge shots.",
                    timerSeconds = WARMUP_SECONDS,
                    metrics = listOf(MetricType.ATTEMPTS)
                ),
                fullSwing,
                shortGame,
                putting,
                pressure
            )
        )
    }
}
