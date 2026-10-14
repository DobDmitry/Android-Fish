package com.dobdmitry.drawnpets.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.FieldCoin
import com.dobdmitry.drawnpets.data.Habitat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Панорама локации: фон шире экрана, его можно двигать пальцем влево-вправо
 * (с инерцией). Все объекты сцены задаются в долях ширины «мира».
 */
class ScenePan(private val startFraction: Float = 0.5f) {
    /** Сдвиг: сколько пикселей мира ушло за левый край экрана. */
    var offset = -1f
    var velocity = 0f
    var worldW = 0f
    var worldH = 0f
    var viewW = 0f

    fun clamp() {
        val maxOff = max(0f, worldW - viewW)
        if (offset < 0f) offset = 0f
        if (offset > maxOff) offset = maxOff
    }

    /** Инерция после броска пальцем. */
    fun step(dt: Float) {
        if (abs(velocity) < 5f) { velocity = 0f; return }
        offset += velocity * dt
        velocity *= exp(-4f * dt)
        val before = offset
        clamp()
        if (before != offset) velocity = 0f
    }

    /** Экранная координата X для доли ширины мира. */
    fun sx(fraction: Float) = fraction * worldW - offset

    /** Доля ширины мира для экранной координаты X. */
    fun fx(screenX: Float) = if (worldW > 0f) (screenX + offset) / worldW else 0f

    internal fun layout(viewWidth: Float, ww: Float, wh: Float) {
        val first = offset < 0f
        viewW = viewWidth
        worldW = ww
        worldH = wh
        if (first) offset = (worldW - viewW) * startFraction
        clamp()
    }
}

/** Рисует фон локации по высоте экрана и сдвигает его по панораме. */
fun DrawScope.drawSceneBackground(image: ImageBitmap, pan: ScenePan) {
    val scale = max(size.height / image.height, size.width / image.width)
    val ww = image.width * scale
    val wh = image.height * scale
    pan.layout(size.width, ww, wh)
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((-pan.offset).roundToInt(), 0),
        dstSize = IntSize(ww.roundToInt(), wh.roundToInt()),
        filterQuality = FilterQuality.High,
    )
}

/** Жест «тянуть сцену влево-вправо». */
fun Modifier.scenePan(pan: ScenePan): Modifier = pointerInput(pan) {
    val tracker = VelocityTracker()
    detectHorizontalDragGestures(
        onDragStart = { tracker.resetTracking(); pan.velocity = 0f },
        onDragEnd = { pan.velocity = -tracker.calculateVelocity().x },
        onDragCancel = { pan.velocity = 0f },
        onHorizontalDrag = { change, dx ->
            tracker.addPosition(change.uptimeMillis, change.position)
            pan.offset -= dx
            pan.clamp()
            change.consume()
        },
    )
}

/**
 * Монетки этого места: лежат на земле, покачиваются и поблёскивают.
 * [rects] заполняется прямоугольниками для нажатий. Если монетка за краем
 * экрана — у края появляется маленькая подсказка.
 */
fun DrawScope.drawFieldCoins(
    coins: List<FieldCoin>, pan: ScenePan, minY: Float, maxY: Float, t: Float, rects: MutableMap<Long, Rect>,
    paperIcons: ImageBitmap? = null,
) {
    rects.clear()
    val s = if (paperIcons != null) max(size.height * 0.06f, 24.dp.toPx()) else size.height * 0.045f
    var leftHint = false
    var rightHint = false
    for (c in coins) {
        val x = pan.sx(c.x)
        val ground = size.height * (minY + c.y * (maxY - minY))
        val bob = sin(t * 3f + c.id % 7) * s * 0.12f
        val center = Offset(x, ground - s * 0.7f + bob)
        if (x < -s) { leftHint = true; continue }
        if (x > size.width + s) { rightHint = true; continue }
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(x - s * 0.35f, ground - s * 0.12f), Size(s * 0.7f, s * 0.22f))
        if (paperIcons != null) drawPaperIcon(paperIcons, PaperIcon.COIN, center, s) else drawEmoji("🪙", center, s)
        if ((t + c.id % 5) % 2.5f < 0.3f) drawEmoji("✨", Offset(center.x + s * 0.4f, center.y - s * 0.4f), s * 0.5f)
        rects[c.id] = Rect(center.x - s * 0.7f, center.y - s * 0.7f, center.x + s * 0.7f, center.y + s * 0.7f)
    }
    val a = 0.6f + 0.4f * sin(t * 4f)
    if (paperIcons != null) {
        if (leftHint) drawPaperIcon(paperIcons, PaperIcon.COIN, Offset(22.dp.toPx(), size.height * 0.35f), 24.dp.toPx())
        if (rightHint) drawPaperIcon(paperIcons, PaperIcon.COIN, Offset(size.width - 22.dp.toPx(), size.height * 0.35f), 24.dp.toPx())
    } else {
        if (leftHint) drawEmoji("🪙", Offset(30f, size.height * 0.42f), 40f, a)
        if (rightHint) drawEmoji("🪙", Offset(size.width - 30f, size.height * 0.42f), 40f, a)
    }
}

/** Вкладки мест: двор, дом, пруд, Марс. В местах с монетками — значок 🪙. */
@Composable
fun LocationTabs(current: Habitat, onSelect: (Habitat) -> Unit, modifier: Modifier = Modifier, coinsAt: Set<Habitat> = emptySet()) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Habitat.entries.forEach { h ->
            val on = h == current
            Text(
                "${h.emoji} ${h.title}" + if (h in coinsAt) " 🪙" else "",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Palette.Lemon else Color.White.copy(alpha = 0.85f))
                    .border(2.dp, Palette.Ink.copy(alpha = if (on) 0.8f else 0.25f), RoundedCornerShape(50))
                    .clickable(enabled = !on) { Sounds.play(Sfx.TAP); onSelect(h) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
