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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.random.Random

private class Obstacle(var x: Float, val emoji: String, var passed: Boolean = false)
private class Coin(var x: Float, val y: Float)

private class JumpState {
    var lift = 0f
    var vy = 0f
    var jumpsLeft = 2
    var speed = 0f
    var distance = 0f
    var nextObstacle = 1.2f
    var nextCoin = 2f
    var hurt = 0f
    var size = Size.Zero
    val obstacles = mutableListOf<Obstacle>()
    val coins = mutableListOf<Coin>()
    val particles = mutableListOf<Particle>()
}

@Composable
fun JumpGame(sprite: CreatureSprite?, accessory: String?, onFinish: (Int) -> Unit) {
    val st = remember { JumpState() }
    var time by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(5) }
    val finish by rememberUpdatedState(onFinish)

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var running = true
        while (running) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                time += dt
                val sz = st.size
                if (sz.width <= 0f) return@withFrameNanos
                if (st.speed == 0f) st.speed = sz.width * 0.36f
                st.speed += sz.width * 0.007f * dt
                st.distance += st.speed * dt
                // физика прыжка
                val gravity = sz.height * 3.2f
                st.vy -= gravity * dt
                st.lift += st.vy * dt
                if (st.lift <= 0f) { st.lift = 0f; st.vy = 0f; st.jumpsLeft = 2 }
                if (st.hurt > 0f) st.hurt -= dt

                st.nextObstacle -= dt
                if (st.nextObstacle <= 0f) {
                    st.obstacles += Obstacle(sz.width + 60f, listOf("🌵", "🪨", "🍄", "🌵").random())
                    st.nextObstacle = (1.3f + Random.nextFloat() * 1.2f) * (sz.width * 0.45f / st.speed).coerceAtLeast(0.7f)
                }
                st.nextCoin -= dt
                if (st.nextCoin <= 0f) {
                    st.coins += Coin(sz.width + 60f, sz.height * (0.42f + Random.nextFloat() * 0.18f))
                    st.nextCoin = 1.5f + Random.nextFloat() * 2f
                }
                val ground = sz.height * 0.78f
                val ph = sz.width * 0.22f
                val px = sz.width * 0.22f
                val player = Rect(px - ph * 0.3f, ground - st.lift - ph * 0.85f, px + ph * 0.3f, ground - st.lift)
                val os = sz.width * 0.12f
                val oi = st.obstacles.iterator()
                while (oi.hasNext()) {
                    val o = oi.next()
                    o.x -= st.speed * dt
                    val rect = Rect(o.x - os * 0.32f, ground - os * 0.8f, o.x + os * 0.32f, ground)
                    if (st.hurt <= 0f && rect.overlaps(player)) {
                        lives -= 1
                        st.hurt = 1.3f
                        st.particles.burst(px, player.center.y, "💥", 3, 40f)
                        Sounds.play(Sfx.HURT)
                    }
                    if (!o.passed && o.x < px - os) {
                        o.passed = true
                        score += 1
                    }
                    if (o.x < -80f) oi.remove()
                }
                val ci = st.coins.iterator()
                while (ci.hasNext()) {
                    val c = ci.next()
                    c.x -= st.speed * dt
                    if (player.inflate(10f).contains(Offset(c.x, c.y))) {
                        ci.remove()
                        score += 2
                        Sounds.play(Sfx.COIN, volume = 0.8f)
                        st.particles += Particle(c.x, c.y, "+2", 0f, -120f, 44f, isLabel = true, color = Palette.Coral)
                    } else if (c.x < -60f) ci.remove()
                }
                st.particles.step(dt)
                running = lives > 0
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
                        if (st.jumpsLeft > 0) {
                            st.vy = size.height * (if (st.jumpsLeft == 2) 1.25f else 1.05f)
                            Sounds.play(Sfx.JUMP, rate = if (st.jumpsLeft == 2) 1f else 1.25f)
                            st.jumpsLeft -= 1
                            st.particles.burst(size.width * 0.22f, size.height * 0.78f - st.lift, "💨", 1, 30f)
                        }
                    })
                }
        ) {
            st.size = size
            val t = time
            val w = size.width
            val h = size.height
            val ground = h * 0.78f
            drawRect(Brush.verticalGradient(listOf(Color(0xFF8FD3FF), Color(0xFFFFF1D6)), 0f, ground))
            // параллакс: облака и холмы
            for (i in 0 until 3) {
                val cx = w + 100f - ((st.distance * 0.1f + i * w * 0.5f) % (w + 200f))
                drawCircle(Color.White, w * 0.06f, Offset(cx, h * (0.2f + i * 0.07f)))
                drawCircle(Color.White, w * 0.045f, Offset(cx + w * 0.06f, h * (0.21f + i * 0.07f)))
            }
            for (i in 0 until 3) {
                val hx = w * 1.4f - ((st.distance * 0.3f + i * w * 0.7f) % (w * 2.1f))
                drawOval(Color(0xFF8ED46A), Offset(hx - w * 0.4f, ground - h * 0.12f), Size(w * 0.8f, h * 0.25f))
            }
            drawRect(Palette.Grass, Offset(0f, ground), Size(w, h - ground))
            // полоски земли
            val stripe = w * 0.15f
            val shift = st.distance % stripe
            var x = -shift
            while (x < w) {
                drawRect(Palette.GrassDark, Offset(x, ground + h * 0.03f), Size(stripe * 0.4f, h * 0.012f))
                x += stripe
            }
            for (c in st.coins) drawEmoji("🪙", Offset(c.x, c.y), w * 0.08f)
            for (o in st.obstacles) drawEmoji(o.emoji, Offset(o.x, ground - w * 0.06f), w * 0.12f)
            val blink = st.hurt > 0f && (t * 12f).toInt() % 2 == 0
            drawCreature(
                sprite = sprite, feet = Offset(w * 0.22f, ground), height = w * 0.22f, time = t,
                jump = st.lift, accessory = accessory, alpha = if (blink) 0.35f else 1f,
                action = when {
                    st.hurt > 0.8f -> Action.SAD
                    st.lift > 0f -> Action.JUMP
                    else -> Action.RUN
                },
            )
            drawParticles(st.particles)
        }
        GameHud("⭐ $score", "❤️".repeat(lives.coerceAtLeast(0)).ifEmpty { "💔" })
    }
}
