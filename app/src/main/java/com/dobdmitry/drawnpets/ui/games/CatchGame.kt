package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.Canvas
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
import com.dobdmitry.drawnpets.data.WornItem
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private class Falling(var x: Float, var y: Float, val emoji: String, val points: Int, val speed: Float, val spin: Float)

private class CatchState {
    var px = 0.5f
    var target = 0.5f
    var spawn = 0f
    var elapsed = 0f
    var hurt = 0f
    var eatT = -1f
    var facingLeft = false
    var playerRect = Rect.Zero
    var size = Size.Zero
    val items = mutableListOf<Falling>()
    val particles = mutableListOf<Particle>()
}

private val goodFood = listOf("🍎", "🍌", "🥕", "🍪", "🧀", "🍓", "🍐", "🥛")
private const val CATCH_DURATION = 45f

@Composable
fun CatchGame(sprite: CreatureSprite?, outfit: List<WornItem>, onFinish: (Int) -> Unit) {
    val art = rememberStoryArtwork()
    val st = remember { CatchState() }
    var time by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var secondsLeft by remember { mutableIntStateOf(CATCH_DURATION.toInt()) }
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
                st.elapsed += dt
                secondsLeft = (CATCH_DURATION - st.elapsed).toInt().coerceAtLeast(0)
                // игрок плавно едет к пальцу
                val dx = st.target - st.px
                if (abs(dx) > 0.003f) st.facingLeft = dx < 0
                st.px += dx * (dt * 10f).coerceAtMost(1f)
                if (st.hurt > 0f) st.hurt -= dt
                if (st.eatT >= 0f) { st.eatT += dt; if (st.eatT > 0.5f) st.eatT = -1f }
                // появление предметов
                st.spawn -= dt
                val difficulty = 1f + st.elapsed / 25f
                if (st.spawn <= 0f) {
                    st.spawn = (0.85f / difficulty).coerceAtLeast(0.32f)
                    val r = Random.nextFloat()
                    val (emoji, pts) = when {
                        r < 0.07f -> "⭐" to 3
                        r < 0.15f -> "🍰" to 2
                        r < 0.33f -> listOf("🪨", "🐝").random() to -1
                        else -> goodFood.random() to 1
                    }
                    st.items += Falling(
                        x = sz.width * (0.08f + Random.nextFloat() * 0.84f), y = -40f, emoji = emoji, points = pts,
                        speed = sz.height * (0.22f + Random.nextFloat() * 0.12f) * difficulty, spin = (Random.nextFloat() - 0.5f) * 200f,
                    )
                }
                val pr = st.playerRect
                val it = st.items.iterator()
                while (it.hasNext()) {
                    val f = it.next()
                    f.y += f.speed * dt
                    val caught = f.y > pr.top + pr.height * 0.1f && f.y < pr.top + pr.height * 0.7f &&
                        abs(f.x - pr.center.x) < pr.width * 0.5f
                    if (caught) {
                        it.remove()
                        if (f.points > 0) {
                            score += f.points
                            st.particles += Particle(f.x, f.y, "+${f.points}", 0f, -120f, 46f, isLabel = true, color = Palette.Coral)
                            st.particles.burst(f.x, f.y, "✨", 2, 30f)
                            st.eatT = 0f
                            Sounds.play(if (f.points > 1) Sfx.COIN else Sfx.EAT, volume = 0.8f)
                        } else if (st.hurt <= 0f) {
                            lives -= 1
                            st.hurt = 1.2f
                            st.particles.burst(f.x, f.y, "💥", 3, 40f)
                            Sounds.play(Sfx.HURT)
                        }
                    } else if (f.y > sz.height + 60f) {
                        it.remove()
                    }
                }
                st.particles.step(dt)
                running = lives > 0 && st.elapsed < CATCH_DURATION
            }
        }
        finish(score)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            ev.changes.firstOrNull()?.let { ch ->
                                if (ch.pressed) st.target = (ch.position.x / size.width).coerceIn(0.08f, 0.92f)
                            }
                        }
                    }
                }
        ) {
            st.size = size
            val t = time
            drawStoryScene(art, 0)
            for (f in st.items) drawGameToken(art, f.emoji, Offset(f.x, f.y), size.width * 0.11f, rotation = t * f.spin)
            val h = size.width * 0.28f
            val blink = st.hurt > 0f && (t * 12f).toInt() % 2 == 0
            val act = when {
                st.hurt > 0.6f -> Action.SAD
                st.eatT >= 0f -> Action.EAT
                abs(st.target - st.px) > 0.01f -> Action.RUN
                else -> Action.IDLE
            }
            st.playerRect = drawCreature(
                sprite = sprite, feet = Offset(st.px * size.width, size.height * 0.92f), height = h, time = t,
                facingLeft = st.facingLeft, outfit = outfit,
                alpha = if (blink) 0.35f else 1f, action = act, actionTime = st.eatT.coerceAtLeast(0f),
            )
            // корзинка над головой — подсказка, куда ловить
            drawGameToken(art, "🧺", Offset(st.playerRect.center.x, st.playerRect.top + 6f + sin(t * 6f) * 2f), h * 0.32f)
            drawParticles(st.particles)
        }
        GameHud("⭐ $score   ${"❤️".repeat(lives.coerceAtLeast(0))}", "⏱ $secondsLeft")
    }
}
