package com.golf70.trainer.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PracticeSessionWithDrills(
    @Embedded val session: PracticeSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val drills: List<DrillEntity>
)

data class RoundWithHoles(
    @Embedded val round: RoundEntity,
    @Relation(parentColumn = "id", entityColumn = "round_id")
    val holes: List<HoleStatEntity>
)
