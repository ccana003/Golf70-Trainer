package com.golf70.trainer.util

import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles

object CsvExporter {
    fun exportSessions(sessions: List<PracticeSessionWithDrills>): String {
        val header = "session_id,date,type,duration_minutes,drill_count"
        val rows = sessions.map {
            "${it.session.id},${it.session.dateEpochMillis},${it.session.type},${it.session.durationMinutes},${it.drills.size}"
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun exportRounds(rounds: List<RoundWithHoles>): String {
        val header = "round_id,date,course,score,holes"
        val rows = rounds.map {
            "${it.round.id},${it.round.dateEpochMillis},${it.round.course},${it.round.score},${it.holes.size}"
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}
