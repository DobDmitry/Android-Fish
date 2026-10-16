package com.dobdmitry.drawnpets.anim

import org.junit.Assert.*
import org.junit.Test

class FishTurnTest {
    @Test fun profileChangesAtTheSlowestPoint() {
        val turn = FishTurn()
        repeat(3) { turn.step(true, .05f) }
        assertFalse(turn.facingLeft)
        turn.step(true, .05f)
        assertTrue(turn.facingLeft)
        assertEquals(0f, turn.movement, .001f)
        repeat(5) { turn.step(true, .05f) }
        assertFalse(turn.active)
        assertEquals(1f, turn.movement, .001f)
    }

    @Test fun targetChangesDoNotFlipRepeatedlyDuringOneTurn() {
        val turn = FishTurn()
        turn.step(true, .05f)
        repeat(8) { turn.step(false, .05f) }
        assertTrue(turn.facingLeft)
        assertFalse(turn.active)
        repeat(3) { turn.step(false, .05f) }
        assertTrue(turn.facingLeft)
        assertFalse(turn.active)
        repeat(14) { turn.step(false, .05f) }
        assertFalse(turn.facingLeft)
    }
}
