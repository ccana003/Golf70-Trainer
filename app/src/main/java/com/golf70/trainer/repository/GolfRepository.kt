package com.golf70.trainer.repository

import com.golf70.trainer.data.local.CourseLayoutEntity
import com.golf70.trainer.data.local.DrillEntity
import com.golf70.trainer.data.local.DrillResultEntity
import com.golf70.trainer.data.local.GolfDatabase
import com.golf70.trainer.data.local.GoalEntity
import com.golf70.trainer.data.local.HoleStatEntity
import com.golf70.trainer.data.local.PracticeSessionEntity
import com.golf70.trainer.data.local.RoundEntity
import com.golf70.trainer.data.local.WeeklyPlanEntity
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.DrillDefinition
import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.domain.SeedSessions
import com.golf70.trainer.domain.SessionDefinition
import com.golf70.trainer.domain.WeeklyPlan
import com.golf70.trainer.domain.WeeklyProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class GolfRepository(
    private val db: GolfDatabase
) {
    val sessions = db.practiceSessionDao().observeSessionsWithDrills()
    val rounds = db.roundDao().observeRoundsWithHoles()
    val goal = db.goalDao().observeGoal()

    val dashboardStats: Flow<DashboardStats> = combine(
        db.analyticsDao().observeFairwaysHit(),
        db.analyticsDao().observeTotalHoles(),
        db.analyticsDao().observeGirHit(),
        db.analyticsDao().observeAveragePuttsPerHole(),
        db.roundDao().observeScoringAverage()
    ) { fairways, holes, gir, avgPuttsHole, scoreAvg ->
        val fairwayPct = if (holes == 0) 0f else fairways * 100f / holes
        val girPct = if (holes == 0) 0f else gir * 100f / holes
        DashboardStats(
            fairwayPercent = fairwayPct,
            girPercent = girPct,
            puttsPerRound = (avgPuttsHole ?: 0f) * 18,
            scoringAverage = scoreAvg ?: 0f
        )
    }


    suspend fun sessionLayouts(): List<SessionDefinition> {
        val customLayouts = db.practiceSessionDao().getSessionsWithDrills()
            .asSequence()
            .filter { it.drills.isNotEmpty() }
            .distinctBy { sessionWithDrills ->
                sessionWithDrills.drills.joinToString("|") { drill ->
                    listOf(drill.name, drill.instructions, drill.timerDurationSeconds.toString()).joinToString("~")
                }
            }
            .mapIndexed { index, sessionWithDrills ->
                SessionDefinition(
                    type = "Custom Layout ${index + 1}",
                    durationMinutes = sessionWithDrills.session.durationMinutes,
                    drills = sessionWithDrills.drills.map { drill ->
                        DrillDefinition(
                            title = drill.name,
                            instructions = drill.instructions,
                            timerSeconds = drill.timerDurationSeconds,
                            metrics = emptyList()
                        )
                    }
                )
            }
            .toList()

        val defaultLayout = SeedSessions.weeklyPlan.first().copy(type = "Default session layout")
        return listOf(defaultLayout) + customLayouts
    }

    suspend fun saveSession(definition: SessionDefinition): Long {
        val sessionId = db.practiceSessionDao().insertSession(
            PracticeSessionEntity(
                dateEpochMillis = System.currentTimeMillis(),
                type = definition.type,
                durationMinutes = definition.durationMinutes
            )
        )
        val drills = definition.drills.mapIndexed { idx, drill ->
            DrillEntity(
                sessionId = sessionId,
                name = drill.title,
                instructions = drill.instructions,
                timerDurationSeconds = drill.timerSeconds,
                orderInSession = idx
            )
        }
        db.practiceSessionDao().insertDrills(drills)
        return sessionId
    }

    suspend fun getDrillIdsForSession(sessionId: Long): List<Long> {
        return db.practiceSessionDao().getDrillsForSession(sessionId).map { it.id }
    }

    suspend fun saveDrillResult(
        drillId: Long,
        attempts: Int,
        successes: Int,
        direction: String?,
        distance: Float?
    ) {
        db.practiceSessionDao().insertDrillResult(
            DrillResultEntity(
                drillId = drillId,
                attempts = attempts,
                successes = successes,
                shotDirection = direction,
                distanceMeters = distance
            )
        )
    }

    suspend fun saveRound(course: String, holes: List<HoleInput>) {
        val roundId = db.roundDao().insertRound(
            RoundEntity(
                dateEpochMillis = System.currentTimeMillis(),
                course = course,
                score = holes.sumOf { it.score }
            )
        )
        db.roundDao().insertHoles(
            holes.map {
                HoleStatEntity(
                    roundId = roundId,
                    holeNumber = it.holeNumber,
                    par = it.par,
                    score = it.score,
                    fairwayHit = it.fairwayHit,
                    gir = it.gir,
                    putts = it.putts,
                    penalty = it.penalty
                )
            }
        )
    }

    suspend fun saveGoal(goalEntity: GoalEntity) = db.goalDao().upsert(goalEntity)

    suspend fun deleteSession(sessionId: Long) = db.practiceSessionDao().deleteSession(sessionId)

    suspend fun deleteRound(roundId: Long) = db.roundDao().deleteRound(roundId)

    suspend fun saveCourseLayout(courseName: String, pars: List<Int>) {
        db.courseLayoutDao().upsert(
            CourseLayoutEntity(
                courseName = courseName.trim(),
                parsCsv = pars.joinToString(",")
            )
        )
    }

    suspend fun getCourseLayout(courseName: String): List<Int>? {
        val layout = db.courseLayoutDao().getLayout(courseName.trim()) ?: return null
        val pars = layout.parsCsv.split(",").mapNotNull { it.toIntOrNull() }
        return pars.takeIf { it.size == 18 }
    }

    suspend fun getWeeklyPlanWithProgress(weekOffset: Int): WeeklyPlan {
        val weekStart = LocalDate.now().plusWeeks(weekOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
        val weekStartEpochDay = weekStart.toEpochDay()
        
        val planEntity = db.weeklyPlanDao().getByWeekStart(weekStartEpochDay) ?: run {
            val default = defaultWeeklyPlan(weekStart)
            val entity = WeeklyPlanEntity(
                weekStartEpochDay = weekStartEpochDay,
                targetSessions = default.targetSessions,
                targetMinutes = default.targetMinutes,
                targetRounds = default.targetRounds,
                targetDrillSaves = default.targetDrillSaves,
                notes = default.notes
            )
            db.weeklyPlanDao().upsert(entity)
            entity
        }

        val range = weekMillisRange(weekStart)
        val sessions = db.practiceSessionDao().countSessionsBetween(range.first, range.second)
        val minutes = db.practiceSessionDao().totalMinutesBetween(range.first, range.second)
        val rounds = db.roundDao().countRoundsBetween(range.first, range.second)
        val drillSaves = db.practiceSessionDao().countDrillResultsBetween(range.first, range.second)

        return WeeklyPlan(
            weekStart = weekStart,
            targetSessions = planEntity.targetSessions,
            targetMinutes = planEntity.targetMinutes,
            targetRounds = planEntity.targetRounds,
            targetDrillSaves = planEntity.targetDrillSaves,
            notes = planEntity.notes,
            completedSessions = sessions,
            completedMinutes = minutes,
            completedRounds = rounds,
            completedDrillSaves = drillSaves
        )
    }

    suspend fun weeklyProgress(weeks: Int = 8): List<WeeklyProgress> {
        val zoneId = ZoneId.systemDefault()
        val currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
        return (weeks - 1 downTo 0).map { idx ->
            val weekStart = currentWeekStart.minusWeeks(idx.toLong())
            val weekRange = weekMillisRange(weekStart)
            val holes = db.analyticsDao().totalHolesBetween(weekRange.first, weekRange.second)
            val fairways = db.analyticsDao().fairwaysBetween(weekRange.first, weekRange.second)
            val gir = db.analyticsDao().girBetween(weekRange.first, weekRange.second)
            val avgPutts = db.analyticsDao().averagePuttsPerHoleBetween(weekRange.first, weekRange.second)
            val avgScore = db.roundDao().averageScoreBetween(weekRange.first, weekRange.second)
            val sessions = db.practiceSessionDao().countSessionsBetween(weekRange.first, weekRange.second)
            val rounds = db.roundDao().countRoundsBetween(weekRange.first, weekRange.second)
            val minutes = db.practiceSessionDao().totalMinutesBetween(weekRange.first, weekRange.second)

            WeeklyProgress(
                weekStartEpochMillis = weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                label = "${weekStart.month.name.take(3)} ${weekStart.dayOfMonth}",
                sessions = sessions,
                rounds = rounds,
                practiceMinutes = minutes,
                fairwayPercent = if (holes == 0) 0f else fairways * 100f / holes,
                girPercent = if (holes == 0) 0f else gir * 100f / holes,
                puttsPerRound = (avgPutts ?: 0f) * 18,
                scoringAverage = avgScore ?: 0f
            )
        }
    }

    private fun defaultWeeklyPlan(weekStart: LocalDate): WeeklyPlan = WeeklyPlan(
        weekStart = weekStart,
        targetSessions = 3,
        targetMinutes = 180,
        targetRounds = 1,
        targetDrillSaves = 6,
        notes = "Sharpen driver start lines and inside-6ft putting confidence."
    )

    private fun weekMillisRange(weekStart: LocalDate): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val start = weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = weekStart.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return start to end
    }
}
