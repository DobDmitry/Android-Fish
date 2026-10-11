package com.dobdmitry.drawnpets.ui.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.data.Creature
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.CoinBadge
import com.dobdmitry.drawnpets.ui.common.CreatureInfoCard
import com.dobdmitry.drawnpets.ui.common.EmojiButton
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Panel
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.SpeechBubble
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import com.dobdmitry.drawnpets.ui.npc.Npc
import com.dobdmitry.drawnpets.ui.npc.NpcDialog
import com.dobdmitry.drawnpets.ui.npc.drawNpc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Состояние «гуляющего» по двору персонажа (координаты — доли экрана). */
private class Walker(var x: Float, var y: Float) {
    var tx = x
    var ty = y
    var wait = Random.nextFloat() * 2f
    var facingLeft = Random.nextBoolean()
    var moving = false
    var jumpT = -1f
    var rect = Rect.Zero
}

private class SceneItem(val y: Float, val draw: DrawScope.() -> Unit)

private const val MIN_Y = 0.64f
private const val MAX_Y = 0.83f

private data class NpcSpot(val npc: Npc, val x: Float, val y: Float, val size: Float, val facingLeft: Boolean)

private val npcSpots = listOf(
    NpcSpot(Npc.OWL, 0.80f, 0.405f, 0.15f, true),
    NpcSpot(Npc.RACCOON, 0.74f, 0.585f, 0.19f, true),
    NpcSpot(Npc.HEDGEHOG, 0.42f, 0.625f, 0.15f, false),
    NpcSpot(Npc.CAT, 0.14f, 0.86f, 0.17f, false),
)

@Composable
fun WorldScreen(
    vm: GameViewModel,
    onCreate: () -> Unit,
    onPond: () -> Unit,
    onGames: () -> Unit,
    onShop: () -> Unit,
    onAlbum: () -> Unit,
    onCare: (String) -> Unit,
) {
    val state = vm.state
    val yard = state.creatures.filter { it.habitat == Habitat.YARD }
    val currentYard by rememberUpdatedState(yard)

    val walkers = remember { mutableMapOf<String, Walker>() }
    val particles = remember { mutableListOf<Particle>() }
    val npcRects = remember { mutableMapOf<Npc, Rect>() }
    var time by remember { mutableFloatStateOf(0f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var openNpc by remember { mutableStateOf<Npc?>(null) }
    val flowers = remember {
        val r = Random(42)
        List(26) { Triple(r.nextFloat(), 0.56f + r.nextFloat() * 0.34f, r.nextInt(4)) }
    }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val start = last
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                val ids = currentYard.map { it.id }.toSet()
                walkers.keys.retainAll(ids)
                for (c in currentYard) {
                    val w = walkers.getOrPut(c.id) {
                        Walker(0.15f + Random.nextFloat() * 0.7f, MIN_Y + Random.nextFloat() * (MAX_Y - MIN_Y))
                    }
                    updateWalker(w, c, dt)
                }
                particles.step(dt)
                time = (now - start) / 1e9f
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        // Сначала персонажи (верхние в отрисовке — последние)
                        val hit = currentYard
                            .mapNotNull { c -> walkers[c.id]?.let { c to it } }
                            .sortedByDescending { it.second.y }
                            .firstOrNull { it.second.rect.inflate(12f).contains(pos) }
                        if (hit != null) {
                            val (c, w) = hit
                            w.jumpT = 0f
                            selectedId = c.id
                            if (!c.sleeping) vm.pet(c.id)
                            particles.burst(pos.x, w.rect.top, if (c.sleeping) "💤" else "💖", 4, 40f)
                            return@detectTapGestures
                        }
                        val npc = npcRects.entries.firstOrNull { it.value.contains(pos) }?.key
                        if (npc != null) {
                            openNpc = npc
                            return@detectTapGestures
                        }
                        selectedId = null
                    }
                }
        ) {
            val t = time
            drawMeadow(t, flowers)
            // Все объекты сцены сортируем по глубине (y), чтобы ближние перекрывали дальних.
            val items = mutableListOf<SceneItem>()
            for (spot in npcSpots) {
                items += SceneItem(spot.y) {
                    val rect = drawNpc(
                        spot.npc, Offset(size.width * spot.x, size.height * spot.y),
                        size.width * spot.size, t, spot.facingLeft,
                    )
                    npcRects[spot.npc] = rect
                    if (spot.npc == Npc.RACCOON) drawStallCounter(t)
                }
            }
            for (c in currentYard) {
                val w = walkers[c.id] ?: continue
                items += SceneItem(w.y) {
                    val depth = 0.75f + (w.y - MIN_Y) / (MAX_Y - MIN_Y) * 0.35f
                    val h = size.width * 0.26f * depth
                    val jump = if (w.jumpT >= 0f) sin(PI.toFloat() * w.jumpT / 0.5f) * h * 0.45f else 0f
                    val feet = Offset(size.width * w.x, size.height * w.y)
                    w.rect = drawCreature(
                        image = vm.bitmap(c.id), feet = feet, height = h, time = t,
                        seed = c.id.hashCode(), facingLeft = w.facingLeft, moving = w.moving,
                        sleeping = c.sleeping, sad = c.mood < 30, accessory = c.accessory, jump = jump,
                    )
                    val labelColor = if (c.id == selectedId) Palette.Coral else Palette.Ink
                    drawLabel(c.name, Offset(feet.x, feet.y + 34f), 34f, labelColor)
                    c.wish()?.let { if (!c.sleeping && (t + c.id.hashCode() % 7) % 7f < 2.5f) drawEmoji("💭", Offset(w.rect.right, w.rect.top), 44f) }
                }
            }
            items.sortedBy { it.y }.forEach { it.draw(this) }
            drawParticles(particles)
        }

        // Верхняя панель
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Живые рисунки",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink,
                modifier = Modifier.weight(1f),
            )
            CoinBadge(state.coins)
        }

        if (state.creatures.size <= 2 && selectedId == null && openNpc == null) {
            SpeechBubble(
                "🦉 Нарисуй персонажа на бумаге и нажми «Рисунок» — он оживёт!",
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            val selected = selectedId?.let { state.creature(it) }
            if (selected != null) {
                CreatureInfoCard(selected, vm.bitmap(selected.id), onCare = { onCare(selected.id) }, onClose = { selectedId = null })
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                EmojiButton("📷", "Рисунок", color = Palette.Lemon, onClick = onCreate)
                EmojiButton("🏞️", "Пруд", color = Palette.SkyLight, onClick = onPond)
                EmojiButton("🎮", "Игры", color = Color(0xFFFFD6E5), onClick = onGames)
                EmojiButton("🏪", "Лавка", color = Color(0xFFD8F5E6), onClick = onShop)
                EmojiButton("📖", "Альбом", color = Color(0xFFEDE1FF), onClick = onAlbum)
            }
        }

        openNpc?.let { npc ->
            NpcDialog(
                npc = npc, vm = vm, onClose = { openNpc = null },
                onOpenShop = onShop, onOpenGames = onGames, onFeedFish = onPond,
            )
        }
    }
}

private fun updateWalker(w: Walker, c: Creature, dt: Float) {
    if (w.jumpT >= 0f) {
        w.jumpT += dt
        if (w.jumpT > 0.5f) w.jumpT = -1f
    }
    if (c.sleeping) {
        w.moving = false
        return
    }
    if (w.wait > 0f) {
        w.wait -= dt
        w.moving = false
        if (w.wait <= 0f) {
            w.tx = 0.1f + Random.nextFloat() * 0.8f
            w.ty = MIN_Y + Random.nextFloat() * (MAX_Y - MIN_Y)
        }
        return
    }
    val dx = w.tx - w.x
    val dy = (w.ty - w.y) * 0.6f
    val dist = hypot(dx, dy)
    val tired = c.energy < 25f
    val speed = if (tired) 0.035f else 0.08f
    if (dist < 0.01f) {
        w.wait = 1.5f + Random.nextFloat() * 4f
        w.moving = false
        return
    }
    w.moving = true
    w.x += dx / dist * speed * dt
    w.y += dy / dist * speed * dt
    if (abs(dx) > 0.005f) w.facingLeft = dx < 0
}

// ------------------------------------------------------------------ фон

private fun DrawScope.drawMeadow(t: Float, flowers: List<Triple<Float, Float, Int>>) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.5f
    drawRect(Brush.verticalGradient(listOf(Palette.Sky, Palette.SkyLight), 0f, horizon), size = Size(w, horizon + 2))
    // солнце
    val sun = Offset(w * 0.15f, h * 0.13f)
    rotate(t * 12f, pivot = sun) {
        for (i in 0 until 12) {
            rotate(i * 30f, pivot = sun) {
                drawLine(Palette.Sun, Offset(sun.x, sun.y - w * 0.1f), Offset(sun.x, sun.y - w * 0.14f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
        }
    }
    drawCircle(Palette.Sun, w * 0.08f, sun)
    drawCircle(Color(0xFFFFB347), w * 0.08f, sun, style = Stroke(5f))
    // облака
    for (i in 0 until 3) {
        val speed = 12f + i * 7f
        val span = w + 300f
        val cx = ((i * 270f + t * speed) % span) - 150f
        val cy = h * (0.12f + i * 0.08f)
        val r = w * (0.06f + i * 0.01f)
        drawCircle(Color.White, r, Offset(cx, cy))
        drawCircle(Color.White, r * 0.8f, Offset(cx - r, cy + r * 0.2f))
        drawCircle(Color.White, r * 0.85f, Offset(cx + r, cy + r * 0.15f))
    }
    // холмы
    drawOval(Color(0xFF8ED46A), Offset(-w * 0.3f, horizon - h * 0.08f), Size(w * 0.9f, h * 0.25f))
    drawOval(Color(0xFF7BCB5C), Offset(w * 0.35f, horizon - h * 0.06f), Size(w * 1.0f, h * 0.25f))
    // трава
    drawRect(
        Brush.verticalGradient(listOf(Palette.Grass, Palette.GrassDark), horizon, h),
        topLeft = Offset(0f, horizon), size = Size(w, h - horizon),
    )
    // дорожка
    val path = Path().apply {
        moveTo(w * 0.22f, horizon + h * 0.06f)
        cubicTo(w * 0.35f, h * 0.7f, w * 0.1f, h * 0.8f, w * 0.45f, h)
        lineTo(w * 0.75f, h)
        cubicTo(w * 0.35f, h * 0.8f, w * 0.6f, h * 0.68f, w * 0.3f, horizon + h * 0.06f)
        close()
    }
    drawPath(path, Color(0xFFE9D3A0))
    // цветочки
    val colors = listOf(Color(0xFFFF8FB8), Color(0xFFFFE27A), Color.White, Color(0xFFB59CFF))
    for ((fx, fy, ci) in flowers) {
        val c = Offset(fx * w, fy * h)
        val sway = sin(t * 2f + fx * 10f) * 3f
        drawLine(Color(0xFF3E8E2F), c, Offset(c.x + sway, c.y - 18f), strokeWidth = 3f)
        for (k in 0 until 5) {
            val a = k * 2 * PI / 5
            drawCircle(colors[ci], 6f, Offset(c.x + sway + (kotlin.math.cos(a) * 7).toFloat(), c.y - 18f + (kotlin.math.sin(a) * 7).toFloat()))
        }
        drawCircle(Color(0xFFFFB347), 4f, Offset(c.x + sway, c.y - 18f))
    }
    drawHouse(t)
    drawTree(t)
    drawStall()
}

private fun DrawScope.drawHouse(t: Float) {
    val w = size.width
    val h = size.height
    val left = w * 0.04f
    val right = w * 0.36f
    val base = h * 0.56f
    val top = h * 0.44f
    val ink = Palette.Ink
    // труба и дым
    drawRect(Color(0xFFB5523B), Offset(w * 0.27f, h * 0.36f), Size(w * 0.04f, h * 0.06f))
    for (i in 0 until 3) {
        val k = (t * 0.4f + i / 3f) % 1f
        drawCircle(Color.White.copy(alpha = 0.7f * (1f - k)), 10f + k * 18f, Offset(w * 0.29f + k * 20f, h * 0.35f - k * h * 0.08f))
    }
    drawRect(Color(0xFFFFE9C7), Offset(left, top), Size(right - left, base - top))
    drawRect(ink, Offset(left, top), Size(right - left, base - top), style = Stroke(4f))
    val roof = Path().apply {
        moveTo(left - w * 0.03f, top); lineTo((left + right) / 2, h * 0.36f); lineTo(right + w * 0.03f, top); close()
    }
    drawPath(roof, Color(0xFFE85D4A))
    drawPath(roof, ink, style = Stroke(4f))
    // дверь и окно
    drawRect(Color(0xFF9C6B45), Offset(left + w * 0.04f, base - h * 0.075f), Size(w * 0.07f, h * 0.075f))
    drawRect(ink, Offset(left + w * 0.04f, base - h * 0.075f), Size(w * 0.07f, h * 0.075f), style = Stroke(3f))
    drawCircle(Palette.Sun, 4f, Offset(left + w * 0.095f, base - h * 0.04f))
    val win = Offset(left + w * 0.17f, top + h * 0.025f)
    drawRect(Color(0xFFBDE6FF), win, Size(w * 0.09f, h * 0.05f))
    drawRect(ink, win, Size(w * 0.09f, h * 0.05f), style = Stroke(3f))
    drawLine(ink, Offset(win.x + w * 0.045f, win.y), Offset(win.x + w * 0.045f, win.y + h * 0.05f), strokeWidth = 3f)
}

private fun DrawScope.drawTree(t: Float) {
    val w = size.width
    val h = size.height
    val x = w * 0.9f
    drawRect(Color(0xFF8A5A3B), Offset(x - w * 0.03f, h * 0.33f), Size(w * 0.06f, h * 0.2f))
    // ветка для Филина
    drawLine(Color(0xFF8A5A3B), Offset(x, h * 0.41f), Offset(w * 0.72f, h * 0.405f), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
    val sway = sin(t * 1.2f) * 4f
    val crown = Color(0xFF3FA34D)
    drawCircle(crown, w * 0.13f, Offset(x + sway, h * 0.25f))
    drawCircle(Color(0xFF4DB85A), w * 0.1f, Offset(x - w * 0.09f + sway, h * 0.29f))
    drawCircle(Color(0xFF4DB85A), w * 0.09f, Offset(x + w * 0.06f + sway, h * 0.31f))
    for (i in 0 until 4) drawCircle(Color(0xFFFF5C5C), w * 0.014f, Offset(x - w * 0.06f + i * w * 0.04f + sway, h * (0.24f + (i % 2) * 0.05f)))
}

private fun DrawScope.drawStall() {
    val w = size.width
    val h = size.height
    val left = w * 0.6f
    val right = w * 0.88f
    // стойки
    drawRect(Color(0xFF9C6B45), Offset(left, h * 0.46f), Size(w * 0.015f, h * 0.12f))
    drawRect(Color(0xFF9C6B45), Offset(right - w * 0.015f, h * 0.46f), Size(w * 0.015f, h * 0.12f))
    // полосатый навес
    val stripes = 6
    val sw = (right - left + w * 0.04f) / stripes
    for (i in 0 until stripes) {
        drawRect(
            if (i % 2 == 0) Color(0xFFFF6B6B) else Color.White,
            Offset(left - w * 0.02f + i * sw, h * 0.44f), Size(sw, h * 0.03f),
        )
    }
    drawRect(Palette.Ink, Offset(left - w * 0.02f, h * 0.44f), Size(right - left + w * 0.04f, h * 0.03f), style = Stroke(3f))
}

private fun DrawScope.drawStallCounter(t: Float) {
    val w = size.width
    val h = size.height
    val left = w * 0.6f
    val right = w * 0.88f
    drawRect(Color(0xFFC98B55), Offset(left, h * 0.545f), Size(right - left, h * 0.045f))
    drawRect(Palette.Ink, Offset(left, h * 0.545f), Size(right - left, h * 0.045f), style = Stroke(3f))
    val goods = listOf("🍎", "🥕", "🍪", "🎩")
    goods.forEachIndexed { i, e ->
        drawEmoji(e, Offset(left + (i + 0.5f) * (right - left) / goods.size, h * 0.54f), w * 0.05f)
    }
    drawLabel("ЛАВКА", Offset((left + right) / 2, h * 0.575f), w * 0.035f, Palette.Ink)
}
