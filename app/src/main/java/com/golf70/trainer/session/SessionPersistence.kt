package com.golf70.trainer.session

import android.content.Context

private const val SESSION_PREFS = "active_training_session"

data class PersistedSessionState(
    val currentWeek: Int,
    val selectedSessionIndex: Int,
    val currentDrillIndex: Int,
    val remainingTime: Int,
    val timerRunning: Boolean,
    val timerLastUpdatedTimestamp: Long,
    val sessionStartTimestamp: Long,
    val completedDrills: Set<Int>,
    val savedSessionId: Long?,
    val savedDrillIds: List<Long>
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

    fun saveManualWeekOverride(weekNumber: Int?) {
        prefs.edit().apply {
            if (weekNumber == null) remove(KEY_MANUAL_WEEK_OVERRIDE)
            else putInt(KEY_MANUAL_WEEK_OVERRIDE, weekNumber)
        }.apply()
    }

    fun manualWeekOverride(): Int? {
        val value = prefs.getInt(KEY_MANUAL_WEEK_OVERRIDE, -1)
        return value.takeIf { it > 0 }
    }

    fun save(state: PersistedSessionState) {
        prefs.edit()
            .putInt(KEY_CURRENT_WEEK, state.currentWeek)
            .putInt(KEY_SELECTED_SESSION_INDEX, state.selectedSessionIndex)
            .putInt(KEY_CURRENT_DRILL_INDEX, state.currentDrillIndex)
            .putInt(KEY_REMAINING_TIME, state.remainingTime)
            .putBoolean(KEY_TIMER_RUNNING, state.timerRunning)
            .putLong(KEY_TIMER_LAST_UPDATED_TIMESTAMP, state.timerLastUpdatedTimestamp)
            .putLong(KEY_SESSION_START_TIMESTAMP, state.sessionStartTimestamp)
            .putString(KEY_COMPLETED_DRILLS, state.completedDrills.sorted().joinToString(","))
            .putLong(KEY_SAVED_SESSION_ID, state.savedSessionId ?: -1L)
            .putString(KEY_SAVED_DRILL_IDS, state.savedDrillIds.joinToString(","))
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
        val savedSessionId = prefs.getLong(KEY_SAVED_SESSION_ID, -1L).takeIf { it > 0L }
        val savedDrillIds = prefs.getString(KEY_SAVED_DRILL_IDS, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toLongOrNull() }

        return PersistedSessionState(
            currentWeek = prefs.getInt(KEY_CURRENT_WEEK, 1),
            selectedSessionIndex = prefs.getInt(KEY_SELECTED_SESSION_INDEX, 0),
            currentDrillIndex = prefs.getInt(KEY_CURRENT_DRILL_INDEX, 0),
            remainingTime = prefs.getInt(KEY_REMAINING_TIME, 0),
            timerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false),
            timerLastUpdatedTimestamp = prefs.getLong(KEY_TIMER_LAST_UPDATED_TIMESTAMP, startTimestamp),
            sessionStartTimestamp = startTimestamp,
            completedDrills = completed,
            savedSessionId = savedSessionId,
            savedDrillIds = savedDrillIds
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
            .remove(KEY_SELECTED_SESSION_INDEX)
            .remove(KEY_SAVED_SESSION_ID)
            .remove(KEY_SAVED_DRILL_IDS)
            .apply()
    }

    companion object {
        private const val KEY_PROGRAM_START_TIMESTAMP = "program_start_timestamp"
        private const val KEY_CURRENT_WEEK = "current_week"
        private const val KEY_SELECTED_SESSION_INDEX = "selected_session_index"
        private const val KEY_CURRENT_DRILL_INDEX = "current_drill_index"
        private const val KEY_REMAINING_TIME = "remaining_time"
        private const val KEY_TIMER_RUNNING = "timer_running"
        private const val KEY_TIMER_LAST_UPDATED_TIMESTAMP = "timer_last_updated_timestamp"
        private const val KEY_SESSION_START_TIMESTAMP = "session_start_timestamp"
        private const val KEY_COMPLETED_DRILLS = "completed_drills"
        private const val KEY_SAVED_SESSION_ID = "saved_session_id"
        private const val KEY_SAVED_DRILL_IDS = "saved_drill_ids"
        private const val KEY_MANUAL_WEEK_OVERRIDE = "manual_week_override"
    }
}
