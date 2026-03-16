package com.golf70.trainer.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles
import com.golf70.trainer.domain.DashboardStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    sessions: List<PracticeSessionWithDrills>,
    rounds: List<RoundWithHoles>,
    onDeleteSession: (Long) -> Unit,
    onDeleteRound: (Long) -> Unit
) {
    val sessionCount = sessions.size
    val totalPracticeMinutes = sessions.sumOf { it.session.durationMinutes }
    val totalDrills = sessions.sumOf { it.drills.size }
    val avgSessionMinutes = if (sessionCount == 0) 0 else totalPracticeMinutes / sessionCount
    var weekOffset by remember { mutableStateOf(0) }
    val currentWeek = remember(weekOffset) {
        val cal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, weekOffset) }
        SimpleDateFormat("'Week of' MMM d", Locale.getDefault()).format(cal.time)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Goal Dashboard", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { weekOffset -= 1 }) { Text("Previous Week") }
                Button(onClick = { weekOffset += 1 }) { Text("Next Week") }
            }
            Text(currentWeek, style = MaterialTheme.typography.titleMedium)
        }

        item { GoalRow("Fairway %", stats.fairwayPercent / 100f, "${stats.fairwayPercent.toInt()}%") }
        item { GoalRow("GIR %", stats.girPercent / 100f, "${stats.girPercent.toInt()}%") }
        item { GoalRow("Putts / Round", (40f - stats.puttsPerRound).coerceAtLeast(0f) / 40f, "${stats.puttsPerRound.toInt()}") }
        item { GoalRow("Scoring Avg", (90f - stats.scoringAverage).coerceAtLeast(0f) / 20f, "${stats.scoringAverage}") }

        item {
            Text("Practice Breakdown", style = MaterialTheme.typography.titleMedium)
            Text("Sessions logged: $sessionCount")
            Text("Total practice minutes: $totalPracticeMinutes")
            Text("Avg session length: $avgSessionMinutes min")
            Text("Total drills assigned: $totalDrills")
        }

        item { Text("Logged Practice Sessions", style = MaterialTheme.typography.titleMedium) }
        items(sessions.take(10), key = { it.session.id }) { session ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${session.session.type} • ${session.session.durationMinutes} min")
                Button(onClick = { onDeleteSession(session.session.id) }) { Text("Remove") }
            }
        }

        item {
            Text("Scoring Rounds", style = MaterialTheme.typography.titleMedium)
            Text("Rounds logged: ${rounds.size}")
        }
        items(rounds.take(10), key = { it.round.id }) { round ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${round.round.course}: ${round.round.score}")
                Button(onClick = { onDeleteRound(round.round.id) }) { Text("Remove") }
            }
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
