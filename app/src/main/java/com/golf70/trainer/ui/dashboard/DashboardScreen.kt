package com.golf70.trainer.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Button(onClick = onWeekBack, modifier = Modifier.weight(1f)) { Text("Previous Week") }
                    Button(onClick = onWeekForward, modifier = Modifier.weight(1f)) { Text("Next Week") }
                }
                Text(
                    weeklyPlan?.weekStart?.format(DateTimeFormatter.ofPattern("'Week of' MMM d")) ?: "Loading week…",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (weeklyPlan != null) {
            item {
                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This week's goals", fontWeight = FontWeight.SemiBold)
                        weeklyPlan.tasks().forEach { task ->
                            Column {
                                Text("${task.title}: ${task.completed}/${task.target}", style = MaterialTheme.typography.bodySmall)
                                LinearProgressIndicator(
                                    progress = { task.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Text("Focus: ${weeklyPlan.notes}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overall Performance", style = MaterialTheme.typography.titleMedium)
                    GoalRow("Fairway %", stats.fairwayPercent / 100f, "${stats.fairwayPercent.toInt()}%")
                    GoalRow("GIR %", stats.girPercent / 100f, "${stats.girPercent.toInt()}%")
                    GoalRow("Putts / Round", (40f - stats.puttsPerRound).coerceAtLeast(0f) / 40f, "${stats.puttsPerRound.toInt()}")
                    GoalRow("Scoring Avg", (90f - stats.scoringAverage).coerceAtLeast(0f) / 20f, "${stats.scoringAverage}")
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Practice Stats", style = MaterialTheme.typography.titleMedium)
                    Text("Sessions: $sessionCount")
                    Text("Total minutes: $totalPracticeMinutes")
                    Text("Avg length: $avgSessionMinutes min")
                    Text("Total drills: $totalDrills")
                }
            }
        }

        item { Text("Recent Practice Sessions", style = MaterialTheme.typography.titleMedium) }
        items(sessions.take(10), key = { "sess_${it.session.id}" }) { session ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(session.session.type, fontWeight = FontWeight.Bold)
                        Text("${session.session.durationMinutes} min • ${session.drills.size} drills", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onDeleteSession(session.session.id) }) { Text("Remove") }
                }
            }
        }

        item { Text("Recent Scoring Rounds", style = MaterialTheme.typography.titleMedium) }
        items(rounds.take(10), key = { "round_${it.round.id}" }) { round ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(round.round.course, fontWeight = FontWeight.Bold)
                        Text("Score: ${round.round.score}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onDeleteRound(round.round.id) }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(title: String, progress: Float, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}
