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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.Animator
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.anim.EyeState
import com.dobdmitry.drawnpets.anim.OutfitLayout
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Slot
import com.dobdmitry.drawnpets.data.WornItem
import kotlin.math.abs
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

private val sadMatrixFilter = android.graphics.ColorMatrixColorFilter(
    android.graphics.ColorMatrix().apply { setSaturation(0.4f) }
)
private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    color = 0xFF3A2A55.toInt()
}
private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
private val tmpPoint = FloatArray(2)
private val tmpRect = android.graphics.RectF()

/**
 * Рисует оживший рисунок. [feet] — точка «ног» (низ по центру), [height] — высота картинки.
 * Персонаж по-настоящему двигается: ноги шагают, руки машут, глаза моргают
 * (см. [Animator]). Если [action] не задан, он выбирается по флагам.
 * Возвращает прямоугольник, который занял персонаж (для нажатий).
 */
fun DrawScope.drawCreature(
    sprite: CreatureSprite?,
    feet: Offset,
    height: Float,
    time: Float,
    seed: Int = 0,
    facingLeft: Boolean = false,
    moving: Boolean = false,
    sleeping: Boolean = false,
    sad: Boolean = false,
    outfit: List<WornItem> = emptyList(),
    jump: Float = 0f,
    swimming: Boolean = false,
    shadow: Boolean = true,
    alpha: Float = 1f,
    action: Action? = null,
    actionTime: Float = 0f,
): Rect {
    val act = action ?: when {
        sleeping -> Action.SLEEP
        swimming -> Action.SWIM
        moving -> Action.WALK
        sad -> Action.SAD
        else -> Action.IDLE
    }
    val phase = seed * 0.37f
    val bw = sprite?.bitmap?.width ?: 1
    val bh = sprite?.bitmap?.height ?: 1
    val scale = height / bh
    val w = bw * scale

    var rot = 0f
    var lift = jump
    var sy = 1f
    when (act) {
        Action.WALK -> {
            lift += abs(sin(time * 8.5f + phase)) * height * 0.04f
            rot = sin(time * 8.5f + phase) * 3f
        }
        Action.RUN -> {
            lift += abs(sin(time * 13f + phase)) * height * 0.06f
            rot = 6f + sin(time * 13f + phase) * 3f
        }
        Action.HAPPY -> lift += abs(sin(time * 7f)) * height * 0.1f
        Action.JUMP -> sy = 1.06f
        Action.SWIM -> rot = sin(time * 2.2f + phase) * 6f
        else -> Unit
    }

    if (shadow && act != Action.SWIM) {
        val shadowScale = (1f - lift / (height * 1.2f)).coerceIn(0.4f, 1f)
        drawOval(
            color = Color.Black.copy(alpha = 0.16f * alpha * shadowScale),
            topLeft = Offset(feet.x - w * 0.4f * shadowScale, feet.y - height * 0.05f),
            size = Size(w * 0.8f * shadowScale, height * 0.1f),
        )
    }

    if (sprite == null) {
        drawEmoji("🙂", Offset(feet.x, feet.y - lift - height / 2), height * 0.8f, alpha)
        return Rect(feet.x - height / 2, feet.y - height, feet.x + height / 2, feet.y)
    }

    val anim = sprite.animator
    anim.action = act
    anim.t = time
    anim.actionT = actionTime
    anim.seed = seed
    anim.update()
    anim.fillMesh(sprite.verts)
    val rig = sprite.rig
    val flip = if (facingLeft) -1f else 1f

    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.translate(feet.x, feet.y - lift)
    canvas.rotate(rot * flip)
    canvas.scale(scale * flip, scale * sy)
    canvas.translate(-bw / 2f, -bh.toFloat())
    meshPaint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    meshPaint.colorFilter = if (act == Action.SLEEP || act == Action.SAD || sad) sadMatrixFilter else null
    // Наряды «за спиной» рисуем до самого персонажа
    drawOutfit(canvas, sprite, outfit, back = true, alpha = alpha)
    canvas.drawBitmapMesh(sprite.bitmap, Animator.GX, Animator.GY, sprite.verts, 0, null, 0, meshPaint)

    // Глаза: моргание, «счастливые» дуги, сон.
    val eyeState = anim.eyeState()
    val a255 = (alpha.coerceIn(0f, 1f) * 255).toInt()
    if (eyeState != EyeState.OPEN) {
        for (e in rig.eyes) {
            anim.map(e.cx, e.cy, tmpPoint)
            val x = tmpPoint[0]
            val y = tmpPoint[1]
            lidPaint.color = e.lidColor
            lidPaint.alpha = a255
            canvas.drawOval(x - e.rx * 1.15f, y - e.ry * 1.15f, x + e.rx * 1.15f, y + e.ry * 1.15f, lidPaint)
            inkPaint.strokeWidth = maxOf(3f, e.rx * 0.28f)
            inkPaint.alpha = a255
            if (eyeState == EyeState.HAPPY) {
                tmpRect.set(x - e.rx, y - e.ry * 0.3f, x + e.rx, y + e.ry * 0.9f)
                canvas.drawArc(tmpRect, 180f, 180f, false, inkPaint)
            } else {
                tmpRect.set(x - e.rx, y - e.ry * 0.9f, x + e.rx, y + e.ry * 0.3f)
                canvas.drawArc(tmpRect, 0f, 180f, false, inkPaint)
            }
        }
    }
    // Румянец, когда весело
    if ((act == Action.HAPPY || act == Action.WAVE) && rig.eyes.size >= 2) {
        val mid = (rig.eyes.first().cx + rig.eyes.last().cx) / 2
        fillPaint.color = 0xFFFF6F91.toInt()
        fillPaint.alpha = (a255 * 0.45f).toInt()
        for (e in rig.eyes) {
            anim.map(e.cx + (e.cx - mid) * 0.35f, e.cy + e.ry * 1.7f, tmpPoint)
            canvas.drawOval(tmpPoint[0] - e.rx * 0.8f, tmpPoint[1] - e.ry * 0.45f, tmpPoint[0] + e.rx * 0.8f, tmpPoint[1] + e.ry * 0.45f, fillPaint)
        }
    }
    // Слёзка, когда грустно
    if (act == Action.SAD && rig.eyes.isNotEmpty()) {
        val e = rig.eyes.first()
        val k = (time * 0.8f + phase) % 1f
        anim.map(e.cx, e.cy + e.ry * (1.2f + k * 3f), tmpPoint)
        fillPaint.color = 0xFF6EC4FF.toInt()
        fillPaint.alpha = (a255 * (1f - k)).toInt()
        canvas.drawCircle(tmpPoint[0], tmpPoint[1], maxOf(4f, e.rx * 0.4f), fillPaint)
    }
    // Рот открывается, когда персонаж ест или говорит
    val open = anim.mouthOpen()
    val mouth = anim.mouthPoint()
    if (open > 0.05f && mouth != null) {
        anim.map(mouth.first, mouth.second, tmpPoint)
        val mw = anim.mouthWidth()
        val mh = mw * 0.75f * open + 2f
        fillPaint.color = 0xFF5A1430.toInt()
        fillPaint.alpha = a255
        canvas.drawOval(tmpPoint[0] - mw / 2, tmpPoint[1] - mh / 2, tmpPoint[0] + mw / 2, tmpPoint[1] + mh / 2, fillPaint)
        fillPaint.color = 0xFFFF7A9A.toInt()
        fillPaint.alpha = a255
        canvas.drawOval(tmpPoint[0] - mw * 0.25f, tmpPoint[1], tmpPoint[0] + mw * 0.25f, tmpPoint[1] + mh / 2, fillPaint)
    }
    drawOutfit(canvas, sprite, outfit, back = false, alpha = alpha)
    canvas.restore()

    if (act == Action.SLEEP) {
        val k = (time * 0.7f + phase) % 1f
        drawEmoji("💤", Offset(feet.x + w * 0.35f + k * 20f, feet.y - height - k * 30f), height * 0.28f, 1f - k)
    }
    val top = feet.y - lift - (bh - rig.top) * scale
    return Rect(feet.x - w / 2, top, feet.x + w / 2, feet.y)
}

private val outfitMatrix = android.graphics.Matrix()
private val outfitBasis = FloatArray(9)


/**
 * Рисует наряды в системе координат картинки (холст уже повёрнут и отмасштабирован),
 * поэтому они двигаются вместе с телом, а предметы в руке — вместе с рукой.
 */
private fun drawOutfit(canvas: android.graphics.Canvas, sprite: CreatureSprite, outfit: List<WornItem>, back: Boolean, alpha: Float) {
    if (outfit.isEmpty()) return
    val rig = sprite.rig
    val anim = sprite.animator
    for (item in outfit) {
        val acc = Catalog.accessory(item.id) ?: continue
        if ((acc.slot == Slot.BACK) != back) continue
        val p = OutfitLayout.placement(item, rig) ?: continue
        val li = OutfitLayout.limbFor(item, p, rig)
        val (ax, ay) = OutfitLayout.attachment(item, p)
        // Transform the whole accessory around its attachment, keeping the grip on the hand.
        anim.attachmentTransform(ax, ay, li, outfitBasis)
        outfitMatrix.setValues(outfitBasis)
        canvas.save()
        canvas.concat(outfitMatrix)
        OutfitArt.draw(canvas, acc.id, p.x, p.y, p.size, p.rot, alpha)
        canvas.restore()
    }
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
