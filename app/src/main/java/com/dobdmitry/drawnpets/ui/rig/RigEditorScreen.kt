package com.dobdmitry.drawnpets.ui.rig

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.anim.LimbKind
import com.dobdmitry.drawnpets.anim.ManualEye
import com.dobdmitry.drawnpets.anim.ManualLimb
import com.dobdmitry.drawnpets.anim.ManualRig
import com.dobdmitry.drawnpets.anim.RigAnalyzer
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.rememberFrameTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Tool(val emoji: String, val title: String, val hint: String, val kind: LimbKind?, val color: Color) {
    LEG("🦵", "Нога", "Проведи пальцем от бедра до ступни", LimbKind.LEG, Color(0xFFE53935)),
    ARM("💪", "Рука", "Проведи пальцем от плеча до кончика руки, лапы или плавника", LimbKind.ARM, Color(0xFF2E9E3E)),
    ANTENNA("🐌", "Рожка", "Рожки, ушки, хвост: проведи пальцем от основания до кончика", LimbKind.ANTENNA, Color(0xFFFF9800)),
    EYE("👁️", "Глаз", "Нажми на середину глаза. Размер — кнопками − и +", null, Color(0xFFD81BDB)),
}

private fun colorOf(kind: LimbKind) = when (kind) {
    LimbKind.LEG -> Tool.LEG.color
    LimbKind.ARM -> Tool.ARM.color
    LimbKind.ANTENNA -> Tool.ANTENNA.color
}

/** Что выделено: конечность или глаз (по номеру в списке). */
private sealed interface Sel {
    data class Limb(val i: Int) : Sel
    data class Eye(val i: Int) : Sel
}

/** Что сейчас тащим пальцем: handle 0 — крепление (или центр глаза), 1 — кончик. */
private data class DragTarget(val sel: Sel, val handle: Int, val creating: Boolean)

private val previewActions = listOf(
    Action.WALK to "Идёт", Action.HAPPY to "Радуется", Action.WAVE to "Машет",
    Action.IDLE to "Стоит и моргает", Action.EAT to "Ест", Action.SWIM to "Плывёт",
)

/**
 * Ручная разметка «скелета»: где у рисунка руки, ноги, рожки и глаза.
 * Начинаем с того, что нашла игра; всё можно поправить, удалить или добавить.
 */
@Composable
fun RigEditorScreen(vm: GameViewModel, creatureId: String, onBack: () -> Unit) {
    val sprite = vm.sprite(creatureId) ?: return
    val creature = vm.state.creature(creatureId) ?: return
    val bitmap = sprite.bitmap
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    val scope = rememberCoroutineScope()

    val initial = remember { sprite.manual ?: RigAnalyzer.toManual(sprite.rig) }
    var limbs by remember { mutableStateOf(initial.limbs) }
    var eyes by remember { mutableStateOf(initial.eyes) }
    var autoMode by remember { mutableStateOf(sprite.manual == null) }
    var edited by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.LEG) }
    var selected by remember { mutableStateOf<Sel?>(null) }
    var preview by remember { mutableStateOf(false) }
    var previewSprite by remember { mutableStateOf<CreatureSprite?>(null) }
    val drag = remember { arrayOfNulls<DragTarget>(1) }
    val time by rememberFrameTime()

    fun changed() { edited = true; autoMode = false }

    LaunchedEffect(limbs, eyes, preview) {
        if (preview) {
            delay(100)
            previewSprite = withContext(Dispatchers.Default) { CreatureSprite.create(bitmap, ManualRig(limbs, eyes)) }
        }
    }

    // Геометрия: картинка вписана в область с полями
    class Fit(val scale: Float, val ox: Float, val oy: Float) {
        fun toScreen(x: Float, y: Float) = Offset(ox + x * scale, oy + y * scale)
        fun toImage(p: Offset) = Offset(((p.x - ox) / scale).coerceIn(0f, bw), ((p.y - oy) / scale).coerceIn(0f, bh))
    }
    fun fit(w: Float, h: Float): Fit {
        val s = min(w / bw, h / bh) * 0.9f
        return Fit(s, (w - bw * s) / 2, (h - bh * s) / 2)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader("Движения: ${creature.name}", null, onBack)
        Text(
            when {
                preview -> "Так ${creature.name} будет двигаться. Нажми «Править», чтобы поменять разметку."
                selected != null -> "Перетащи кружочки пальцем или удали. Можно добавить ещё — выбери инструмент внизу."
                else -> tool.hint
            },
            fontSize = 15.sp, color = Palette.Ink, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(10.dp),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFF7FF))
                .border(2.dp, Palette.Ink.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(tool, preview) {
                        if (preview) return@pointerInput
                        val touch = 30.dp.toPx()
                        fun hitHandle(pos: Offset): DragTarget? {
                            val f = fit(size.width.toFloat(), size.height.toFloat())
                            var best: DragTarget? = null
                            var bestD = touch
                            limbs.forEachIndexed { i, l ->
                                val d0 = (f.toScreen(l.ax, l.ay) - pos).getDistance()
                                val d1 = (f.toScreen(l.tx, l.ty) - pos).getDistance()
                                if (d0 < bestD) { bestD = d0; best = DragTarget(Sel.Limb(i), 0, false) }
                                if (d1 < bestD) { bestD = d1; best = DragTarget(Sel.Limb(i), 1, false) }
                            }
                            eyes.forEachIndexed { i, e ->
                                val d = (f.toScreen(e.cx, e.cy) - pos).getDistance()
                                if (d < bestD) { bestD = d; best = DragTarget(Sel.Eye(i), 0, false) }
                            }
                            return best
                        }
                        detectDragGestures(
                            onDragStart = { pos ->
                                val f = fit(size.width.toFloat(), size.height.toFloat())
                                val hit = hitHandle(pos)
                                val kind = tool.kind
                                if (hit != null) {
                                    drag[0] = hit
                                    selected = hit.sel
                                } else if (kind != null) {
                                    val p = f.toImage(pos)
                                    limbs = limbs + ManualLimb(kind, p.x, p.y, p.x, p.y)
                                    drag[0] = DragTarget(Sel.Limb(limbs.lastIndex), 1, true)
                                    selected = Sel.Limb(limbs.lastIndex)
                                    changed()
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val d = drag[0] ?: return@detectDragGestures
                                val f = fit(size.width.toFloat(), size.height.toFloat())
                                val p = f.toImage(change.position)
                                when (val s = d.sel) {
                                    is Sel.Limb -> limbs = limbs.mapIndexed { i, l ->
                                        if (i != s.i) l else if (d.handle == 0) l.copy(ax = p.x, ay = p.y) else l.copy(tx = p.x, ty = p.y)
                                    }
                                    is Sel.Eye -> eyes = eyes.mapIndexed { i, e -> if (i != s.i) e else e.copy(cx = p.x, cy = p.y) }
                                }
                                changed()
                            },
                            onDragEnd = {
                                val d = drag[0]
                                if (d != null && d.creating) {
                                    val s = d.sel as Sel.Limb
                                    val l = limbs.getOrNull(s.i)
                                    if (l != null && hypot(l.tx - l.ax, l.ty - l.ay) < max(bw, bh) * 0.04f) {
                                        // слишком короткая линия — это была случайность
                                        limbs = limbs.filterIndexed { i, _ -> i != s.i }
                                        selected = null
                                    } else {
                                        Sounds.play(Sfx.TAP)
                                    }
                                }
                                drag[0] = null
                            },
                            onDragCancel = { drag[0] = null },
                        )
                    }
                    .pointerInput(tool, preview) {
                        detectTapGestures { pos ->
                            if (preview) return@detectTapGestures
                            val f = fit(size.width.toFloat(), size.height.toFloat())
                            val touch = 30.dp.toPx()
                            // Нажали на существующую линию или глаз — выделяем
                            var hit: Sel? = null
                            var bestD = touch
                            limbs.forEachIndexed { i, l ->
                                val a = f.toScreen(l.ax, l.ay); val b = f.toScreen(l.tx, l.ty)
                                val d = distToSegment(pos, a, b)
                                if (d < bestD) { bestD = d; hit = Sel.Limb(i) }
                            }
                            eyes.forEachIndexed { i, e ->
                                val d = (f.toScreen(e.cx, e.cy) - pos).getDistance() - e.r * f.scale
                                if (d < bestD) { bestD = d; hit = Sel.Eye(i) }
                            }
                            when {
                                hit != null -> { selected = hit; Sounds.play(Sfx.TAP) }
                                tool == Tool.EYE -> {
                                    val p = f.toImage(pos)
                                    val r = eyes.lastOrNull()?.r ?: max(6f, min(bw, bh) * 0.05f)
                                    eyes = eyes + ManualEye(p.x, p.y, r)
                                    selected = Sel.Eye(eyes.lastIndex)
                                    changed()
                                    Sounds.play(Sfx.TAP)
                                }
                                else -> selected = null
                            }
                        }
                    }
            ) {
                val f = fit(size.width, size.height)
                if (preview) {
                    val ps = previewSprite
                    val (act, label) = previewActions[((time / 2.4f).toInt()) % previewActions.size]
                    drawCreature(
                        sprite = ps, feet = Offset(size.width / 2, f.oy + bh * f.scale), height = bh * f.scale,
                        time = time, seed = 3, action = act, actionTime = time % 2.4f,
                        accessory = creature.accessory, swimming = act == Action.SWIM,
                    )
                    drawLabel(label, Offset(size.width / 2, 34.dp.toPx()), 20.dp.toPx(), Palette.Coral)
                    return@Canvas
                }
                drawImage(
                    sprite.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset(f.ox.roundToInt(), f.oy.roundToInt()),
                    dstSize = IntSize((bw * f.scale).roundToInt(), (bh * f.scale).roundToInt()),
                )
                val line = 6.dp.toPx()
                limbs.forEachIndexed { i, l ->
                    val a = f.toScreen(l.ax, l.ay); val b = f.toScreen(l.tx, l.ty)
                    val c = colorOf(l.kind)
                    val sel = selected == Sel.Limb(i)
                    if (sel) drawLine(Palette.Lemon, a, b, strokeWidth = line * 2.2f, cap = StrokeCap.Round)
                    drawLine(Color.White, a, b, strokeWidth = line * 1.6f, cap = StrokeCap.Round)
                    drawLine(c, a, b, strokeWidth = line, cap = StrokeCap.Round)
                    drawCircle(Color.White, 11.dp.toPx(), a)
                    drawCircle(c, 9.dp.toPx(), a)
                    drawCircle(Color.White, 8.dp.toPx(), b)
                    drawCircle(c, 8.dp.toPx(), b, style = Stroke(3.dp.toPx()))
                }
                eyes.forEachIndexed { i, e ->
                    val c = f.toScreen(e.cx, e.cy)
                    val sel = selected == Sel.Eye(i)
                    if (sel) drawCircle(Palette.Lemon, e.r * f.scale + 4.dp.toPx(), c, style = Stroke(6.dp.toPx()))
                    drawCircle(Color.White, e.r * f.scale, c, style = Stroke(5.dp.toPx()))
                    drawCircle(Tool.EYE.color, e.r * f.scale, c, style = Stroke(3.dp.toPx()))
                    drawCircle(Tool.EYE.color, 4.dp.toPx(), c)
                }
            }
        }

        // Действия с выделенным
        val sel = selected
        if (sel != null && !preview) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                PillButton("🗑️ Удалить", color = Color.White, textColor = Palette.Ink) {
                    when (sel) {
                        is Sel.Limb -> limbs = limbs.filterIndexed { i, _ -> i != sel.i }
                        is Sel.Eye -> eyes = eyes.filterIndexed { i, _ -> i != sel.i }
                    }
                    selected = null
                    changed()
                }
                if (sel is Sel.Eye) {
                    PillButton("−", color = Color.White, textColor = Palette.Ink) {
                        eyes = eyes.mapIndexed { i, e -> if (i == sel.i) e.copy(r = max(3f, e.r / 1.15f)) else e }
                        changed()
                    }
                    PillButton("+", color = Color.White, textColor = Palette.Ink) {
                        eyes = eyes.mapIndexed { i, e -> if (i == sel.i) e.copy(r = min(bw / 3, e.r * 1.15f)) else e }
                        changed()
                    }
                }
            }
        }

        // Инструменты
        if (!preview) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tool.entries.forEach { t ->
                    val on = t == tool
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) t.color.copy(alpha = 0.25f) else Color.White)
                            .border(2.dp, if (on) t.color else Palette.Ink.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .clickable { tool = t; selected = null; Sounds.play(Sfx.TAP) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.emoji, fontSize = 24.sp)
                        Text(t.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                if (preview) "✏️ Править" else "▶️ Проверить",
                Modifier.weight(1f),
                color = Palette.Mint, textColor = Palette.Ink,
            ) {
                preview = !preview
                selected = null
            }
            PillButton("↺ Авто", Modifier.weight(1f), color = Color.White, textColor = Palette.Ink) {
                scope.launch {
                    val m = withContext(Dispatchers.Default) { CreatureSprite.autoManual(bitmap) }
                    limbs = m.limbs
                    eyes = m.eyes
                    selected = null
                    autoMode = true
                    edited = false
                }
            }
            PillButton("✅ Готово", Modifier.weight(1f)) {
                vm.setManualRig(creatureId, if (autoMode && !edited) null else ManualRig(limbs, eyes))
                Sounds.play(Sfx.MAGIC)
                onBack()
            }
        }
        Box(Modifier.height(4.dp))
    }
}

private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 < 1f) return (p - a).getDistance()
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - Offset(a.x + ab.x * t, a.y + ab.y * t)).getDistance()
}
