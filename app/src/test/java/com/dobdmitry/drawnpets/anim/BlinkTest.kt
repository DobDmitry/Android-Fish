package com.dobdmitry.drawnpets.anim

import org.junit.Assert.*
import org.junit.Test

class BlinkTest {
    @Test fun eyelidsMoveContinuouslyAndReopen() {
        assertEquals(0f, blinkClosure(0f, 0), .001f)
        assertTrue(blinkClosure(.045f, 0) in .4f.. .6f)
        assertEquals(1f, blinkClosure(.1f, 0), .001f)
        assertTrue(blinkClosure(.195f, 0) in .4f.. .6f)
        assertEquals(0f, blinkClosure(.28f, 0), .001f)
        var previous = blinkClosure(0f, 0)
        for (i in 1..3300) {
            val current = blinkClosure(i / 1000f, 0)
            assertTrue(kotlin.math.abs(current - previous) < .03f)
            previous = current
        }
    }
    @Test fun extremeCreatureIdsCannotProduceInvalidClosure() {
        for (seed in listOf(Int.MIN_VALUE, -91, 0, 1, Int.MAX_VALUE)) {
            for (i in 0..600) assertTrue(blinkClosure(i*.017f, seed) in 0f..1f)
        }
    }
    @Test fun sleepNeverBlinksOrOpensEyes() {
        val animator = Animator(Rig.empty(200, 280))
        animator.action = Action.SLEEP
        repeat(500) {
            animator.t = it*.03f
            assertEquals(EyeState.SLEEP, animator.eyeState())
            assertEquals(1f, animator.eyelidClosure(), 0f)
        }
        animator.action = Action.IDLE
        animator.t = 1f
        assertEquals(0f, animator.eyelidClosure(), 0f)
    }
}
