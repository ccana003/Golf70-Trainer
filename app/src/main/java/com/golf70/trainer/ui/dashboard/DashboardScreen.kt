package com.golf70.trainer.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles
import com.golf70.trainer.domain.DashboardStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    sessions: List<PracticeSessionWithDrills>,
    rounds: List<RoundWithHoles>
) {
    val sessionCount = sessions.size
    val totalPracticeMinutes = sessions.sumOf { it.session.durationMinutes }
    val totalDrills = sessions.sumOf { it.drills.size }
    val avgSessionMinutes = if (sessionCount == 0) 0 else totalPracticeMinutes / sessionCount
    val currentWeek = SimpleDateFormat("'Week of' MMM d", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Goal Dashboard", style = MaterialTheme.typography.headlineSmall)
        Text(currentWeek, style = MaterialTheme.typography.titleMedium)

        GoalRow("Fairway %", stats.fairwayPercent / 100f, "${stats.fairwayPercent.toInt()}%")
        GoalRow("GIR %", stats.girPercent / 100f, "${stats.girPercent.toInt()}%")
        GoalRow("Putts / Round", (40f - stats.puttsPerRound).coerceAtLeast(0f) / 40f, "${stats.puttsPerRound.toInt()}")
        GoalRow("Scoring Avg", (90f - stats.scoringAverage).coerceAtLeast(0f) / 20f, "${stats.scoringAverage}")

        Text("Practice Breakdown", style = MaterialTheme.typography.titleMedium)
        Text("Sessions logged: $sessionCount")
        Text("Total practice minutes: $totalPracticeMinutes")
        Text("Avg session length: $avgSessionMinutes min")
        Text("Total drills assigned: $totalDrills")

        Text("Scoring Rounds", style = MaterialTheme.typography.titleMedium)
        Text("Rounds logged: ${rounds.size}")
        val latestRound = rounds.firstOrNull()
        if (latestRound != null) {
            Text("Latest score: ${latestRound.round.score} on ${latestRound.round.course}")
        } else {
            Text("No rounds logged yet.")
        }
    }
}

@Composable
private fun GoalRow(title: String, progress: Float, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$title: $value")
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}
