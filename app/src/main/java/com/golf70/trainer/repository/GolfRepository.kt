package com.golf70.trainer.repository

import com.golf70.trainer.data.local.DrillEntity
import com.golf70.trainer.data.local.DrillResultEntity
import com.golf70.trainer.data.local.GolfDatabase
import com.golf70.trainer.data.local.GoalEntity
import com.golf70.trainer.data.local.HoleStatEntity
import com.golf70.trainer.data.local.PracticeSessionEntity
import com.golf70.trainer.data.local.RoundEntity
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.domain.SessionDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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
}
