package com.dobdmitry.drawnpets.anim

/** Short, smooth eyelid movement with a slower opening than closing. */
internal fun blinkClosure(time: Float, seed: Int): Float {
    val key = seed.toLong().let { if (it < 0) -it else it }
    val period = 3.3f + (key % 7) * .25f
    val phase = ((time + (key % 5)) % period + period) % period
    val linear = when {
        phase < .09f -> phase / .09f
        phase < .12f -> 1f
        phase < .27f -> 1f - (phase - .12f) / .15f
        else -> 0f
    }.coerceIn(0f, 1f)
    return linear * linear * (3f - 2f * linear)
}
