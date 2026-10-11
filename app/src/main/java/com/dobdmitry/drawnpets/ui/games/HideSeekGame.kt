package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private enum class HidePhase { INTRO, HIDING, SWAPPING, GUESS, FOUND, MISSED }

private val homes = listOf(
    Offset(0.2f, 0.45f), Offset(0.5f, 0.45f), Offset(0.8f, 0.45f),
    Offset(0.2f, 0.68f), Offset(0.5f, 0.68f), Offset(0.8f, 0.68f),
)
private val start = Offset(0.5f, 0.92f)

private class HideState {
    val pos = homes.toTypedArray()
    var phase = HidePhase.INTRO
    var timer = 0f
    var hideBush = Random.nextInt(6)
    var swapsLeft = 0
    var swapA = 0
    var swapB = 1
    var swapT = 0f
    var fromA = Offset.Zero
    var fromB = Offset.Zero
    var wrongBush = -1
    var round = 1
    var misses = 0
    var size = Size.Zero
    val particles = mutableListOf<Particle>()

    val swapDuration: Float get() = (0.75f - round * 0.04f).coerceAtLeast(0.32f)

    fun nextRound() {
        round += 1
        hideBush = Random.nextInt(6)
        swapsLeft = (round - 2).coerceIn(0, 8)
        wrongBush = -1
        phase = HidePhase.INTRO
        timer = 0f
    }

    fun startSwap() {
        swapA = Random.nextInt(6)
        do { swapB = Random.nextInt(6) } while (swapB == swapA)
        fromA = pos[swapA]
        fromB = pos[swapB]
        swapT = 0f
        Sounds.play(Sfx.FLIP)
    }
}

@Composable
fun HideSeekGame(sprite: CreatureSprite?, accessory: String?, onFinish: (Int) -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val st = remember { HideState() }
    var time by remember { mutableFloatStateOf(0f) }
    var found by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var running = true
        while (running) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                st.timer += dt
                st.particles.step(dt)
                when (st.phase) {
                    HidePhase.INTRO -> if (st.timer > 1.1f) { st.phase = HidePhase.HIDING; st.timer = 0f }
                    HidePhase.HIDING -> if (st.timer > 1.0f) {
                        st.timer = 0f
                        if (st.swapsLeft > 0) { st.phase = HidePhase.SWAPPING; st.startSwap() } else st.phase = HidePhase.GUESS
                    }
                    HidePhase.SWAPPING -> {
                        st.swapT += dt / st.swapDuration
                        val k = st.swapT.coerceAtMost(1f)
                        val e = k * k * (3 - 2 * k)
                        val arc = sin(PI.toFloat() * e) * 0.08f
                        st.pos[st.swapA] = Offset(st.fromA.x + (st.fromB.x - st.fromA.x) * e, st.fromA.y + (st.fromB.y - st.fromA.y) * e - arc)
                        st.pos[st.swapB] = Offset(st.fromB.x + (st.fromA.x - st.fromB.x) * e, st.fromB.y + (st.fromA.y - st.fromB.y) * e + arc)
                        if (st.swapT >= 1f) {
                            st.pos[st.swapA] = st.fromB
                            st.pos[st.swapB] = st.fromA
                            st.swapsLeft -= 1
                            if (st.swapsLeft > 0) st.startSwap() else { st.phase = HidePhase.GUESS; st.timer = 0f }
                        }
                    }
                    HidePhase.GUESS -> Unit
                    HidePhase.FOUND, HidePhase.MISSED -> if (st.timer > 1.8f) {
                        if (st.misses >= 3) running = false else st.nextRound()
                    }
                }
                time += dt
            }
        }
        finish(found)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        if (st.phase != HidePhase.GUESS) return@detectTapGestures
                        val r = size.width * 0.14f
                        val tapped = (0 until 6).firstOrNull { i ->
                            val c = st.pos[i]
                            hypot(p.x - c.x * size.width, p.y - (c.y * size.height - r * 0.5f)) < r * 1.15f
                        } ?: return@detectTapGestures
                        st.timer = 0f
                        if (tapped == st.hideBush) {
                            st.phase = HidePhase.FOUND
                            found += 1
                            Sounds.play(Sfx.HAPPY)
                            st.particles.burst(p.x, p.y - r, "💖", 6, 44f)
                        } else {
                            st.phase = HidePhase.MISSED
                            st.wrongBush = tapped
                            st.misses += 1
                            misses = st.misses
                            Sounds.play(Sfx.WRONG)
                            st.particles.burst(p.x, p.y - r, "🍂", 4, 36f)
                        }
                    }
                }
        ) {
            st.size = size
            val t = time
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(Color(0xFF9ED8FF), Color(0xFFDFF5C8)), 0f, h * 0.3f))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF8ED46A), Palette.GrassDark), h * 0.3f, h), Offset(0f, h * 0.3f), Size(w, h * 0.7f))
            val r = w * 0.14f
            val ch = w * 0.24f
            val hidden = st.hideBush

            // Бег к кусту — рисуем до кустов, чтобы питомец «заходил» за куст
            val target = st.pos[hidden]
            if (st.phase == HidePhase.HIDING) {
                val k = (st.timer / 1f).coerceIn(0f, 1f)
                val x = start.x + (target.x - start.x) * k
                val y = start.y + (target.y - start.y) * k
                drawCreature(
                    sprite, Offset(x * w, y * h), ch * (1f - 0.2f * k), t, moving = true,
                    facingLeft = target.x < start.x, accessory = accessory, alpha = 1f - ((k - 0.75f) * 4f).coerceIn(0f, 1f),
                )
            }

            val order = (0 until 6).sortedBy { st.pos[it].y }
            for (i in order) {
                val c = Offset(st.pos[i].x * w, st.pos[i].y * h)
                if (i == hidden && (st.phase == HidePhase.FOUND || st.phase == HidePhase.MISSED)) {
                    val k = (st.timer / 0.4f).coerceIn(0f, 1f)
                    drawCreature(sprite, Offset(c.x, c.y - r * 0.2f), ch, t, jump = ch * 0.55f * k, accessory = accessory, shadow = false, action = if (st.phase == HidePhase.FOUND) Action.HAPPY else Action.WAVE, actionTime = st.timer)
                }
                val rustle = st.phase == HidePhase.GUESS && i == hidden && st.round <= 2 && (t % 2.2f) < 0.35f
                drawBush(c, r, if (rustle) sin(t * 40f) * 5f else 0f)
                if (i == st.wrongBush && st.phase == HidePhase.MISSED) {
                    drawLabel("Пусто!", Offset(c.x, c.y - r * 1.6f), 42f, Color(0xFF8A5A3B))
                }
            }
            if (st.phase == HidePhase.INTRO) {
                drawCreature(sprite, Offset(start.x * w, start.y * h), ch, t, accessory = accessory, action = Action.WAVE, actionTime = st.timer)
                drawLabel("Ищи меня!", Offset(start.x * w, start.y * h - ch - 20f), 46f, Palette.Coral)
            }
            if (st.phase == HidePhase.GUESS) {
                drawLabel("Где я? Нажми на куст!", Offset(w / 2, h * 0.88f), 46f, Palette.Ink)
            }
            if (st.phase == HidePhase.FOUND) {
                drawLabel("Нашёл!", Offset(w / 2, h * 0.88f), 54f, Palette.Coral)
            }
            drawParticles(st.particles)
        }
        GameHud("🔍 Найдено: $found", "❌ ${"●".repeat(misses)}${"○".repeat((3 - misses).coerceAtLeast(0))}")
    }
}

private fun DrawScope.drawBush(c: Offset, r: Float, shake: Float) {
    val ink = Palette.Ink
    val parts = listOf(
        Offset(-0.55f, -0.35f) to 0.55f,
        Offset(0.55f, -0.35f) to 0.55f,
        Offset(0f, -0.75f) to 0.65f,
        Offset(-0.25f, -0.25f) to 0.6f,
        Offset(0.3f, -0.2f) to 0.55f,
    )
    for ((o, k) in parts) {
        val p = Offset(c.x + o.x * r + shake, c.y + o.y * r)
        drawCircle(ink, r * k + 3f, p)
    }
    for ((o, k) in parts) {
        val p = Offset(c.x + o.x * r + shake, c.y + o.y * r)
        drawCircle(Color(0xFF3FA34D), r * k, p)
    }
    drawCircle(Color(0xFF5CC46A), r * 0.3f, Offset(c.x - r * 0.25f + shake, c.y - r * 0.85f))
    for (i in 0 until 3) {
        drawCircle(Color(0xFFFF5C7A), r * 0.07f, Offset(c.x + (i - 1) * r * 0.45f + shake, c.y - r * (0.35f + (i % 2) * 0.3f)))
    }
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - r, c.y - r * 0.1f), Size(r * 2, r * 0.3f))
}
