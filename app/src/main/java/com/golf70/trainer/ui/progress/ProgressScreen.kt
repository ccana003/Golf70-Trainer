package com.golf70.trainer.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.WeeklyProgress
import com.golf70.trainer.ui.navigation.Dependencies
import kotlin.math.abs

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
            Text("Skill trends", style = MaterialTheme.typography.titleMedium)
            if (state.loading) {
                Text("Loading trends…")
            }
        }

        if (!state.loading && state.weeks.isNotEmpty()) {
            item {
                SkillTrendCard(
                    title = "Driving accuracy",
                    subtitle = "Fairway hit % week over week",
                    values = state.weeks.map { it.fairwayPercent },
                    labels = state.weeks.map { it.label },
                    higherIsBetter = true,
                    lineColor = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SkillTrendCard(
                    title = "Putting",
                    subtitle = "Putts per round (lower is better)",
                    values = state.weeks.map { it.puttsPerRound },
                    labels = state.weeks.map { it.label },
                    higherIsBetter = false,
                    lineColor = MaterialTheme.colorScheme.tertiary
                )
            }
            item {
                SkillTrendCard(
                    title = "Ups & downs proxy",
                    subtitle = "GIR % trend (short-game pressure indicator)",
                    values = state.weeks.map { it.girPercent },
                    labels = state.weeks.map { it.label },
                    higherIsBetter = true,
                    lineColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            if (!state.loading && state.weeks.isNotEmpty()) {
                Text("8-week breakdown", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!state.loading && state.weeks.isNotEmpty()) {
            items(state.weeks, key = { it.weekStartEpochMillis }) { week ->
                WeeklyTrendRow(week)
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

@Composable
private fun SkillTrendCard(
    title: String,
    subtitle: String,
    values: List<Float>,
    labels: List<String>,
    higherIsBetter: Boolean,
    lineColor: Color
) {
    val firstValue = values.firstOrNull() ?: 0f
    val latestValue = values.lastOrNull() ?: 0f
    val delta = latestValue - firstValue
    val improving = if (higherIsBetter) delta >= 0f else delta <= 0f
    val trendLabel = when {
        values.size < 2 -> "Need more weeks"
        improving -> "Trending up"
        else -> "Dip detected"
    }

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            LineGraph(values = values, lineColor = lineColor)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${labels.firstOrNull().orEmpty()} → ${labels.lastOrNull().orEmpty()}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "$trendLabel (${trendDeltaLabel(delta, higherIsBetter)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (improving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LineGraph(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
) {
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f
    val range = (maxValue - minValue).takeIf { abs(it) > 0.01f } ?: 1f

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val stepX = if (values.size == 1) 0f else size.width / (values.size - 1)
        val graphHeight = size.height * 0.85f
        val yOffset = size.height * 0.08f

        drawLine(
            color = Color.LightGray,
            start = Offset(0f, size.height - yOffset),
            end = Offset(size.width, size.height - yOffset),
            strokeWidth = 2f
        )

        val path = Path()
        values.forEachIndexed { index, point ->
            val x = index * stepX
            val normalized = (point - minValue) / range
            val y = yOffset + (graphHeight - normalized * graphHeight)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f)
        )
    }
}

private fun trendDeltaLabel(delta: Float, higherIsBetter: Boolean): String {
    val amount = abs(delta).toInt()
    return if (delta == 0f) {
        "flat"
    } else if ((higherIsBetter && delta > 0) || (!higherIsBetter && delta < 0)) {
        "+$amount"
    } else {
        "-$amount"
    }
}
