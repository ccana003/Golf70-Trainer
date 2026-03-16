package com.golf70.trainer.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMillis: Long,
    val type: String,
    val durationMinutes: Int
)

@Entity(
    tableName = "drills",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class DrillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val name: String,
    val instructions: String,
    val timerDurationSeconds: Int,
    val orderInSession: Int
)

@Entity(
    tableName = "drill_results",
    foreignKeys = [
        ForeignKey(
            entity = DrillEntity::class,
            parentColumns = ["id"],
            childColumns = ["drill_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("drill_id")]
)
data class DrillResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "drill_id") val drillId: Long,
    val attempts: Int,
    val successes: Int,
    val shotDirection: String?,
    val distanceMeters: Float?,
    val timestampEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMillis: Long,
    val course: String,
    val score: Int
)

@Entity(
    tableName = "hole_stats",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("round_id")]
)
data class HoleStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "round_id") val roundId: Long,
    val holeNumber: Int,
    val par: Int,
    val score: Int,
    val fairwayHit: Boolean,
    val gir: Boolean,
    val putts: Int,
    val penalty: Int
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: Int = 1,
    val targetScore: Int = 70,
    val targetFairwayPercent: Float = 60f,
    val targetGirPercent: Float = 55f,
    val targetPuttsPerRound: Float = 32f
)
