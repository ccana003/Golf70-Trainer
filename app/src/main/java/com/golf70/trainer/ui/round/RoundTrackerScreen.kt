package com.golf70.trainer.ui.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.ui.navigation.Dependencies

@Composable
fun RoundTrackerScreen(vm: RoundTrackerViewModel = viewModel(factory = RoundTrackerViewModel.factory(Dependencies.repository(LocalContext.current)))) {
    var course by remember { mutableStateOf("Home Course") }
    val holes by vm.holes.collectAsState()
    val summary = vm.summary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Round Tracker", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course") })

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(holes) { hole ->
                HoleRow(hole = hole, onChange = vm::updateHole)
            }
        }

        Text("Total Score: ${summary.totalScore}")
        Text("Fairway %: ${summary.fairwayPercentage.toInt()}  GIR %: ${summary.girPercentage.toInt()}")
        Text("Putts / Round: ${summary.puttsPerRound}")
        Button(onClick = { vm.saveRound(course) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Round")
        }
    }
}

@Composable
private fun HoleRow(hole: HoleInput, onChange: (HoleInput) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("H${hole.holeNumber}", modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(
            value = hole.score.toString(),
            onValueChange = { onChange(hole.copy(score = it.toIntOrNull() ?: hole.score)) },
            label = { Text("Score") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = hole.putts.toString(),
            onValueChange = { onChange(hole.copy(putts = it.toIntOrNull() ?: hole.putts)) },
            label = { Text("Putts") },
            modifier = Modifier.weight(1f)
        )
        Column {
            Text("FW")
            Checkbox(checked = hole.fairwayHit, onCheckedChange = { onChange(hole.copy(fairwayHit = it)) })
        }
        Column {
            Text("GIR")
            Checkbox(checked = hole.gir, onCheckedChange = { onChange(hole.copy(gir = it)) })
        }
    }
}
