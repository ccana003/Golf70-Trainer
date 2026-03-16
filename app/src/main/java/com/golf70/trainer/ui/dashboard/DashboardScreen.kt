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
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golf70.trainer.domain.DashboardStats

@Composable
fun DashboardScreen(stats: State<DashboardStats>) {
    val current = stats.value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Goal Dashboard", style = MaterialTheme.typography.headlineSmall)
        GoalRow("Fairway %", current.fairwayPercent / 100f, "${current.fairwayPercent.toInt()}%")
        GoalRow("GIR %", current.girPercent / 100f, "${current.girPercent.toInt()}%")
        GoalRow("Putts / Round", (40f - current.puttsPerRound).coerceAtLeast(0f) / 40f, "${current.puttsPerRound.toInt()}")
        GoalRow("Scoring Avg", (90f - current.scoringAverage).coerceAtLeast(0f) / 20f, "${current.scoringAverage}")
    }
}

@Composable
private fun GoalRow(title: String, progress: Float, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$title: $value")
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}
