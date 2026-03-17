package com.golf70.trainer.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.golf70.trainer.R
import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.WeeklyPlan
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    sessions: List<PracticeSessionWithDrills>,
    rounds: List<RoundWithHoles>,
    weeklyPlan: WeeklyPlan?,
    onWeekBack: () -> Unit,
    onWeekForward: () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onDeleteRound: (Long) -> Unit
) {
    val sessionCount = sessions.size
    val totalPracticeMinutes = sessions.sumOf { it.session.durationMinutes }
    val totalDrills = sessions.sumOf { it.drills.size }
    val avgSessionMinutes = if (sessionCount == 0) 0 else totalPracticeMinutes / sessionCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.ic_golf70_logo),
                contentDescription = "Golf70 logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
            Text("Goal Dashboard", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onWeekBack) { Text("Previous Week") }
                Button(onClick = onWeekForward) { Text("Next Week") }
            }
            Text(
                weeklyPlan?.weekStart?.format(DateTimeFormatter.ofPattern("'Week of' MMM d")) ?: "Loading week…",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (weeklyPlan != null) {
            item {
                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This week needs to be accomplished", fontWeight = FontWeight.SemiBold)
                        weeklyPlan.tasks().forEach { task ->
                            Text("${task.title}: ${task.completed}/${task.target}")
                            LinearProgressIndicator(
                                progress = { task.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text("Focus note: ${weeklyPlan.notes}")
                    }
                }
            }
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
