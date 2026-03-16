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
    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithDrills(sessionId: Long): PracticeSessionWithDrills?

    @Query("SELECT * FROM drills WHERE session_id = :sessionId ORDER BY orderInSession")
    suspend fun getDrillsForSession(sessionId: Long): List<DrillEntity>
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

    @Query("SELECT AVG(putts) FROM hole_stats")
    fun observeAveragePuttsPerHole(): Flow<Float?>
}


@Dao
interface CourseLayoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(layout: CourseLayoutEntity)

    @Query("SELECT * FROM course_layouts WHERE courseName = :courseName LIMIT 1")
    suspend fun getLayout(courseName: String): CourseLayoutEntity?
}
