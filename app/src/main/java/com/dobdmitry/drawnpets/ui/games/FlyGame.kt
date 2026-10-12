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

private val pillarColors = listOf(Color(0xFF5ED6A8), Color(0xFFFF8FB8), Color(0xFFFFC94D), Color(0xFF8FB8FF))
private const val GROUND = 0.9f

/**
 * «Полёт» — в духе Flappy Bird: нажатие подбрасывает питомца вверх,
 * нужно пролетать между колоннами и не задевать их и землю.
 */
@Composable
fun FlyGame(sprite: CreatureSprite?, outfit: List<WornItem>, best: Int, onFinish: (Int) -> Unit) {
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
            drawFlySky(st.distance)
            val pw = size.width * 0.18f
            for (p in st.pillars) drawPillar(p, pw)
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

private fun DrawScope.drawFlySky(distance: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF7EC8F0), Color(0xFFD6F1FF)), 0f, h * GROUND))
    // облака и холмы движутся медленнее колонн — объём
    for (i in 0 until 4) {
        val cx = w + 120f - ((distance * 0.15f + i * w * 0.45f) % (w + 240f))
        val cy = h * (0.18f + (i % 3) * 0.1f)
        drawCircle(Color.White, w * 0.06f, Offset(cx, cy))
        drawCircle(Color.White, w * 0.045f, Offset(cx + w * 0.06f, cy + w * 0.01f))
        drawCircle(Color.White, w * 0.04f, Offset(cx - w * 0.055f, cy + w * 0.012f))
    }
    for (i in 0 until 3) {
        val hx = w * 1.5f - ((distance * 0.35f + i * w * 0.8f) % (w * 2.4f))
        drawOval(Color(0xFF8ED46A), Offset(hx - w * 0.45f, h * (GROUND - 0.1f)), Size(w * 0.9f, h * 0.22f))
    }
    drawRect(Palette.Grass, Offset(0f, h * GROUND), Size(w, h * (1f - GROUND)))
    val stripe = w * 0.12f
    var x = -(distance % stripe)
    while (x < w) {
        drawRect(Palette.GrassDark, Offset(x, h * GROUND + h * 0.02f), Size(stripe * 0.45f, h * 0.012f))
        x += stripe
    }
}

private fun DrawScope.drawPillar(p: Pillar, pw: Float) {
    val h = size.height
    val gapTop = h * (p.gapY - p.gap / 2)
    val gapBottom = h * (p.gapY + p.gap / 2)
    val capH = h * 0.035f
    val ink = Palette.Ink
    val dark = p.color.copy(red = p.color.red * 0.8f, green = p.color.green * 0.8f, blue = p.color.blue * 0.8f)
    // верхняя колонна
    drawRect(p.color, Offset(p.x - pw / 2, 0f), Size(pw, gapTop - capH))
    drawRect(dark, Offset(p.x + pw * 0.2f, 0f), Size(pw * 0.25f, gapTop - capH))
    drawRect(ink, Offset(p.x - pw / 2, -4f), Size(pw, gapTop - capH + 4f), style = Stroke(4f))
    drawRoundRect(p.color, Offset(p.x - pw * 0.6f, gapTop - capH), Size(pw * 1.2f, capH), CornerRadius(10f))
    drawRoundRect(ink, Offset(p.x - pw * 0.6f, gapTop - capH), Size(pw * 1.2f, capH), CornerRadius(10f), style = Stroke(4f))
    // нижняя колонна
    val groundY = h * GROUND
    drawRect(p.color, Offset(p.x - pw / 2, gapBottom + capH), Size(pw, groundY - gapBottom - capH))
    drawRect(dark, Offset(p.x + pw * 0.2f, gapBottom + capH), Size(pw * 0.25f, groundY - gapBottom - capH))
    drawRect(ink, Offset(p.x - pw / 2, gapBottom + capH), Size(pw, groundY - gapBottom - capH), style = Stroke(4f))
    drawRoundRect(p.color, Offset(p.x - pw * 0.6f, gapBottom), Size(pw * 1.2f, capH), CornerRadius(10f))
    drawRoundRect(ink, Offset(p.x - pw * 0.6f, gapBottom), Size(pw * 1.2f, capH), CornerRadius(10f), style = Stroke(4f))
    // цветочек на крышке нижней колонны
    drawEmoji("🌼", Offset(p.x, gapBottom - pw * 0.12f), pw * 0.35f)
    if (p.star) drawEmoji("🌟", Offset(p.x, h * p.gapY), pw * 0.45f)
}
