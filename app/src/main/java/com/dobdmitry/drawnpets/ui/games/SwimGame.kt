package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.WornItem
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val JUMP_TIME = 0.65f
private const val DIVE_TIME = 0.8f
private const val HURT_TIME = 1.6f
private const val MAGNET_TIME = 10f
private const val TOW_TIME = 5f
private const val DOUBLE_TIME = 10f
private const val PLAYER_Y = 0.74f
private const val PIF = PI.toFloat()
/** Длина одной реки в высотах экрана: 10 м на экран, значит 200 м. */
private const val SEGMENT = 20f

private val Ink = GameInk

private enum class Kind(val pickup: Boolean) {
    ROCK(false), LOG(false), BRIDGE(false), WHIRL(false),
    STAR(true), MAGNET(true), SHIELD(true), TOW(true), DOUBLE(true),
}

/** Предмет на реке. [y] — экранная координата, всё плывёт сверху вниз. */
private class Thing(val kind: Kind, val lane: Int, var y: Float, val river: River) {
    /** Своя x — только у звёздочек, которых тянет магнит. */
    var x = Float.NaN
    /** Звёздочка собрана или препятствие уже задето. */
    var done = false
}

/** Оформление реки: меняется случайно каждые [SEGMENT] высот экрана (200 м). */
private enum class River(
    val title: String,
    val bank: Color,
    val bankDark: Color,
    val shore: Color,
    val water: Color,
    val waterLight: Color,
    val ripple: Color,
) {
    FOREST("Лесная речка", Color(0xFF76C055), Color(0xFF4E9F3A), Color(0xFFE8D29B), Color(0xFF2C9DB6), Color(0xFF4CBCD0), Color.White),
    WINTER("Зимняя речка", Color(0xFFF2F8FF), Color(0xFFCFE2F3), Color(0xFFBFE3F5), Color(0xFF3F86C2), Color(0xFF66A9DC), Color.White),
    JUNGLE("Река в джунглях", Color(0xFF3E9E4A), Color(0xFF2D7A37), Color(0xFFE8C98A), Color(0xFF1F9C8C), Color(0xFF45C0AC), Color.White),
    MARS("Марсианский канал", Color(0xFFD9734A), Color(0xFFB5532F), Color(0xFFF0A070), Color(0xFF6E5FD8), Color(0xFF9184EE), Color(0xFFE6E0FF)),
    MILK("Молочная река", Color(0xFFF49AC1), Color(0xFFE07AA8), Color(0xFFFFD1E3), Color(0xFFEDE6D8), Color(0xFFFFFDF7), Color(0xFFB8AC98)),
}

private class SwimState {
    var size = Size.Zero
    var started = false
    var over = false
    var overT = 0f
    var runT = 0f
    var lane = 1
    var laneF = 1f
    /** Время с начала прыжка/нырка; −1 — не прыгает/не ныряет. */
    var jumpT = -1f
    var diveT = -1f
    var hearts = 3
    var hurtT = 0f
    var stars = 0
    var dist = 0f
    var spawnIn = 0f
    var magnetT = 0f
    var shield = false
    var towT = 0f
    var doubleT = 0f
    var slowT = 0f
    var spinT = 0f
    val things = mutableListOf<Thing>()
    val particles = mutableListOf<Particle>()
    /** Река, в которую уже заплыл персонаж, и таймер её названия на экране. */
    var shownSegment = 0
    var bannerT = 0f
    private val rivers = mutableListOf(River.entries.random())

    /** Реки идут друг за другом, каждая следующая — случайная и не такая, как предыдущая. */
    fun riverAt(k: Int): River {
        while (rivers.size <= k) rivers += River.entries.filter { it != rivers.last() }.random()
        return rivers[k.coerceAtLeast(0)]
    }

    /** Номер реки в точке экрана [y]: дальше по течению — больше. */
    fun segmentAt(y: Float, h: Float) = floor((dist - y + h) / (h * SEGMENT)).toInt()
}

private fun laneX(w: Float, lane: Float) = w * (0.5f + (lane - 1f) * 0.25f)

private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777215f
}

/**
 * «Заплыв» — раннер в духе Subway Surfers, только по реке и с видом сверху.
 * Свайп влево-вправо — сменить дорожку, вверх — прыжок через бревно,
 * вниз — нырок под мостик. Камни можно только оплыть, водоворот кружит и
 * отнимает звёздочки. Три сердечка; очки — собранные звёздочки.
 */
@Composable
fun SwimGame(sprite: CreatureSprite?, outfit: List<WornItem>, onFinish: (Int) -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val st = remember { SwimState() }
    val art = rememberStoryArtwork()
    var time by remember { mutableFloatStateOf(0f) }
    var hudStars by remember { mutableIntStateOf(0) }
    var hudHearts by remember { mutableIntStateOf(3) }
    var hudMeters by remember { mutableIntStateOf(0) }

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
                if (sz.width <= 0f || !st.started) return@withFrameNanos
                if (st.over) {
                    st.overT += dt
                    running = st.overT < 1.3f
                    return@withFrameNanos
                }
                st.update(dt, sz.width, sz.height)
                hudStars = st.stars
                hudHearts = st.hearts
                hudMeters = (st.dist / sz.height * 10f).toInt()
            }
        }
        finish(st.stars)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val threshold = 24.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!st.started) {
                            st.started = true
                            Sounds.play(Sfx.SPLASH, volume = 0.6f)
                        }
                        var acc = Offset.Zero
                        var fired = false
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            acc += ch.positionChange()
                            if (!fired && acc.getDistance() > threshold) {
                                fired = true
                                st.swipe(acc)
                            }
                            ch.consume()
                        }
                    }
                }
        ) {
            st.size = size
            val t = time
            val w = size.width
            val h = size.height
            drawRivers(st, t)
            for (o in st.things) if (o.kind != Kind.BRIDGE) drawThing(art, o.river, o, t)
            val diving = st.diveT >= 0f
            if (diving) drawSwimmer(st, sprite, outfit, t)
            for (o in st.things) if (o.kind == Kind.BRIDGE) drawThing(art, o.river, o, t)
            if (!diving) drawSwimmer(st, sprite, outfit, t)
            drawParticles(st.particles)
            drawBonusBar(st)
            if (st.bannerT > 0f) {
                drawLabel(st.riverAt(st.shownSegment).title, Offset(w / 2, h * 0.3f), w * 0.08f, Color(0xFF2F6F80), alpha = st.bannerT.coerceAtMost(1f))
            }
            if (!st.started) {
                val river = st.riverAt(0)
                drawRect(GamePaper.copy(alpha = 0.5f))
                drawLabel(river.title, Offset(w / 2, h * 0.34f), w * 0.08f, Color(0xFF4F7F8C))
                drawLabel("← →  сменить дорожку", Offset(w / 2, h * 0.44f), w * 0.052f)
                drawLabel("↑  прыжок через бревно", Offset(w / 2, h * 0.5f), w * 0.052f)
                drawLabel("↓  нырок под мостик", Offset(w / 2, h * 0.56f), w * 0.052f)
                drawLabel("Коснись, чтобы поплыть!", Offset(w / 2, h * 0.64f + sin(t * 4f) * h * 0.005f), w * 0.06f, Palette.Coral)
            }
        }
        GameHud("⭐ $hudStars · $hudMeters м", if (hudHearts > 0) "❤️".repeat(hudHearts) else "💔")
    }
}

private fun SwimState.swipe(d: Offset) {
    if (over) return
    if (abs(d.x) > abs(d.y)) {
        val nl = (lane + if (d.x > 0f) 1 else -1).coerceIn(0, 2)
        if (nl != lane) {
            lane = nl
            Sounds.play(Sfx.TAP, volume = 0.5f, rate = 1.3f)
        }
    } else if (jumpT < 0f && diveT < 0f) {
        if (d.y < 0f) {
            jumpT = 0f
            Sounds.play(Sfx.JUMP, volume = 0.7f)
        } else {
            diveT = 0f
            Sounds.play(Sfx.SPLASH, volume = 0.7f)
        }
    }
}

private fun SwimState.update(dt: Float, w: Float, h: Float) {
    runT += dt
    hurtT -= dt; magnetT -= dt; towT -= dt; doubleT -= dt; slowT -= dt; spinT -= dt
    // скорость растёт около полутора минут, потом держится
    var speed = h * (0.42f + (runT * 0.006f).coerceAtMost(0.5f))
    if (towT > 0f) speed *= 1.7f
    if (slowT > 0f) speed *= 0.5f
    dist += speed * dt
    laneF += (lane - laneF) * (dt * 12f).coerceAtMost(1f)
    val px = laneX(w, laneF)
    val py = h * PLAYER_Y
    if (jumpT >= 0f) {
        jumpT += dt
        if (jumpT >= JUMP_TIME) {
            jumpT = -1f
            Sounds.play(Sfx.SPLASH, volume = 0.5f)
            particles.burst(px, py, "💦", 3, w * 0.05f)
        }
    }
    if (diveT >= 0f) {
        diveT += dt
        if (diveT >= DIVE_TIME) diveT = -1f
    }
    spawnIn -= speed * dt
    if (spawnIn <= 0f) spawn(h)
    bannerT -= dt
    val seg = segmentAt(py, h)
    if (seg > shownSegment) {
        shownSegment = seg
        bannerT = 2.5f
        Sounds.play(Sfx.MAGIC, volume = 0.6f)
    }

    val jumping = jumpT >= 0f
    val diving = diveT >= 0f
    for (o in things) {
        o.y += speed * dt
        if (o.done) continue
        if (o.kind == Kind.STAR && magnetT > 0f && o.y > py - h * 0.5f && o.y < py + w * 0.1f) {
            if (o.x.isNaN()) o.x = laneX(w, o.lane.toFloat())
            val k = (dt * 7f).coerceAtMost(1f)
            o.x += (px - o.x) * k
            o.y += (py - o.y) * k
        }
        val dx = abs((if (o.x.isNaN()) laneX(w, o.lane.toFloat()) else o.x) - px)
        val dy = abs(o.y - py)
        when (o.kind) {
            Kind.STAR -> if (dx < w * 0.1f && dy < w * 0.09f) collectStar(o, px, py, w)
            Kind.MAGNET, Kind.SHIELD, Kind.TOW, Kind.DOUBLE -> if (dx < w * 0.11f && dy < w * 0.1f) takeBonus(o, px, py, w)
            Kind.ROCK -> if (dx < w * 0.11f && dy < w * 0.1f) hit(o, px, py, w)
            Kind.LOG -> if (dx < w * 0.12f && dy < w * 0.07f && !jumping) hit(o, px, py, w)
            Kind.BRIDGE -> if (dx < w * 0.12f && dy < w * 0.075f && !diving) hit(o, px, py, w)
            Kind.WHIRL -> if (dx < w * 0.1f && dy < w * 0.07f && !jumping) whirl(o, px, py, w)
        }
    }
    things.removeAll { it.y > h + w * 0.3f || (it.done && it.kind.pickup) }
}

private fun SwimState.collectStar(o: Thing, px: Float, py: Float, w: Float) {
    o.done = true
    val add = if (doubleT > 0f) 2 else 1
    stars += add
    Sounds.play(Sfx.COIN, volume = 0.55f, rate = 1f + (stars % 6) * 0.05f)
    particles += Particle(px, py - w * 0.14f, "+$add", 0f, -w * 0.25f, w * 0.06f, decay = 1.6f, isLabel = true, color = Palette.Coral)
}

private fun SwimState.takeBonus(o: Thing, px: Float, py: Float, w: Float) {
    o.done = true
    val name = when (o.kind) {
        Kind.MAGNET -> { magnetT = MAGNET_TIME; "Магнит!" }
        Kind.SHIELD -> { shield = true; "Пузырь!" }
        Kind.TOW -> { towT = TOW_TIME; "Рыбка-буксир!" }
        else -> { doubleT = DOUBLE_TIME; "Звёзды ×2!" }
    }
    Sounds.play(Sfx.MAGIC)
    particles += Particle(px, py - w * 0.2f, name, 0f, -w * 0.15f, w * 0.065f, decay = 0.9f, isLabel = true, color = Color(0xFF4F7F8C))
}

private fun SwimState.hit(o: Thing, px: Float, py: Float, w: Float) {
    o.done = true
    if (towT > 0f) {
        particles.burst(px, py - w * 0.1f, "💦", 3, w * 0.05f)
        return
    }
    if (hurtT > 0f) return
    if (shield) {
        shield = false
        hurtT = 1f
        Sounds.play(Sfx.POP)
        particles.burst(px, py - w * 0.1f, "💦", 5, w * 0.05f)
        return
    }
    hearts--
    hurtT = HURT_TIME
    Sounds.play(Sfx.HURT)
    particles.burst(px, py - w * 0.1f, "💥", 3, w * 0.06f)
    if (hearts <= 0) over = true
}

private fun SwimState.whirl(o: Thing, px: Float, py: Float, w: Float) {
    o.done = true
    if (towT > 0f) return
    slowT = 1.2f
    spinT = 1f
    val lost = minOf(stars, 2)
    stars -= lost
    Sounds.play(Sfx.BUBBLE)
    if (lost > 0) {
        particles += Particle(px, py - w * 0.14f, "−$lost ⭐", 0f, -w * 0.2f, w * 0.06f, decay = 1.2f, isLabel = true, color = Palette.Ink)
    }
}

/** Добавляет на реку следующий кусок: препятствия и дорожку из звёздочек. */
private fun SwimState.spawn(h: Float) {
    val d = (runT / 90f).coerceAtMost(1f)
    val y0 = -h * 0.3f
    val sp = h * 0.075f
    fun put(kind: Kind, lane: Int, y: Float) { things += Thing(kind, lane, y, riverAt(segmentAt(y, h))) }
    fun stars(lane: Int, from: Float, n: Int) { for (i in 0 until n) put(Kind.STAR, lane, from - i * sp) }
    fun others(lane: Int) = (0..2).filter { it != lane }
    val length: Float
    if (Random.nextFloat() < 0.13f) {
        val l = Random.nextInt(3)
        put(listOf(Kind.MAGNET, Kind.SHIELD, Kind.TOW, Kind.DOUBLE).random(), l, y0)
        stars(l, y0 + sp * 3, 2)
        stars(l, y0 - sp * 2, 2)
        length = h * 0.3f
    } else {
        val pool = buildList {
            add(0); add(0); add(1); add(1); add(2); add(3); add(4)
            if (d > 0.25f) { add(5); add(5) }
            if (d > 0.45f) { add(6); add(6) }
        }
        when (pool.random()) {
            0 -> { // камни на двух дорожках
                val f = Random.nextInt(3)
                others(f).forEach { put(Kind.ROCK, it, y0 - Random.nextFloat() * h * 0.05f) }
                stars(f, y0 + sp * 2, 5)
                length = h * 0.3f
            }
            1 -> { // бревно, звёздочки ведут через него
                val l = Random.nextInt(3)
                put(Kind.LOG, l, y0 - sp * 2)
                stars(l, y0, 5)
                length = h * 0.35f
            }
            2 -> { // брёвна поперёк всей реки — только прыгать
                for (l in 0..2) put(Kind.LOG, l, y0)
                stars(Random.nextInt(3), y0 - sp * 2, 4)
                length = h * 0.4f
            }
            3 -> { // низкий мостик
                val l = Random.nextInt(3)
                put(Kind.BRIDGE, l, y0 - sp)
                stars(l, y0, 3)
                if (d > 0.3f) put(Kind.ROCK, others(l).random(), y0 - sp)
                length = h * 0.3f
            }
            4 -> { // водоворот
                val l = Random.nextInt(3)
                put(Kind.WHIRL, l, y0)
                stars(others(l).random(), y0 + sp, 5)
                length = h * 0.35f
            }
            5 -> { // зигзаг из двух рядов камней
                val a = Random.nextInt(3)
                val b = others(a).random()
                others(a).forEach { put(Kind.ROCK, it, y0) }
                others(b).forEach { put(Kind.ROCK, it, y0 - h * 0.34f) }
                stars(a, y0 + sp, 2)
                stars(b, y0 - h * 0.34f + sp, 3)
                length = h * 0.5f
            }
            else -> { // камни и бревно в свободной дорожке
                val f = Random.nextInt(3)
                others(f).forEach { put(Kind.ROCK, it, y0) }
                put(Kind.LOG, f, y0)
                stars(f, y0 + sp * 3, 2)
                stars(f, y0 - sp * 2, 3)
                length = h * 0.35f
            }
        }
    }
    spawnIn = length + h * (0.36f - 0.14f * d)
}

// ---------- рисование ----------

/** Река на экране; на стыке двух рек — шов рваной бумаги. */
private fun DrawScope.drawRivers(st: SwimState, t: Float) {
    val w = size.width
    val h = size.height
    val top = st.segmentAt(0f, h)
    if (top == st.segmentAt(h, h)) {
        drawRiver(st.riverAt(top), st.dist, t)
        return
    }
    val yb = st.dist + h - top * h * SEGMENT
    val seam = List(25) { i -> Offset(w * (24 - i) / 24f, yb + (hash(i + top * 31) - 0.5f) * h * 0.014f) }
    clipRect(top = yb - h * 0.01f) { drawRiver(st.riverAt(top - 1), st.dist, t) }
    val upper = Path().apply {
        moveTo(0f, -1f); lineTo(w, -1f)
        seam.forEach { lineTo(it.x, it.y) }
        close()
    }
    clipPath(upper) { drawRiver(st.riverAt(top), st.dist, t) }
    val line = Path().apply {
        moveTo(seam[0].x, seam[0].y)
        seam.drop(1).forEach { lineTo(it.x, it.y) }
    }
    translate(0f, h * 0.004f) { drawPath(line, Color.Black.copy(alpha = 0.18f), style = Stroke(w * 0.012f)) }
    drawPath(line, Color(0xFFFFF9EC), style = Stroke(w * 0.006f))
}

private fun DrawScope.drawRiver(r: River, dist: Float, t: Float) {
    val w = size.width
    val h = size.height
    drawRect(r.bank)
    val l0 = w * 0.13f
    val r0 = w * 0.87f
    // берега извилистые, изгибы привязаны к пройденному пути
    fun edge(y: Float, side: Int): Float {
        val wy = (y - dist) / h
        return (if (side < 0) l0 else r0) + side * w * (0.014f * sin(wy * 7f) + 0.008f * sin(wy * 17f + 1f))
    }
    fun band(extra: Float) = Path().apply {
        val stepY = h / 24f
        for (i in 0..26) {
            val y = -stepY + i * stepY
            if (i == 0) moveTo(edge(y, -1) - extra, y) else lineTo(edge(y, -1) - extra, y)
        }
        for (i in 26 downTo 0) {
            val y = -stepY + i * stepY
            lineTo(edge(y, 1) + extra, y)
        }
        close()
    }
    // слои рваной бумаги: тень под берегом, песок, вода с притенёнными краями
    drawPath(band(w * 0.034f), Color(0xFF4A3B2A).copy(alpha = 0.12f))
    drawPath(band(w * 0.025f), r.shore)
    val water = band(0f)
    drawPath(water, Brush.horizontalGradient(listOf(r.water, r.waterLight, r.water), l0, r0))
    drawPath(water, Color(0xFF3A3020).copy(alpha = 0.1f), style = Stroke(w * 0.014f))

    // разделители дорожек — еле заметный пунктир
    val dash = h * 0.05f
    val off = dist % dash
    for (bx in listOf(w * 0.375f, w * 0.625f)) {
        var y = off - dash
        while (y < h) {
            drawLine(r.ripple.copy(alpha = 0.14f), Offset(bx, y), Offset(bx, y + dash * 0.45f), w * 0.006f, StrokeCap.Round)
            y += dash
        }
    }
    // струйки течения
    val rs = h * 0.06f
    for (m in floor((dist - h * 1.05f) / rs).toInt()..ceil((dist + h * 0.05f) / rs).toInt()) {
        val y = dist - m * rs
        val x = l0 + w * 0.05f + hash(m) * (r0 - l0 - w * 0.1f)
        drawLine(r.ripple.copy(alpha = 0.45f), Offset(x, y), Offset(x, y + h * 0.022f), w * 0.007f, StrokeCap.Round)
    }
    // кувшинки, льдинки и прочее у берега
    val ws = h * 0.13f
    for (m in floor((dist - h * 1.1f) / ws).toInt()..ceil((dist + h * 0.1f) / ws).toInt()) {
        if (hash(m * 3 + 11) < 0.55f) continue
        val y = dist - m * ws
        val side = if (hash(m * 7 + 5) > 0.5f) 1 else -1
        drawWaterDecor(r, Offset(edge(y, side) - side * w * 0.045f, y), w * 0.034f, m, t)
    }
    // деревья, сугробы, кратеры на берегах
    val bs = h * 0.1f
    for (m in floor((dist - h * 1.1f) / bs).toInt()..ceil((dist + h * 0.1f) / bs).toInt()) {
        val y = dist - m * bs
        for (side in listOf(-1, 1)) {
            if (hash(m * 2 + (side + 1) * 31 + 7) > 0.8f) continue
            val x = (if (side < 0) w * 0.06f else w * 0.94f) + (hash(m * 5 + side * 3) - 0.5f) * w * 0.03f
            val kind = (hash(m * 13 + side) * 3f).toInt().coerceAtMost(2)
            drawBankDecor(r, kind, Offset(x, y), w * 0.058f, m * 2 + side)
        }
    }
    // бумажное зерно, плывёт вместе с рекой
    for (k in 0 until 160) {
        val y = (hash(k * 5 + 1) * h * 1.5f + dist) % (h * 1.5f) - h * 0.25f
        drawCircle(Color(0xFF866A49).copy(alpha = 0.08f), w * (0.0025f + 0.002f * hash(k)), Offset(hash(k * 3 + 2) * w, y))
    }
}

private fun DrawScope.drawBankDecor(r: River, kind: Int, c: Offset, s: Float, seed: Int) {
    when (r) {
        River.FOREST -> when (kind) {
            0 -> {
                drawCircle(r.bankDark, s * 1.15f, c)
                drawCircle(Color(0xFF5DB547), s * 0.8f, c + Offset(-s * 0.2f, -s * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.18f), s * 0.3f, c + Offset(-s * 0.4f, -s * 0.45f))
            }
            1 -> for (i in 0..2) drawCircle(Color(0xFF3F8F35), s * 0.5f, c + Offset((i - 1) * s * 0.55f, (i % 2) * s * 0.3f))
            else -> {
                for (i in 0..4) {
                    val a = i * 1.2566f
                    drawCircle(Color.White, s * 0.2f, c + Offset(cos(a) * s * 0.3f, sin(a) * s * 0.3f))
                }
                drawCircle(Color(0xFFFFD84D), s * 0.18f, c)
            }
        }
        River.WINTER -> when (kind) {
            0 -> {
                drawCircle(Color(0xFF2E6B4A), s * 1.1f, c)
                drawCircle(Color(0xFF3E8A5E), s * 0.75f, c)
                drawCircle(Color(0xFF4FA372), s * 0.4f, c)
                for (i in 0..5) {
                    val a = i * 1.047f + 0.3f
                    drawCircle(Color.White, s * 0.14f, c + Offset(cos(a) * s * 0.85f, sin(a) * s * 0.85f))
                }
            }
            1 -> {
                drawCircle(Color.White, s * 0.8f, c)
                drawCircle(Color(0xFFB5D3EA), s * 0.8f, c, style = Stroke(s * 0.08f))
                val head = c + Offset(0f, -s * 0.9f)
                drawCircle(Color.White, s * 0.5f, head)
                drawCircle(Color(0xFFB5D3EA), s * 0.5f, head, style = Stroke(s * 0.08f))
                drawCircle(Ink, s * 0.07f, head + Offset(-s * 0.18f, -s * 0.1f))
                drawCircle(Ink, s * 0.07f, head + Offset(s * 0.18f, -s * 0.1f))
                drawCircle(Color(0xFFFF9A3C), s * 0.1f, head + Offset(0f, s * 0.1f))
            }
            else -> {
                drawOval(Color(0xFFD6E6F4), Offset(c.x - s, c.y - s * 0.5f), Size(s * 2f, s))
                drawOval(Color.White, Offset(c.x - s * 0.8f, c.y - s * 0.6f), Size(s * 1.4f, s * 0.8f))
            }
        }
        River.JUNGLE -> when (kind) {
            0 -> {
                for (i in 0..6) rotate(i * 51.4f + seed * 13f, pivot = c) {
                    drawOval(Color(0xFF2F8F3A), Offset(c.x - s * 0.25f, c.y - s * 1.3f), Size(s * 0.5f, s * 1.1f))
                }
                drawCircle(Color(0xFF8B5A2B), s * 0.25f, c)
            }
            1 -> {
                for (i in 0..4) {
                    val a = i * 1.2566f
                    drawCircle(Color(0xFFFF6FA8), s * 0.32f, c + Offset(cos(a) * s * 0.4f, sin(a) * s * 0.4f))
                }
                drawCircle(Color(0xFFFFD84D), s * 0.25f, c)
            }
            else -> rotate(seed * 37f, pivot = c) {
                drawOval(Color(0xFF256B30), Offset(c.x - s * 0.45f, c.y - s), Size(s * 0.9f, s * 2f))
                drawLine(Color(0xFF4FA35A), c + Offset(0f, -s), c + Offset(0f, s), s * 0.08f)
            }
        }
        River.MARS -> when (kind) {
            0 -> {
                drawCircle(r.bankDark, s, c)
                drawCircle(Color(0xFFF0A070), s, c, style = Stroke(s * 0.2f))
                drawCircle(Color(0xFF9E4526), s * 0.5f, c + Offset(s * 0.15f, s * 0.15f))
            }
            1 -> for (i in 0..2) {
                val cc = c + Offset((i - 1) * s * 0.5f, (i % 2) * s * 0.3f)
                val k = 0.5f + 0.25f * (i % 2)
                val p = Path().apply {
                    moveTo(cc.x, cc.y - s * k); lineTo(cc.x + s * 0.3f, cc.y)
                    lineTo(cc.x, cc.y + s * k * 0.6f); lineTo(cc.x - s * 0.3f, cc.y); close()
                }
                drawPath(p, Color(0xFF7FE3F0))
                drawPath(p, Ink, style = Stroke(s * 0.08f))
            }
            else -> for (i in 0..2) drawCircle(Color(0xFFA8492A), s * (0.25f + 0.1f * i), c + Offset((i - 1) * s * 0.6f, (i % 2) * s * 0.4f))
        }
        River.MILK -> when (kind) {
            0 -> {
                drawLine(Color.White, c, c + Offset(s * 0.5f, s * 1.1f), s * 0.18f, StrokeCap.Round)
                drawCircle(Color(0xFFFF6FA8), s * 0.75f, c)
                for (i in 0..2) {
                    val rr = s * (0.2f + i * 0.18f)
                    drawArc(Color.White, i * 120f, 200f, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(s * 0.12f))
                }
            }
            1 -> {
                val col = listOf(Color(0xFF7EC8F0), Color(0xFFFFD84D), Color(0xFF5ED6A8))[abs(seed) % 3]
                drawPath(Path().apply { moveTo(c.x - s * 0.5f, c.y); lineTo(c.x - s * 1.05f, c.y - s * 0.4f); lineTo(c.x - s * 1.05f, c.y + s * 0.4f); close() }, col)
                drawPath(Path().apply { moveTo(c.x + s * 0.5f, c.y); lineTo(c.x + s * 1.05f, c.y - s * 0.4f); lineTo(c.x + s * 1.05f, c.y + s * 0.4f); close() }, col)
                drawCircle(col, s * 0.55f, c)
                drawCircle(Color.White.copy(alpha = 0.5f), s * 0.15f, c + Offset(-s * 0.2f, -s * 0.2f))
            }
            else -> {
                val cols = listOf(Color(0xFFFFFFFF), Color(0xFFFFD84D), Color(0xFF7EC8F0), Color(0xFF5ED6A8), Color(0xFFFF7A59))
                for (i in 0..5) {
                    val a = hash(seed * 11 + i) * 6.28f
                    val p = c + Offset(cos(a) * s * 0.8f * hash(seed + i * 3), sin(a) * s * 0.8f * hash(seed * 7 + i))
                    drawLine(cols[i % cols.size], p, p + Offset(cos(a * 2f) * s * 0.3f, sin(a * 2f) * s * 0.3f), s * 0.14f, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawWaterDecor(r: River, c: Offset, s: Float, seed: Int, t: Float) {
    when (r) {
        River.FOREST, River.JUNGLE -> {
            drawArc(Color(0xFF5FB04A), 20f + seed * 47f, 320f, true, Offset(c.x - s, c.y - s), Size(s * 2, s * 2))
            if (r == River.JUNGLE) drawCircle(Color(0xFFFF6FA8), s * 0.35f, c + Offset(s * 0.2f, -s * 0.1f))
        }
        River.WINTER -> {
            drawCircle(Color(0xFFE8F6FF), s * 0.8f, c)
            drawCircle(Color(0xFFB5DDF2), s * 0.8f, c, style = Stroke(s * 0.12f))
        }
        River.MARS -> for (i in 0..2) {
            val k = (t * 0.6f + i * 0.33f + seed * 0.1f) % 1f
            drawCircle(r.ripple.copy(alpha = 0.7f * (1f - k)), s * (0.2f + 0.5f * k), c + Offset((i - 1) * s * 0.7f, 0f), style = Stroke(s * 0.1f))
        }
        River.MILK -> drawCircle(Color(0xFFE2B26A), s * 0.6f, c, style = Stroke(s * 0.35f))
    }
}

private fun DrawScope.drawThing(art: StoryArtwork, r: River, o: Thing, t: Float) {
    val w = size.width
    val c = Offset(if (o.x.isNaN()) laneX(w, o.lane.toFloat()) else o.x, o.y)
    if (c.y < -w * 0.3f || c.y > size.height + w * 0.3f) return
    when (o.kind) {
        Kind.ROCK -> drawRock(art, r, c, w, o.lane)
        Kind.LOG -> drawLog(r, c, w, t)
        Kind.BRIDGE -> drawBridge(r, c, w)
        Kind.WHIRL -> drawWhirl(r, c, w, t)
        Kind.STAR -> rotate(sin(t * 3f + o.y * 0.02f) * 12f, c) {
            drawSprite(art, 11, c, w * 0.1f * (1f + 0.08f * sin(t * 6f + o.y * 0.01f)))
        }
        else -> drawBonus(o.kind, c, w * 0.065f * (1f + 0.06f * sin(t * 5f)), t)
    }
}

private fun rockPath(c: Offset, r: Float, seed: Int) = Path().apply {
    for (i in 0 until 9) {
        val a = i / 9f * 2f * PIF
        val rr = r * (1f + 0.12f * sin(i * 2.3f + seed))
        val x = c.x + cos(a) * rr
        val y = c.y + sin(a) * rr * 0.85f
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawRock(art: StoryArtwork, r: River, c: Offset, w: Float, seed: Int) {
    val rad = w * 0.085f
    drawOval(Color.White.copy(alpha = 0.55f), Offset(c.x - rad * 1.3f, c.y - rad * 1.05f), Size(rad * 2.6f, rad * 2.2f), style = Stroke(w * 0.008f))
    when (r) {
        River.WINTER -> { // льдина
            val p = rockPath(c, rad, seed)
            drawPath(p, Color(0xFFEAF3F5))
            drawPath(p, Color(0xFF8FB1C2), style = Stroke(w * 0.007f))
            drawOval(Color.White, Offset(c.x - rad * 0.55f, c.y - rad * 0.55f), Size(rad * 0.6f, rad * 0.4f))
        }
        River.MILK -> drawSprite(art, 3, c, rad * 2.2f) // печенька
        River.MARS -> drawSprite(art, 9, c, rad * 2.3f, ColorFilter.tint(Color(0xFFE0906A), BlendMode.Modulate))
        else -> {
            drawSprite(art, 9, c, rad * 2.3f)
            if (r == River.JUNGLE) {
                drawCircle(Color(0xFF7FA35E), rad * 0.28f, c + Offset(rad * 0.3f, -rad * 0.35f))
                drawCircle(Color(0xFF5DB547), rad * 0.2f, c + Offset(rad * 0.05f, -rad * 0.5f))
            }
        }
    }
}

/** Рисованный предмет из общего атласа игр, вписанный в квадрат [edge]. */
private fun DrawScope.drawSprite(art: StoryArtwork, index: Int, c: Offset, edge: Float, filter: ColorFilter? = null) {
    val b = art.items[index]
    val k = edge / maxOf(b.width, b.height)
    val dw = (b.width * k).roundToInt().coerceAtLeast(1)
    val dh = (b.height * k).roundToInt().coerceAtLeast(1)
    drawImage(
        b, IntOffset.Zero, IntSize(b.width, b.height),
        IntOffset((c.x - dw / 2f).roundToInt(), (c.y - dh / 2f).roundToInt()), IntSize(dw, dh),
        colorFilter = filter, filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawLog(r: River, c: Offset, w: Float, t: Float) {
    val lw = w * 0.21f
    val lh = w * 0.075f
    val y = c.y + sin(t * 2f + c.x) * w * 0.003f
    val tl = Offset(c.x - lw / 2, y - lh / 2)
    val sz = Size(lw, lh)
    val line = Stroke(w * 0.007f)
    drawRoundRect(
        Color.White.copy(alpha = 0.6f), tl - Offset(w * 0.015f, w * 0.015f), Size(lw + w * 0.03f, lh + w * 0.03f),
        CornerRadius(lh), style = Stroke(w * 0.008f),
    )
    when (r) {
        River.MARS -> { // труба
            drawRoundRect(Color(0xFF9AA6B2), tl, sz, CornerRadius(lh * 0.3f))
            for (i in 0..2) drawRect(Color(0xFF6F7C89), Offset(tl.x + lw * (0.2f + i * 0.28f), tl.y), Size(lw * 0.06f, lh))
            drawRect(Color.White.copy(alpha = 0.3f), Offset(tl.x, tl.y + lh * 0.15f), Size(lw, lh * 0.15f))
            drawRoundRect(Ink, tl, sz, CornerRadius(lh * 0.3f), style = line)
        }
        River.MILK -> { // шоколадка
            drawRoundRect(Color(0xFF7B4A2A), tl, sz, CornerRadius(lh * 0.2f))
            for (i in 1..3) drawLine(Color(0xFF9A6440), Offset(tl.x + lw * i / 4f, tl.y), Offset(tl.x + lw * i / 4f, tl.y + lh), w * 0.006f)
            drawLine(Color(0xFF9A6440), Offset(tl.x, y), Offset(tl.x + lw, y), w * 0.006f)
            drawRoundRect(Ink, tl, sz, CornerRadius(lh * 0.2f), style = line)
        }
        else -> {
            drawRoundRect(Color(0xFF8B5A2B), tl, sz, CornerRadius(lh / 2))
            for (i in -2..2) {
                val bx = c.x + i * lw * 0.16f
                drawLine(Color(0xFF6B4120), Offset(bx - lh * 0.3f, y - lh * 0.15f), Offset(bx + lh * 0.3f, y - lh * 0.15f), w * 0.005f, StrokeCap.Round)
            }
            if (r == River.WINTER) drawRoundRect(Color.White, Offset(tl.x + lh * 0.3f, tl.y - lh * 0.1f), Size(lw * 0.7f, lh * 0.45f), CornerRadius(lh * 0.25f))
            drawRoundRect(Ink, tl, sz, CornerRadius(lh / 2), style = line)
            val cut = Offset(tl.x + lw - lh * 0.7f, tl.y + lh * 0.08f)
            drawOval(Color(0xFFE2B77D), cut, Size(lh * 0.6f, lh * 0.84f))
            drawOval(Ink, cut, Size(lh * 0.6f, lh * 0.84f), style = Stroke(w * 0.005f))
        }
    }
}

private fun DrawScope.drawBridge(r: River, c: Offset, w: Float) {
    val bw = w * 0.25f
    val bd = w * 0.11f
    val tl = Offset(c.x - bw / 2, c.y - bd / 2)
    val corner = CornerRadius(w * 0.01f)
    drawRect(Color.Black.copy(alpha = 0.15f), tl + Offset(w * 0.02f, w * 0.035f), Size(bw, bd))
    val deck = when (r) { River.MARS -> Color(0xFF8E9AA6); River.MILK -> Color(0xFFE8B868); else -> Color(0xFFAD7A40) }
    val seam = when (r) { River.MARS -> Color(0xFF66727E); River.MILK -> Color(0xFFC48F3E); else -> Color(0xFF7A5228) }
    if (r == River.JUNGLE) {
        for (x in listOf(tl.x - w * 0.012f, tl.x + bw + w * 0.012f)) {
            drawLine(Color(0xFFD9B77A), Offset(x, tl.y - w * 0.01f), Offset(x, tl.y + bd + w * 0.01f), w * 0.008f)
        }
    }
    drawRoundRect(deck, tl, Size(bw, bd), corner)
    for (i in 1..3) drawLine(seam, Offset(tl.x, tl.y + bd * i / 4f), Offset(tl.x + bw, tl.y + bd * i / 4f), w * 0.005f)
    if (r == River.MILK) for (i in 1..4) drawLine(seam, Offset(tl.x + bw * i / 5f, tl.y), Offset(tl.x + bw * i / 5f, tl.y + bd), w * 0.005f)
    if (r == River.WINTER) drawRoundRect(Color.White, tl, Size(bw, bd * 0.3f), corner)
    drawRoundRect(Ink, tl, Size(bw, bd), corner, style = Stroke(w * 0.007f))
    val post = if (r == River.MARS) Color(0xFF55606B) else Color(0xFF5B3A1A)
    for (px in listOf(tl.x, tl.x + bw)) for (py in listOf(tl.y, tl.y + bd)) {
        drawCircle(post, w * 0.02f, Offset(px, py))
        drawCircle(Ink, w * 0.02f, Offset(px, py), style = Stroke(w * 0.005f))
    }
}

private fun DrawScope.drawWhirl(r: River, c: Offset, w: Float, t: Float) {
    val rad = w * 0.09f
    val col = if (r == River.MILK) Color(0xFFC98B3A) else Color.White
    drawCircle(Color(0xFF0A3C5A).copy(alpha = if (r == River.MILK) 0.12f else 0.25f), rad, c)
    for (i in 0..3) {
        val ri = rad * (0.25f + i * 0.24f)
        drawArc(
            col.copy(alpha = 0.9f - i * 0.15f), t * 260f + i * 80f, 210f, false,
            Offset(c.x - ri, c.y - ri), Size(ri * 2, ri * 2), style = Stroke(w * 0.01f, cap = StrokeCap.Round),
        )
    }
}

private fun starPath(c: Offset, r: Float, rotDeg: Float) = Path().apply {
    for (i in 0 until 10) {
        val a = rotDeg / 180f * PIF - PIF / 2 + i * PIF / 5
        val rr = if (i % 2 == 0) r else r * 0.48f
        val x = c.x + cos(a) * rr
        val y = c.y + sin(a) * rr
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawStar(c: Offset, r: Float, rotDeg: Float) {
    val p = starPath(c, r, rotDeg)
    drawPath(p, Color(0xFFFFD84D))
    drawPath(p, Ink, style = Stroke(r * 0.14f, join = StrokeJoin.Round))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.16f, c + Offset(-r * 0.22f, -r * 0.22f))
}

private fun DrawScope.drawBonus(k: Kind, c: Offset, r: Float, t: Float) {
    drawCircle(Color.White.copy(alpha = 0.85f), r, c)
    drawCircle(Color(0xFF7FD3F0), r, c, style = Stroke(r * 0.12f))
    drawBonusIcon(k, c, r * 0.62f, t)
    drawCircle(Color.White, r * 0.15f, c + Offset(-r * 0.5f, -r * 0.5f))
}

private fun DrawScope.drawBonusIcon(k: Kind, c: Offset, s: Float, t: Float) {
    when (k) {
        Kind.MAGNET -> {
            val red = Color(0xFFE8473C)
            val sw = s * 0.45f
            drawArc(red, 180f, 180f, false, Offset(c.x - s * 0.6f, c.y - s * 0.7f), Size(s * 1.2f, s * 1.2f), style = Stroke(sw))
            for (dx in listOf(-s * 0.6f, s * 0.6f)) {
                drawLine(red, Offset(c.x + dx, c.y - s * 0.1f), Offset(c.x + dx, c.y + s * 0.35f), sw)
                drawLine(Color(0xFFD9DDE3), Offset(c.x + dx, c.y + s * 0.35f), Offset(c.x + dx, c.y + s * 0.7f), sw)
            }
        }
        Kind.SHIELD -> {
            drawCircle(Color(0xFF9EE4FF).copy(alpha = 0.7f), s * 0.75f, c)
            drawCircle(Color.White, s * 0.75f, c, style = Stroke(s * 0.12f))
            drawArc(Color.White, 200f, 60f, false, Offset(c.x - s * 0.5f, c.y - s * 0.5f), Size(s, s), style = Stroke(s * 0.14f, cap = StrokeCap.Round))
        }
        Kind.TOW -> drawFish(c, s * 0.8f, t)
        Kind.DOUBLE -> {
            drawStar(c + Offset(-s * 0.25f, -s * 0.1f), s * 0.6f, 0f)
            drawLabel("×2", Offset(c.x + s * 0.45f, c.y + s * 0.65f), s * 0.8f, Palette.Coral)
        }
        else -> Unit
    }
}

/** Рыбка головой влево. */
private fun DrawScope.drawFish(c: Offset, s: Float, t: Float) {
    val wig = sin(t * 12f) * s * 0.15f
    val tail = Path().apply {
        moveTo(c.x + s * 0.5f, c.y)
        lineTo(c.x + s * 1.0f, c.y - s * 0.4f + wig)
        lineTo(c.x + s * 1.0f, c.y + s * 0.4f + wig)
        close()
    }
    drawPath(tail, Color(0xFFFF9A3C))
    drawPath(tail, Ink, style = Stroke(s * 0.1f, join = StrokeJoin.Round))
    val tl = Offset(c.x - s * 0.7f, c.y - s * 0.42f)
    drawOval(Color(0xFFFFB347), tl, Size(s * 1.3f, s * 0.84f))
    drawOval(Ink, tl, Size(s * 1.3f, s * 0.84f), style = Stroke(s * 0.1f))
    drawCircle(Color.White, s * 0.15f, c + Offset(-s * 0.35f, -s * 0.08f))
    drawCircle(Ink, s * 0.07f, c + Offset(-s * 0.37f, -s * 0.08f))
}

private fun DrawScope.drawSwimmer(st: SwimState, sprite: CreatureSprite?, outfit: List<WornItem>, t: Float) {
    val w = size.width
    val h = size.height
    val px = laneX(w, st.laneF)
    val py = h * PLAYER_Y
    val ch = w * 0.2f
    val pivot = Offset(px, py)
    val blink = st.hurtT > 0f && (st.hurtT * 10f).toInt() % 2 == 0
    val a = if (blink) 0.35f else 1f
    val tilt = (st.lane - st.laneF) * 25f
    if (st.towT > 0f) {
        val fc = Offset(px + sin(t * 3f) * w * 0.01f, py - ch * 1.15f)
        drawLine(Color.White, Offset(px, py - ch * 0.35f), fc + Offset(0f, ch * 0.2f), w * 0.005f)
        rotate(90f, pivot = fc) { drawFish(fc, w * 0.055f, t) }
    }
    when {
        st.jumpT >= 0f -> {
            val jh = sin(PIF * st.jumpT / JUMP_TIME)
            drawOval(Color.Black.copy(alpha = 0.2f), Offset(px - ch * 0.4f, py + ch * 0.05f), Size(ch * 0.8f, ch * 0.25f))
            rotate(tilt, pivot) {
                drawCreature(
                    sprite, Offset(px, py + ch * 0.45f - jh * ch * 0.45f), ch * (1f + 0.35f * jh), t,
                    outfit = outfit, shadow = false, alpha = a, action = Action.JUMP,
                )
            }
        }
        st.diveT >= 0f -> {
            rotate(tilt, pivot) {
                drawCreature(sprite, Offset(px, py + ch * 0.45f), ch * 0.85f, t, outfit = outfit, shadow = false, alpha = 0.3f * a, action = Action.SWIM)
            }
            for (i in 0..4) {
                val k = (t * 1.8f + i * 0.2f) % 1f
                drawCircle(
                    Color.White.copy(alpha = 0.8f * (1f - k)), w * (0.008f + 0.004f * (i % 2)),
                    Offset(px + sin(i * 2.1f + t * 4f) * ch * 0.3f, py + ch * 0.2f - k * ch * 0.6f), style = Stroke(w * 0.004f),
                )
            }
        }
        else -> {
            // по пояс в воде: рисунок виден только над линией воды
            val wl = py + ch * 0.12f
            val feet = Offset(px, py + ch * 0.5f + sin(t * 3f) * ch * 0.02f)
            val act = if (st.hurtT > HURT_TIME - 0.6f) Action.SAD else Action.SWIM
            val turn = if (st.spinT > 0f) st.spinT * 720f else tilt
            clipRect(bottom = wl) {
                rotate(turn, pivot) {
                    drawCreature(sprite, feet, ch, t, outfit = outfit, shadow = false, alpha = a, action = if (st.spinT > 0f) Action.SAD else act)
                }
            }
            // лёгкая рябь вокруг пояса
            drawOval(Color(0xFF0A3C5A).copy(alpha = 0.12f), Offset(px - ch * 0.42f, wl - ch * 0.03f), Size(ch * 0.84f, ch * 0.14f))
            for (i in 0..1) {
                val k = (t * 0.9f + i * 0.5f) % 1f
                val rw = ch * (0.55f + 0.5f * k)
                drawArc(
                    Color.White.copy(alpha = 0.5f * (1f - k)), 15f, 150f, false,
                    Offset(px - rw / 2, wl - rw * 0.1f), Size(rw, rw * 0.2f), style = Stroke(w * 0.005f, cap = StrokeCap.Round),
                )
            }
        }
    }
    if (st.shield) {
        val c = pivot - Offset(0f, ch * 0.05f)
        drawCircle(Color(0xFF9EE4FF).copy(alpha = 0.3f), ch * 0.75f, c)
        drawCircle(Color.White.copy(alpha = 0.8f), ch * 0.75f, c, style = Stroke(w * 0.006f))
        drawArc(
            Color.White, 200f, 60f, false, Offset(c.x - ch * 0.55f, c.y - ch * 0.55f), Size(ch * 1.1f, ch * 1.1f),
            style = Stroke(w * 0.01f, cap = StrokeCap.Round),
        )
    }
    if (st.magnetT > 0f) {
        val k = (t * 1.5f) % 1f
        drawCircle(Color(0xFFE8473C).copy(alpha = 0.4f * (1f - k)), ch * (0.6f + k * 0.6f), pivot, style = Stroke(w * 0.006f))
    }
}

/** Значки действующих бонусов внизу экрана, кольцо — сколько осталось. */
private fun DrawScope.drawBonusBar(st: SwimState) {
    val w = size.width
    val h = size.height
    val items = buildList {
        if (st.magnetT > 0f) add(Kind.MAGNET to st.magnetT / MAGNET_TIME)
        if (st.shield) add(Kind.SHIELD to 1f)
        if (st.towT > 0f) add(Kind.TOW to st.towT / TOW_TIME)
        if (st.doubleT > 0f) add(Kind.DOUBLE to st.doubleT / DOUBLE_TIME)
    }
    val r = w * 0.05f
    val gap = w * 0.14f
    var x = w / 2 - (items.size - 1) * gap / 2
    for ((k, f) in items) {
        val c = Offset(x, h * 0.9f)
        drawCircle(Color.White.copy(alpha = 0.9f), r, c)
        drawBonusIcon(k, c, r * 0.62f, 0f)
        drawArc(Palette.Coral, -90f, 360f * f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(w * 0.008f, cap = StrokeCap.Round))
        x += gap
    }
}
