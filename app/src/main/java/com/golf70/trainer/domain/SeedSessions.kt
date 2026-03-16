package com.golf70.trainer.domain

object SeedSessions {
    val weeklyPlan: List<SessionDefinition> = listOf(
        SessionDefinition(
            type = "Practice 1",
            durationMinutes = 60,
            drills = listOf(
                DrillDefinition("Warmup", "Loosen up with wedges and short putts.", 300, listOf(MetricType.ATTEMPTS)),
                DrillDefinition("Driver Drill", "Hit 15 drives and track left/center/right.", 600, listOf(MetricType.LEFT, MetricType.CENTER, MetricType.RIGHT)),
                DrillDefinition("Approach Ladder", "Hit targets from increasing distances.", 600, listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)),
                DrillDefinition("Putting Circle", "6ft circle drill around the cup.", 600, listOf(MetricType.ATTEMPTS, MetricType.MADE))
            )
        ),
        SessionDefinition(
            type = "Practice 2",
            durationMinutes = 45,
            drills = listOf(
                DrillDefinition("Short Game", "Chip and pitch to varied lies.", 900, listOf(MetricType.ATTEMPTS, MetricType.SUCCESS)),
                DrillDefinition("Pressure Putting", "10 made putts in a row challenge.", 600, listOf(MetricType.ATTEMPTS, MetricType.MADE))
            )
        )
    )
}
