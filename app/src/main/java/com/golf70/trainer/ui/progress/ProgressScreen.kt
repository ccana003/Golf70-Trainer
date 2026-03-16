package com.golf70.trainer.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.util.AdaptiveCoaching

@Composable
fun ProgressScreen(stats: DashboardStats = DashboardStats(45f, 48f, 36f, 84f)) {
    val recommendations = AdaptiveCoaching.recommendations(stats)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineSmall)
        Text("Historical chart integration uses MPAndroidChart in AndroidView wrappers.")
        Text("Adaptive coaching")
        recommendations.forEach { Text("• $it") }
    }
}
