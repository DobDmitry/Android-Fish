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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.WornItem
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.sin
import kotlin.random.Random

/** Пара колонн с просветом: [gapY] — центр просвета, [gap] — его высота (доли высоты экрана). */
private class Pillar(var x: Float, val gapY: Float, val gap: Float, val color: Color, var star: Boolean) {
    var passed = false
}

private class FlyState {
    var y = 0f
    var vy = 0f
    var started = false
    var dead = false
    var deadT = 0f
    var flapT = 1f
    var speed = 0f
    var nextPillar = 0f
    var distance = 0f
    var size = Size.Zero
    val pillars = mutableListOf<Pillar>()
    val particles = mutableListOf<Particle>()
}

private val pillarColors = listOf(Color(0xFFA8B88B), Color(0xFFDCA59A), Color(0xFFDEC388), Color(0xFF9CB9BF))
private const val GROUND = 0.9f

/**
 * «Полёт» — в духе Flappy Bird: нажатие подбрасывает питомца вверх,
 * нужно пролетать между колоннами и не задевать их и землю.
 */
@Composable
fun FlyGame(sprite: CreatureSprite?, outfit: List<WornItem>, best: Int, onFinish: (Int) -> Unit) {
    val art = rememberStoryArtwork()
    val finish by rememberUpdatedState(onFinish)
    val st = remember { FlyState() }
    var time by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var running = true
        while (running) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.04f)
                last = now
                time += dt
                st.particles.step(dt)
                val sz = st.size
                if (sz.width <= 0f) return@withFrameNanos
                if (st.y == 0f) st.y = sz.height * 0.45f
                st.flapT += dt
                if (!st.started) {
                    // до первого нажатия питомец парит на месте
                    st.y = sz.height * 0.45f + sin(time * 3f) * sz.height * 0.015f
                    return@withFrameNanos
                }
                val ph = sz.width * 0.16f
                val px = sz.width * 0.28f
                if (st.dead) {
                    st.deadT += dt
                    st.vy += sz.height * 2.6f * dt
                    st.y = (st.y + st.vy * dt).coerceAtMost(sz.height * GROUND - ph * 0.4f)
                    running = st.deadT < 1.2f
                    return@withFrameNanos
                }
                if (st.speed == 0f) st.speed = sz.width * 0.42f
                st.speed = (st.speed + sz.width * 0.008f * dt).coerceAtMost(sz.width * 0.75f)
                st.distance += st.speed * dt
                st.vy += sz.height * 2.6f * dt
                st.y += st.vy * dt
                val top = sz.height * 0.1f
                if (st.y < top) { st.y = top; st.vy = 0f }

                st.nextPillar -= dt
                if (st.nextPillar <= 0f) {
                    val difficulty = (score / 25f).coerceAtMost(1f)
                    val gap = 0.34f - 0.08f * difficulty
                    val gapY = 0.22f + gap / 2 + Random.nextFloat() * (GROUND - 0.3f - gap)
                    st.pillars += Pillar(sz.width + sz.width * 0.1f, gapY, gap, pillarColors.random(), Random.nextFloat() < 0.35f)
                    st.nextPillar = sz.width * 0.62f / st.speed
                }
                val pw = sz.width * 0.18f
                val player = Rect(px - ph * 0.3f, st.y - ph * 0.3f, px + ph * 0.3f, st.y + ph * 0.3f)
                var hit = st.y + ph * 0.35f >= sz.height * GROUND
                val it = st.pillars.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.x -= st.speed * dt
                    val gapTop = sz.height * (p.gapY - p.gap / 2)
                    val gapBottom = sz.height * (p.gapY + p.gap / 2)
                    val upper = Rect(p.x - pw / 2, -100f, p.x + pw / 2, gapTop)
                    val lower = Rect(p.x - pw / 2, gapBottom, p.x + pw / 2, sz.height)
                    if (player.overlaps(upper) || player.overlaps(lower)) hit = true
                    if (p.star && player.contains(Offset(p.x, sz.height * p.gapY))) {
                        p.star = false
                        score += 2
                        Sounds.play(Sfx.COIN, volume = 0.8f)
                        st.particles += Particle(p.x, sz.height * p.gapY, "+2", 0f, -120f, 44f, isLabel = true, color = Palette.Coral)
                    }
                    if (!p.passed && p.x + pw / 2 < px - ph * 0.3f) {
                        p.passed = true
                        score += 1
                        Sounds.play(Sfx.TAP, rate = 1.4f)
                    }
                    if (p.x < -pw) it.remove()
                }
                if (hit) {
                    st.dead = true
                    st.vy = -sz.height * 0.3f
                    Sounds.play(Sfx.HURT)
                    st.particles.burst(px, st.y, "💥", 4, 44f)
                }
            }
        }
        finish(score)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        if (!st.dead) {
                            if (!st.started) { st.started = true; started = true }
                            st.vy = -size.height * 0.75f
                            st.flapT = 0f
                            Sounds.play(Sfx.JUMP, volume = 0.6f, rate = 1.3f)
                            st.particles.burst(size.width * 0.24f, st.y + size.width * 0.05f, "💨", 1, 30f)
                        }
                    })
                }
        ) {
            st.size = size
            val t = time
            drawStoryScene(art, 4)
            val pw = size.width * 0.18f
            for (p in st.pillars) drawPillar(art, p, pw)
            val ph = size.width * 0.16f
            val px = size.width * 0.28f
            val angle = if (!st.started) 0f else (st.vy / size.height * 70f).coerceIn(-25f, 70f)
            rotate(angle, pivot = Offset(px, st.y)) {
                drawCreature(
                    sprite = sprite, feet = Offset(px, st.y + ph / 2), height = ph, time = t,
                    outfit = outfit, shadow = false,
                    action = when {
                        st.dead -> Action.SAD
                        st.flapT < 0.3f -> Action.JUMP
                        else -> Action.SWIM
                    },
                )
            }
            drawParticles(st.particles)
            if (!st.started) {
                drawLabel("Нажми, чтобы взлететь!", Offset(size.width / 2, size.height * 0.68f), 52f, Palette.Coral)
                drawEmoji("👆", Offset(size.width / 2, size.height * 0.76f + sin(t * 5f) * 10f), 70f)
            }
        }
        GameHud("🐦 $score", "🏆 ${maxOf(best, score)}")
    }
}

private fun DrawScope.drawPillar(art: StoryArtwork, p: Pillar, pw: Float) {
    val h = size.height
    val gapTop = h * (p.gapY - p.gap / 2)
    val gapBottom = h * (p.gapY + p.gap / 2)
    rotate(180f, Offset(p.x, gapTop / 2)) {
        drawStoryItem(art, 19, Offset(p.x, gapTop / 2), Size(pw * 1.2f, gapTop))
    }
    val lowerHeight = (h * GROUND - gapBottom).coerceAtLeast(1f)
    drawStoryItem(art, 19, Offset(p.x, gapBottom + lowerHeight / 2), Size(pw * 1.2f, lowerHeight))
    if (p.star) drawGameToken(art, "🌟", Offset(p.x, h * p.gapY), pw * .45f)
}
