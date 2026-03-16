package com.golf70.trainer

import com.golf70.trainer.timer.DrillTimerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrillTimerEngineTest {
    @Test
    fun timerTicksAndStopsAtZero() {
        val engine = DrillTimerEngine(0)
        var state = engine.start(2)
        assertTrue(state.running)

        state = engine.tick()
        assertEquals(1, state.remaining)

        state = engine.tick()
        assertEquals(0, state.remaining)
        assertFalse(state.running)
    }
}
