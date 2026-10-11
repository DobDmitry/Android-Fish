package com.dobdmitry.drawnpets.ui.common

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dobdmitry.drawnpets.data.Catalog
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** Время в секундах с момента появления composable, обновляется каждый кадр. */
@Composable
fun rememberFrameTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return time
}

private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

/** Рисует эмодзи (или любой текст) с центром в точке [center]. */
fun DrawScope.drawEmoji(text: String, center: Offset, size: Float, alpha: Float = 1f, rotation: Float = 0f) {
    emojiPaint.textSize = size
    emojiPaint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.rotate(rotation, center.x, center.y)
    canvas.drawText(text, center.x, center.y + size * 0.35f, emojiPaint)
    canvas.restore()
}

/** Подпись с белой обводкой — хорошо читается на любом фоне. */
fun DrawScope.drawLabel(text: String, center: Offset, size: Float, color: Color = Palette.Ink, alpha: Float = 1f) {
    val canvas = drawContext.canvas.nativeCanvas
    labelPaint.textSize = size
    labelPaint.style = Paint.Style.STROKE
    labelPaint.strokeWidth = size * 0.22f
    labelPaint.color = Color.White.toArgb()
    labelPaint.alpha = (alpha * 255).toInt()
    canvas.drawText(text, center.x, center.y, labelPaint)
    labelPaint.style = Paint.Style.FILL
    labelPaint.color = color.toArgb()
    labelPaint.alpha = (alpha * 255).toInt()
    canvas.drawText(text, center.x, center.y, labelPaint)
}

private val sadFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })

/**
 * Рисует оживший рисунок. [feet] — точка «ног» (низ по центру).
 * Возвращает прямоугольник, который занял персонаж (для нажатий).
 */
fun DrawScope.drawCreature(
    image: ImageBitmap?,
    feet: Offset,
    height: Float,
    time: Float,
    seed: Int = 0,
    facingLeft: Boolean = false,
    moving: Boolean = false,
    sleeping: Boolean = false,
    sad: Boolean = false,
    accessory: String? = null,
    jump: Float = 0f,
    swimming: Boolean = false,
    shadow: Boolean = true,
    alpha: Float = 1f,
): Rect {
    val phase = seed * 0.37f
    val aspect = if (image != null) image.width.toFloat() / image.height else 1f
    val w = height * aspect

    var sx = 1f
    var sy = 1f
    var rot = 0f
    var lift = jump
    when {
        sleeping -> {
            val br = sin(time * 1.6f + phase)
            sy = 0.94f + 0.03f * br
            sx = 1.03f - 0.02f * br
        }
        swimming -> {
            rot = sin(time * 2.2f + phase) * 6f
            sy = 1f + 0.03f * sin(time * 4f + phase)
        }
        moving -> {
            val hop = abs(sin(time * 7f + phase))
            lift += hop * height * 0.10f
            sy = 0.94f + 0.10f * hop
            sx = 1.05f - 0.07f * hop
            rot = sin(time * 7f + phase) * 5f
        }
        else -> {
            val br = sin(time * 2.4f + phase)
            sy = 1f + 0.035f * br
            sx = 1f - 0.025f * br
            rot = sin(time * 1.1f + phase) * 2f
        }
    }

    if (shadow && !swimming) {
        val shadowScale = (1f - lift / (height * 1.2f)).coerceIn(0.4f, 1f)
        drawOval(
            color = Color.Black.copy(alpha = 0.16f * alpha * shadowScale),
            topLeft = Offset(feet.x - w * 0.4f * shadowScale, feet.y - height * 0.05f),
            size = Size(w * 0.8f * shadowScale, height * 0.1f),
        )
    }

    val base = Offset(feet.x, feet.y - lift)
    withTransform({
        rotate(rot, pivot = base)
        scale(if (facingLeft) -sx else sx, sy, pivot = base)
    }) {
        if (image != null) {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset((base.x - w / 2).roundToInt(), (base.y - height).roundToInt()),
                dstSize = IntSize(w.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
                alpha = alpha,
                colorFilter = if (sad || sleeping) sadFilter else null,
                filterQuality = FilterQuality.Medium,
            )
        } else {
            drawEmoji("🙂", Offset(base.x, base.y - height / 2), height * 0.8f, alpha)
        }
    }
    Catalog.accessory(accessory)?.let { acc ->
        val headY = base.y - height * sy
        drawEmoji(acc.emoji, Offset(base.x, headY - height * 0.02f), (w * 0.42f).coerceAtMost(height * 0.5f), alpha, rot)
    }
    if (sleeping) {
        val k = (time * 0.7f + phase) % 1f
        drawEmoji("💤", Offset(base.x + w * 0.35f + k * 20f, base.y - height - k * 30f), height * 0.28f, 1f - k)
    }
    return Rect(feet.x - w / 2, feet.y - height - lift, feet.x + w / 2, feet.y)
}

/** Частица, которая улетает вверх и тает: сердечки, звёзды, «+1». */
class Particle(
    var x: Float,
    var y: Float,
    val text: String,
    val vx: Float,
    val vy: Float,
    val size: Float,
    var life: Float = 1f,
    val decay: Float = 0.8f,
    val isLabel: Boolean = false,
    val color: Color = Palette.Ink,
)

fun MutableList<Particle>.step(dt: Float) {
    val it = iterator()
    while (it.hasNext()) {
        val p = it.next()
        p.x += p.vx * dt
        p.y += p.vy * dt
        p.life -= p.decay * dt
        if (p.life <= 0f) it.remove()
    }
}

fun DrawScope.drawParticles(list: List<Particle>) {
    for (p in list) {
        if (p.isLabel) drawLabel(p.text, Offset(p.x, p.y), p.size, p.color, p.life.coerceIn(0f, 1f))
        else drawEmoji(p.text, Offset(p.x, p.y), p.size, p.life.coerceIn(0f, 1f))
    }
}

fun MutableList<Particle>.burst(x: Float, y: Float, emoji: String, count: Int, size: Float) {
    repeat(count) {
        add(
            Particle(
                x = x + (Math.random().toFloat() - 0.5f) * size,
                y = y,
                text = emoji,
                vx = (Math.random().toFloat() - 0.5f) * size * 3f,
                vy = -size * (1.5f + Math.random().toFloat() * 2f),
                size = size * (0.7f + Math.random().toFloat() * 0.5f),
            )
        )
    }
}
