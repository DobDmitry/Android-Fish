package com.dobdmitry.drawnpets.ui.outfit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.anim.OutfitLayout
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.Accessory
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Slot
import com.dobdmitry.drawnpets.data.WornItem
import com.dobdmitry.drawnpets.ui.common.CoinBadge
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmojiAt
import com.dobdmitry.drawnpets.ui.common.rememberFrameTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val previewActions = listOf(Action.WALK, Action.HAPPY, Action.WAVE, Action.IDLE)

/**
 * Примерочная: надеть наряды и подогнать их под рисунок — перетащить пальцем,
 * двумя пальцами изменить размер и повернуть (или кнопками).
 */
@Composable
fun OutfitScreen(vm: GameViewModel, creatureId: String, onBack: () -> Unit) {
    val creature = vm.state.creature(creatureId) ?: return
    val sprite = vm.sprite(creatureId) ?: return
    val bitmap = sprite.bitmap
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    val rig = sprite.rig

    var items by remember { mutableStateOf(creature.outfit) }
    var selected by remember { mutableStateOf<String?>(items.lastOrNull()?.id) }
    var slot by remember { mutableStateOf(Slot.HEAD) }
    var preview by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val time by rememberFrameTime()

    /** Делаем место наряда «своим», чтобы его можно было двигать. */
    fun materialize(w: WornItem): WornItem {
        if (w.custom) return w
        val p = OutfitLayout.placement(w, rig) ?: return w
        return w.copy(x = p.x, y = p.y, size = p.size, rot = p.rot)
    }
    fun editSelected(block: (WornItem) -> WornItem) {
        val id = selected ?: return
        items = items.map { if (it.id == id) block(materialize(it)) else it }
    }

    class Fit(val scale: Float, val ox: Float, val oy: Float) {
        fun toScreen(x: Float, y: Float) = Offset(ox + x * scale, oy + y * scale)
    }
    fun fit(w: Float, h: Float): Fit {
        val s = min(w / bw, h / bh) * 0.72f
        return Fit(s, (w - bw * s) / 2, (h - bh * s) / 2 + h * 0.04f)
    }

    fun tapItem(acc: Accessory) {
        val worn = items.any { it.id == acc.id }
        when {
            worn -> {
                selected = acc.id
            }
            acc.minLevel > creature.level -> {
                message = "${acc.emoji} можно надеть с ${acc.minLevel}-го уровня. У ${creature.name} сейчас ${creature.level}-й."
                Sounds.play(Sfx.WRONG)
            }
            acc.id !in vm.state.ownedAccessories -> {
                if (vm.buyAccessory(acc.id)) {
                    Sounds.play(Sfx.COIN)
                    items = items.filter { Catalog.accessory(it.id)?.slot != acc.slot } + WornItem(acc.id)
                    selected = acc.id
                    message = "Куплено и надето: ${acc.name}!"
                } else {
                    Sounds.play(Sfx.WRONG)
                    message = "Не хватает монеток: нужно ${acc.price} 🪙."
                }
            }
            else -> {
                // на одно место — один наряд
                items = items.filter { Catalog.accessory(it.id)?.slot != acc.slot } + WornItem(acc.id)
                selected = acc.id
                Sounds.play(Sfx.HAPPY)
                message = null
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF4FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader("Наряды: ${creature.name}", vm.state.coins, onBack)
        Text(
            message ?: if (preview) "Так ${creature.name} выглядит в движении."
            else if (selected != null) "Двигай наряд пальцем, двумя пальцами — меняй размер и поворачивай."
            else "Выбери наряд внизу. Уровень ${creature.name}: ${creature.level}.",
            fontSize = 14.sp, color = Palette.Ink, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(8.dp),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFF7FF))
                .border(2.dp, Palette.Ink.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(preview) {
                        if (preview) return@pointerInput
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val f = fit(size.width.toFloat(), size.height.toFloat())
                            editSelected { w ->
                                w.copy(
                                    x = ((w.x ?: 0f) + pan.x / f.scale).coerceIn(-bw * 0.3f, bw * 1.3f),
                                    y = ((w.y ?: 0f) + pan.y / f.scale).coerceIn(-bh * 0.3f, bh * 1.3f),
                                    size = ((w.size ?: 50f) * zoom).coerceIn(bw * 0.05f, bw * 1.6f),
                                    rot = w.rot + rotation,
                                )
                            }
                        }
                    }
                    .pointerInput(preview) {
                        detectTapGestures { pos ->
                            if (preview) return@detectTapGestures
                            val f = fit(size.width.toFloat(), size.height.toFloat())
                            // выбираем наряд, на который нажали (ближайший)
                            val hit = items.mapNotNull { w ->
                                val p = OutfitLayout.placement(w, rig) ?: return@mapNotNull null
                                val c = f.toScreen(p.x, p.y)
                                val d = (c - pos).getDistance()
                                if (d < max(p.size * f.scale * 0.6f, 28.dp.toPx())) w.id to d else null
                            }.minByOrNull { it.second }?.first
                            selected = hit
                            if (hit != null) Sounds.play(Sfx.TAP)
                        }
                    }
            ) {
                val f = fit(size.width, size.height)
                if (preview) {
                    val act = previewActions[((time / 2.4f).toInt()) % previewActions.size]
                    drawCreature(
                        sprite = sprite, feet = Offset(size.width / 2, f.oy + bh * f.scale), height = bh * f.scale,
                        time = time, seed = creature.id.hashCode(), action = act, actionTime = time % 2.4f, outfit = items,
                    )
                    return@Canvas
                }
                val canvas = drawContext.canvas.nativeCanvas
                fun drawItem(w: WornItem) {
                    val acc = Catalog.accessory(w.id) ?: return
                    val p = OutfitLayout.placement(w, rig) ?: return
                    val c = f.toScreen(p.x, p.y)
                    drawEmojiAt(canvas, acc.emoji, c.x, c.y, p.size * f.scale, p.rot)
                }
                items.filter { Catalog.accessory(it.id)?.slot == Slot.BACK }.forEach { drawItem(it) }
                drawImage(
                    sprite.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset(f.ox.roundToInt(), f.oy.roundToInt()),
                    dstSize = IntSize((bw * f.scale).roundToInt(), (bh * f.scale).roundToInt()),
                )
                items.filter { Catalog.accessory(it.id)?.slot != Slot.BACK }.forEach { drawItem(it) }
                // рамка вокруг выбранного наряда
                items.firstOrNull { it.id == selected }?.let { w ->
                    val p = OutfitLayout.placement(w, rig) ?: return@let
                    val c = f.toScreen(p.x, p.y)
                    val half = p.size * f.scale * 0.55f
                    rotate(p.rot, pivot = c) {
                        drawRoundRect(
                            Palette.Coral,
                            topLeft = Offset(c.x - half, c.y - half),
                            size = androidx.compose.ui.geometry.Size(half * 2, half * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                            style = Stroke(3.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                        )
                    }
                }
            }
        }

        // Кнопки для выбранного наряда
        val sel = selected
        if (sel != null && !preview) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SmallBtn("−") { editSelected { it.copy(size = (it.size ?: 50f) / 1.12f) } }
                SmallBtn("+") { editSelected { it.copy(size = (it.size ?: 50f) * 1.12f) } }
                SmallBtn("⟲") { editSelected { it.copy(rot = it.rot - 10f) } }
                SmallBtn("⟳") { editSelected { it.copy(rot = it.rot + 10f) } }
                SmallBtn("↺ На место") { items = items.map { if (it.id == sel) WornItem(it.id) else it } }
                SmallBtn("🗑️ Снять") {
                    items = items.filterNot { it.id == sel }
                    selected = null
                }
            }
        }

        // Места для нарядов
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Slot.entries.forEach { s ->
                val on = s == slot
                val worn = items.firstOrNull { Catalog.accessory(it.id)?.slot == s }
                Text(
                    "${worn?.let { Catalog.accessory(it.id)?.emoji } ?: s.emoji} ${s.title}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Palette.Lemon else Color.White)
                        .border(2.dp, Palette.Ink.copy(alpha = if (on) 0.8f else 0.25f), RoundedCornerShape(50))
                        .clickable { slot = s; Sounds.play(Sfx.TAP) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        // Наряды выбранного места
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Catalog.accessories.filter { it.slot == slot }.sortedBy { it.minLevel }.forEach { acc ->
                val worn = items.any { it.id == acc.id }
                val owned = acc.id in vm.state.ownedAccessories
                val locked = acc.minLevel > creature.level
                Column(
                    Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                worn -> Palette.Mint
                                locked -> Color(0xFFEDEDED)
                                else -> Color.White
                            }
                        )
                        .border(2.dp, Palette.Ink.copy(alpha = if (worn) 0.9f else 0.25f), RoundedCornerShape(16.dp))
                        .clickable { tapItem(acc) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(acc.emoji, fontSize = 30.sp, modifier = Modifier.padding(2.dp))
                        if (locked) Text("🔒", fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Text(
                        when {
                            locked -> "ур. ${acc.minLevel}"
                            owned -> if (worn) "надето" else "есть"
                            else -> "🪙 ${acc.price}"
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink,
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton(
                if (preview) "✏️ Подогнать" else "▶️ Посмотреть",
                Modifier.weight(1f), color = Palette.Mint, textColor = Palette.Ink,
            ) { preview = !preview }
            PillButton("✅ Готово", Modifier.weight(1f)) {
                vm.setOutfit(creatureId, items)
                Sounds.play(Sfx.MAGIC)
                onBack()
            }
        }
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(2.dp, Palette.Ink.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable { Sounds.play(Sfx.TAP); onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
