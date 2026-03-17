package com.golf70.trainer.session

import android.content.Context

private const val SESSION_PREFS = "active_training_session"

data class PersistedSessionState(
    val currentWeek: Int,
    val currentDrillIndex: Int,
    val remainingTime: Int,
    val timerRunning: Boolean,
    val timerLastUpdatedTimestamp: Long,
    val sessionStartTimestamp: Long,
    val completedDrills: Set<Int>
)

class SessionPersistence(context: Context) {
    private val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    fun saveProgramStartTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_PROGRAM_START_TIMESTAMP, timestamp).apply()
    }

    fun programStartTimestamp(): Long? {
        val value = prefs.getLong(KEY_PROGRAM_START_TIMESTAMP, -1L)
        return value.takeIf { it > 0 }
    }

    fun save(state: PersistedSessionState) {
        prefs.edit()
            .putInt(KEY_CURRENT_WEEK, state.currentWeek)
            .putInt(KEY_CURRENT_DRILL_INDEX, state.currentDrillIndex)
            .putInt(KEY_REMAINING_TIME, state.remainingTime)
            .putBoolean(KEY_TIMER_RUNNING, state.timerRunning)
            .putLong(KEY_TIMER_LAST_UPDATED_TIMESTAMP, state.timerLastUpdatedTimestamp)
            .putLong(KEY_SESSION_START_TIMESTAMP, state.sessionStartTimestamp)
            .putString(KEY_COMPLETED_DRILLS, state.completedDrills.sorted().joinToString(","))
            .apply()
    }

    fun load(): PersistedSessionState? {
        val startTimestamp = prefs.getLong(KEY_SESSION_START_TIMESTAMP, -1L)
        if (startTimestamp <= 0L) return null
        val completed = prefs.getString(KEY_COMPLETED_DRILLS, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()

        return PersistedSessionState(
            currentWeek = prefs.getInt(KEY_CURRENT_WEEK, 1),
            currentDrillIndex = prefs.getInt(KEY_CURRENT_DRILL_INDEX, 0),
            remainingTime = prefs.getInt(KEY_REMAINING_TIME, 0),
            timerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false),
            timerLastUpdatedTimestamp = prefs.getLong(KEY_TIMER_LAST_UPDATED_TIMESTAMP, startTimestamp),
            sessionStartTimestamp = startTimestamp,
            completedDrills = completed
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_CURRENT_WEEK)
            .remove(KEY_CURRENT_DRILL_INDEX)
            .remove(KEY_REMAINING_TIME)
            .remove(KEY_TIMER_RUNNING)
            .remove(KEY_TIMER_LAST_UPDATED_TIMESTAMP)
            .remove(KEY_SESSION_START_TIMESTAMP)
            .remove(KEY_COMPLETED_DRILLS)
            .apply()
    }

    companion object {
        private const val KEY_PROGRAM_START_TIMESTAMP = "program_start_timestamp"
        private const val KEY_CURRENT_WEEK = "current_week"
        private const val KEY_CURRENT_DRILL_INDEX = "current_drill_index"
        private const val KEY_REMAINING_TIME = "remaining_time"
        private const val KEY_TIMER_RUNNING = "timer_running"
        private const val KEY_TIMER_LAST_UPDATED_TIMESTAMP = "timer_last_updated_timestamp"
        private const val KEY_SESSION_START_TIMESTAMP = "session_start_timestamp"
        private const val KEY_COMPLETED_DRILLS = "completed_drills"
    }
}
