package com.golf70.trainer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PracticeSessionEntity::class,
        DrillEntity::class,
        DrillResultEntity::class,
        RoundEntity::class,
        HoleStatEntity::class,
        GoalEntity::class,
        CourseLayoutEntity::class,
        WeeklyPlanEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GolfDatabase : RoomDatabase() {
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun roundDao(): RoundDao
    abstract fun goalDao(): GoalDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun courseLayoutDao(): CourseLayoutDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao
}
