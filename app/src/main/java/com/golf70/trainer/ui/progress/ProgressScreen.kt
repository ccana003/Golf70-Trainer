package com.golf70.trainer.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.WeeklyProgress
import com.golf70.trainer.ui.navigation.Dependencies

@Composable
fun ProgressScreen(
    stats: DashboardStats,
    vm: ProgressViewModel = viewModel(
        factory = ProgressViewModel.factory(Dependencies.repository(LocalContext.current))
    )
) {
    val state by vm.uiState.collectAsState()
    val latest = state.weeks.lastOrNull()
    val previous = if (state.weeks.size > 1) state.weeks[state.weeks.size - 2] else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineSmall)
            Text("Your golf game in motion", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            if (latest == null) {
                EmptyProgressCard()
            } else {
                KpiDeltaCard(latest = latest, previous = previous, liveStats = stats)
            }
        }

        item {
            Text("8-week trend", style = MaterialTheme.typography.titleMedium)
            if (state.loading) {
                Text("Loading trends…")
            }
        }

        if (!state.loading && state.weeks.isNotEmpty()) {
            items(state.weeks, key = { it.weekStartEpochMillis }) { week ->
                WeeklyTrendRow(week)
            }
        }

        item {
            if (!state.loading && state.weeks.size > 1) {
                Text("Week-over-week changes", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!state.loading && state.weeks.size > 1) {
            items(state.weeks.zipWithNext(), key = { "${it.first.weekStartEpochMillis}_${it.second.weekStartEpochMillis}" }) { pair ->
                WeekOverWeekRow(previous = pair.first, current = pair.second)
            }
        }

        item {
            latest?.let {
                Text("This week accomplished", style = MaterialTheme.typography.titleMedium)
                Text("Sessions: ${it.sessions} • Rounds: ${it.rounds} • Minutes: ${it.practiceMinutes}")
            }
        }
    }
}

@Composable
private fun EmptyProgressCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("No history yet", fontWeight = FontWeight.SemiBold)
            Text("Log a round or finish a session to unlock progress trends and weekly accomplishments.")
        }
    }
}

@Composable
private fun KpiDeltaCard(latest: WeeklyProgress, previous: WeeklyProgress?, liveStats: DashboardStats) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Latest week snapshot", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiItem("Fairway", "${latest.fairwayPercent.toInt()}%", delta(latest.fairwayPercent, previous?.fairwayPercent))
                KpiItem("GIR", "${latest.girPercent.toInt()}%", delta(latest.girPercent, previous?.girPercent))
                KpiItem("Putts", "${latest.puttsPerRound.toInt()}", delta(previous?.puttsPerRound, latest.puttsPerRound))
                KpiItem("Score", "${latest.scoringAverage.toInt()}", delta(previous?.scoringAverage, latest.scoringAverage))
            }
            Text(
                "Live dashboard: FW ${liveStats.fairwayPercent.toInt()}% • GIR ${liveStats.girPercent.toInt()}% • Putts ${liveStats.puttsPerRound.toInt()} • Score ${liveStats.scoringAverage.toInt()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun KpiItem(title: String, value: String, delta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.Bold)
        Text(delta, style = MaterialTheme.typography.labelSmall)
    }
}

private fun delta(current: Float?, previous: Float?): String {
    if (current == null || previous == null) return "new"
    val d = current - previous
    return if (d >= 0) "+${d.toInt()}" else d.toInt().toString()
}

@Composable
private fun WeeklyTrendRow(week: WeeklyProgress) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Week of ${week.label}", fontWeight = FontWeight.SemiBold)
            Text("Practice ${week.practiceMinutes} min • Sessions ${week.sessions} • Rounds ${week.rounds}")
            MetricBar("Fairway", week.fairwayPercent / 100f, MaterialTheme.colorScheme.primary)
            MetricBar("GIR", week.girPercent / 100f, MaterialTheme.colorScheme.secondary)
            MetricBar("Putts control", (40f - week.puttsPerRound).coerceAtLeast(0f) / 40f, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun WeekOverWeekRow(previous: WeeklyProgress, current: WeeklyProgress) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${previous.label} → ${current.label}",
                fontWeight = FontWeight.SemiBold
            )
            Text("Fairways: ${previous.fairwayPercent.toInt()}% → ${current.fairwayPercent.toInt()}% (${deltaLabel(current.fairwayPercent - previous.fairwayPercent)})")
            Text("GIR: ${previous.girPercent.toInt()}% → ${current.girPercent.toInt()}% (${deltaLabel(current.girPercent - previous.girPercent)})")
            Text("Putts/Round: ${previous.puttsPerRound.toInt()} → ${current.puttsPerRound.toInt()} (${deltaLabel(previous.puttsPerRound - current.puttsPerRound)})")
            Text("Score Avg: ${previous.scoringAverage.toInt()} → ${current.scoringAverage.toInt()} (${deltaLabel(previous.scoringAverage - current.scoringAverage)})")
        }
    }
}

private fun deltaLabel(value: Float): String {
    val rounded = value.toInt()
    return if (rounded >= 0) "+$rounded" else rounded.toString()
}

@Composable
private fun MetricBar(label: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}
