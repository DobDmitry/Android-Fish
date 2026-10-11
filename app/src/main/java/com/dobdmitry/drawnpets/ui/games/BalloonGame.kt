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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class BalloonKind { NORMAL, FRIEND, GOLD, STORM }

private class Balloon(
    var x: Float,
    var y: Float,
    val speed: Float,
    val color: Color,
    val kind: BalloonKind,
    val phase: Float,
    val friend: ImageBitmap?,
)

private val balloonColors = listOf(
    Color(0xFFFF5C7A), Color(0xFF5CC8FF), Color(0xFF7BE07A), Color(0xFFB59CFF), Color(0xFFFFA94D), Color(0xFFFF8FB8),
)

private const val BALLOON_TIME = 40f

@Composable
fun BalloonGame(vm: GameViewModel, onFinish: (Int) -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val friends = remember { vm.state.creatures.mapNotNull { vm.bitmap(it.id) } }
    val balloons = remember { mutableListOf<Balloon>() }
    val particles = remember { mutableListOf<Particle>() }
    val sizeRef = remember { arrayOf(Size.Zero) }
    var time by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(BALLOON_TIME.toInt()) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var elapsed = 0f
        var spawn = 0f
        while (elapsed < BALLOON_TIME) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                time += dt
                val sz = sizeRef[0]
                if (sz.width <= 0f) return@withFrameNanos
                elapsed += dt
                secondsLeft = (BALLOON_TIME - elapsed).toInt().coerceAtLeast(0)
                spawn -= dt
                if (spawn <= 0f) {
                    spawn = (0.6f - elapsed / BALLOON_TIME * 0.3f)
                    val r = Random.nextFloat()
                    val kind = when {
                        r < 0.08f -> BalloonKind.GOLD
                        r < 0.2f -> BalloonKind.STORM
                        r < 0.38f && friends.isNotEmpty() -> BalloonKind.FRIEND
                        else -> BalloonKind.NORMAL
                    }
                    balloons += Balloon(
                        x = sz.width * (0.1f + Random.nextFloat() * 0.8f),
                        y = sz.height + 80f,
                        speed = sz.height * (0.13f + Random.nextFloat() * 0.1f) * (1f + elapsed / 40f),
                        color = balloonColors.random(),
                        kind = kind,
                        phase = Random.nextFloat() * 6f,
                        friend = if (kind == BalloonKind.FRIEND) friends.random() else null,
                    )
                }
                val it = balloons.iterator()
                while (it.hasNext()) {
                    val b = it.next()
                    b.y -= b.speed * dt
                    if (b.y < -150f) it.remove()
                }
                particles.step(dt)
            }
        }
        finish(score)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { pos ->
                        val r = size.width * 0.09f
                        val hit = balloons.lastOrNull { b ->
                            val bx = b.x + sin(time * 2f + b.phase) * 12f
                            hypot(pos.x - bx, pos.y - b.y) < r * 1.3f
                        }
                        if (hit != null) {
                            balloons.remove(hit)
                            val pts = when (hit.kind) {
                                BalloonKind.NORMAL -> 1
                                BalloonKind.FRIEND -> 3
                                BalloonKind.GOLD -> 5
                                BalloonKind.STORM -> -3
                            }
                            score = (score + pts).coerceAtLeast(0)
                            particles.burst(pos.x, pos.y, if (pts > 0) "✨" else "⚡", 4, 36f)
                            particles += Particle(
                                pos.x, pos.y, if (pts > 0) "+$pts" else "$pts", 0f, -130f, 48f,
                                isLabel = true, color = if (pts > 0) Palette.Coral else Color(0xFF5A5A8A),
                            )
                        }
                    })
                }
        ) {
            sizeRef[0] = size
            val t = time
            drawRect(Brush.verticalGradient(listOf(Color(0xFF7EC8F0), Color(0xFFFFE0EC))))
            for (b in balloons) {
                val bx = b.x + sin(t * 2f + b.phase) * 12f
                drawBalloon(Offset(bx, b.y), size.width * 0.09f, b, t)
            }
            drawParticles(particles)
        }
        GameHud("🎈 $score", "⏱ $secondsLeft")
    }
}

private fun DrawScope.drawBalloon(c: Offset, r: Float, b: Balloon, t: Float) {
    if (b.kind == BalloonKind.STORM) {
        drawEmoji("⛈️", c, r * 2.2f)
        return
    }
    val color = if (b.kind == BalloonKind.GOLD) Color(0xFFFFD84D) else b.color
    // ниточка
    val string = Path().apply {
        moveTo(c.x, c.y + r * 1.2f)
        quadraticBezierTo(c.x + sin(t * 3f + b.phase) * 10f, c.y + r * 1.8f, c.x, c.y + r * 2.5f)
    }
    drawPath(string, Palette.Ink.copy(alpha = 0.7f), style = Stroke(2.5f))
    val knot = Path().apply {
        moveTo(c.x, c.y + r * 1.15f); lineTo(c.x - r * 0.12f, c.y + r * 1.3f); lineTo(c.x + r * 0.12f, c.y + r * 1.3f); close()
    }
    drawPath(knot, color)
    drawOval(color, Offset(c.x - r, c.y - r * 1.2f), Size(r * 2, r * 2.4f))
    drawOval(Palette.Ink.copy(alpha = 0.7f), Offset(c.x - r, c.y - r * 1.2f), Size(r * 2, r * 2.4f), style = Stroke(3f))
    drawOval(Color.White.copy(alpha = 0.5f), Offset(c.x - r * 0.6f, c.y - r * 0.9f), Size(r * 0.45f, r * 0.7f))
    val img = b.friend
    if (img != null) {
        val s = r * 1.4f
        val aspect = img.width.toFloat() / img.height
        val w = if (aspect > 1f) s else s * aspect
        val h = if (aspect > 1f) s / aspect else s
        drawImage(
            img, IntOffset.Zero, IntSize(img.width, img.height),
            IntOffset((c.x - w / 2).roundToInt(), (c.y - h / 2).roundToInt()),
            IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
        )
    }
    if (b.kind == BalloonKind.GOLD) drawEmoji("⭐", c, r)
}
