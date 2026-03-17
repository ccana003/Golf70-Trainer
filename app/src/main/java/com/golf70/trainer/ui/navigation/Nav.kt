package com.golf70.trainer.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.golf70.trainer.R
import com.golf70.trainer.ui.dashboard.DashboardScreen
import com.golf70.trainer.ui.progress.ProgressScreen
import com.golf70.trainer.ui.round.RoundTrackerScreen
import com.golf70.trainer.ui.session.PracticeSessionScreen

enum class Golf70Destination(val route: String, val label: String) {
    Dashboard("dashboard", "Dashboard"),
    Session("session", "Start Session"),
    Round("round", "Log Round"),
    Progress("progress", "Progress")
}

@Composable
fun Golf70NavHost(vm: MainViewModel) {
    val navController = rememberNavController()
    val dashboard by vm.dashboardStats.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val rounds by vm.rounds.collectAsState()
    val weeklyPlanState by vm.weeklyPlanState.collectAsState()
    val items = Golf70Destination.entries
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Image(
                            painter = painterResource(id = R.drawable.ic_golf70_logo),
                            contentDescription = "Golf70 logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("  Golf70", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val destination = backStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = { navController.navigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    Golf70Destination.Dashboard -> Icons.Default.Home
                                    Golf70Destination.Session -> Icons.Default.PlayArrow
                                    Golf70Destination.Round -> Icons.Default.Add
                                    Golf70Destination.Progress -> Icons.Default.DateRange
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Golf70Destination.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Golf70Destination.Dashboard.route) {
                DashboardScreen(
                    stats = dashboard,
                    sessions = sessions,
                    rounds = rounds,
                    weeklyPlan = weeklyPlanState.plan,
                    onWeekBack = { vm.changeWeek(-1) },
                    onWeekForward = { vm.changeWeek(1) },
                    onDeleteSession = vm::deleteSession,
                    onDeleteRound = vm::deleteRound
                )
            }
            composable(Golf70Destination.Session.route) {
                PracticeSessionScreen()
            }
            composable(Golf70Destination.Round.route) {
                RoundTrackerScreen(onRoundSaved = { navController.navigate(Golf70Destination.Dashboard.route) })
            }
            composable(Golf70Destination.Progress.route) {
                ProgressScreen(stats = dashboard)
            }
        }
    }
}
