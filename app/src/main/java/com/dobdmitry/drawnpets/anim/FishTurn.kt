package com.dobdmitry.drawnpets.anim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Brake, change profile once at rest, then resume; never collapse a fish into a line. */
internal class FishTurn {
    var facingLeft = false
        private set
    private var targetLeft = false
    private var elapsed = DURATION
    private var cooldown = 0f
    val active get() = elapsed < DURATION
    val progress get() = (elapsed / DURATION).coerceIn(0f, 1f)
    val movement get() = if (active) abs(cos(PI.toFloat() * progress)) else 1f
    val tilt get() = if (active) sin(PI.toFloat() * progress) * 6f else 0f

    fun step(wantLeft: Boolean, dt: Float) {
        val delta = dt.coerceIn(0f, .05f)
        cooldown = (cooldown - delta).coerceAtLeast(0f)
        if (!active && cooldown == 0f && wantLeft != facingLeft) {
            targetLeft = wantLeft
            elapsed = 0f
        }
        if (active) {
            elapsed = (elapsed + delta).coerceAtMost(DURATION)
            if (progress >= .5f) facingLeft = targetLeft
            if (!active) cooldown = .3f
        }
    }

    private companion object { const val DURATION = .4f }
}
