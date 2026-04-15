package com.golf70.trainer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("DELETE FROM practice_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Insert
    suspend fun insertDrills(drills: List<DrillEntity>)

    @Insert
    suspend fun insertDrillResult(result: DrillResultEntity)

    @Transaction
    @Query("SELECT * FROM practice_sessions ORDER BY dateEpochMillis DESC")
    fun observeSessionsWithDrills(): Flow<List<PracticeSessionWithDrills>>

    @Transaction
    @Query("SELECT * FROM practice_sessions ORDER BY dateEpochMillis DESC")
    suspend fun getSessionsWithDrills(): List<PracticeSessionWithDrills>

    @Transaction
    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithDrills(sessionId: Long): PracticeSessionWithDrills?

    @Query("SELECT * FROM drills WHERE session_id = :sessionId ORDER BY orderInSession")
    suspend fun getDrillsForSession(sessionId: Long): List<DrillEntity>

    @Query(
        """
        SELECT COUNT(*) FROM practice_sessions
        WHERE dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun countSessionsBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(durationMinutes), 0) FROM practice_sessions
        WHERE dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun totalMinutesBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT COUNT(drill_results.id)
        FROM drill_results
        INNER JOIN drills ON drills.id = drill_results.drill_id
        INNER JOIN practice_sessions ON practice_sessions.id = drills.session_id
        WHERE drill_results.timestampEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun countDrillResultsBetween(startInclusive: Long, endExclusive: Long): Int
}

@Dao
interface RoundDao {
    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteRound(roundId: Long)

    @Insert
    suspend fun insertRound(round: RoundEntity): Long

    @Insert
    suspend fun insertHoles(holes: List<HoleStatEntity>)

    @Transaction
    @Query("SELECT * FROM rounds ORDER BY dateEpochMillis DESC")
    fun observeRoundsWithHoles(): Flow<List<RoundWithHoles>>

    @Query("SELECT AVG(score) FROM rounds")
    fun observeScoringAverage(): Flow<Float?>

    @Query(
        """
        SELECT COUNT(*) FROM rounds
        WHERE dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun countRoundsBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT AVG(score) FROM rounds
        WHERE dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun averageScoreBetween(startInclusive: Long, endExclusive: Long): Float?
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goalEntity: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = 1")
    fun observeGoal(): Flow<GoalEntity?>
}

@Dao
interface AnalyticsDao {
    @Query("SELECT COUNT(*) FROM hole_stats WHERE fairwayHit = 1")
    fun observeFairwaysHit(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hole_stats")
    fun observeTotalHoles(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hole_stats WHERE gir = 1")
    fun observeGirHit(): Flow<Int>

    @Query(
        """
        SELECT AVG(round_putts) FROM (
            SELECT SUM(putts) AS round_putts
            FROM hole_stats
            GROUP BY round_id
        )
        """
    )
    fun observeAveragePuttsPerRound(): Flow<Float?>

    @Query(
        """
        SELECT COUNT(*) FROM hole_stats
        INNER JOIN rounds ON rounds.id = hole_stats.round_id
        WHERE rounds.dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun totalHolesBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM hole_stats
        INNER JOIN rounds ON rounds.id = hole_stats.round_id
        WHERE fairwayHit = 1 AND rounds.dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun fairwaysBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM hole_stats
        INNER JOIN rounds ON rounds.id = hole_stats.round_id
        WHERE gir = 1 AND rounds.dateEpochMillis BETWEEN :startInclusive AND :endExclusive
        """
    )
    suspend fun girBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT AVG(round_putts) FROM (
            SELECT SUM(hole_stats.putts) AS round_putts
            FROM hole_stats
            INNER JOIN rounds ON rounds.id = hole_stats.round_id
            WHERE rounds.dateEpochMillis BETWEEN :startInclusive AND :endExclusive
            GROUP BY hole_stats.round_id
        )
        """
    )
    suspend fun averagePuttsPerRoundBetween(startInclusive: Long, endExclusive: Long): Float?
}

@Dao
interface CourseLayoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(layout: CourseLayoutEntity)

    @Query("SELECT * FROM course_layouts WHERE courseName = :courseName LIMIT 1")
    suspend fun getLayout(courseName: String): CourseLayoutEntity?

    @Query("SELECT courseName FROM course_layouts ORDER BY courseName COLLATE NOCASE ASC")
    suspend fun getCourseNames(): List<String>
}

@Dao
interface WeeklyPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: WeeklyPlanEntity)

    @Query("SELECT * FROM weekly_plans WHERE weekStartEpochDay = :weekStartEpochDay LIMIT 1")
    suspend fun getByWeekStart(weekStartEpochDay: Long): WeeklyPlanEntity?
}
