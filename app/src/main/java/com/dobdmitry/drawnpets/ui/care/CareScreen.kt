package com.dobdmitry.drawnpets.ui.care

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import com.dobdmitry.drawnpets.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.FeedResult
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private enum class Mode { NONE, FEED, WASH }

/** Еда, летящая ко рту персонажа. */
private class FlyingFood(val foodId: String, val emoji: String, var t: Float = 0f)

private val happyPhrases = listOf(
    "Я тебя люблю!", "Ура, ты пришёл!", "Мне так хорошо!", "Давай играть!",
    "Я самый счастливый рисунок!", "Хи-хи, щекотно!", "Ты мой лучший друг!",
)

@Composable
fun CareScreen(
    vm: GameViewModel,
    creatureId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onRig: (String) -> Unit,
    onOutfit: (String) -> Unit,
) {
    val creature = vm.state.creature(creatureId)
    // Персонажа отпустили — экран сейчас закрывается, рисовать нечего.
    if (creature == null) return
    val c by rememberUpdatedState(creature)
    val sprite = vm.sprite(creatureId)

    var mode by remember { mutableStateOf(Mode.NONE) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var showMore by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showRelease by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    val particles = remember { mutableListOf<Particle>() }
    val flying = remember { mutableListOf<FlyingFood>() }
    val creatureRect = remember { arrayOf(Rect.Zero) }
    var time by remember { mutableFloatStateOf(0f) }
    var rubbed by remember { mutableFloatStateOf(0f) }
    // Текущая эмоция поверх обычной позы и сколько она уже длится
    val emote = remember { arrayOf<Action?>(null) }
    val emoteT = remember { floatArrayOf(0f) }
    val lastRub = remember { floatArrayOf(-10f) }
    fun startEmote(a: Action) { emote[0] = a; emoteT[0] = 0f }
    val voicePitch = 1.35f + (kotlin.math.abs(creatureId.hashCode()) % 5) * 0.1f

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val start = last
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                particles.step(dt)
                val iter = flying.iterator()
                while (iter.hasNext()) {
                    val f = iter.next()
                    f.t += dt * 1.6f
                    if (f.t >= 1f) {
                        iter.remove()
                        val r = creatureRect[0]
                        particles.burst(r.center.x, r.top + r.height * 0.4f, "💖", 5, 42f)
                        particles += Particle(r.center.x, r.top, "Ням!", 0f, -60f, 52f, isLabel = true, color = Palette.Coral)
                        startEmote(Action.EAT)
                        Sounds.play(Sfx.EAT)
                    }
                }
                emote[0]?.let { a ->
                    emoteT[0] += dt
                    if (emoteT[0] > (if (a == Action.EAT) 1.1f else 1.7f)) emote[0] = null
                }
                time = (now - start) / 1e9f
            }
        }
    }
    LaunchedEffect(bubble) {
        val text = bubble
        if (text != null) {
            Voice.say(text, pitch = voicePitch)
            kotlinx.coroutines.delay(2500)
            bubble = null
        }
    }
    LaunchedEffect(Unit) {
        creature.wish()?.let { Voice.say(it, pitch = voicePitch) }
    }

    val night = c.sleeping
    val nightShade by animateFloatAsState(if (night) 1f else 0f, tween(900), label = "careNight")
    val room = ImageBitmap.imageResource(R.drawable.bg_care_paper)
    val atlas = ImageBitmap.imageResource(R.drawable.care_icons)
    CarePage {
        CareHeader(c, vm.state.coins, onBack, onMore = { showMore = true })
        // Сцена с персонажем
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(mode) {
                        if (mode == Mode.WASH) {
                            detectDragGestures { change, drag ->
                                val p = change.position
                                if (!c.sleeping && creatureRect[0].inflate(16.dp.toPx()).contains(p)) {
                                    change.consume()
                                    rubbed += hypot(drag.x, drag.y)
                                    lastRub[0] = time
                                    if (rubbed > 96.dp.toPx()) {
                                        rubbed = 0f
                                        vm.wash(creatureId, 8f)
                                        Sounds.play(if (Math.random() < 0.5) Sfx.SQUEAK else Sfx.BUBBLE, volume = 0.7f)
                                        if (vm.state.creature(creatureId)?.cleanliness?.let { it >= 100f } == true) {
                                            particles.burst(p.x, p.y, "✨", 6, 44f)
                                            Sounds.play(Sfx.MAGIC)
                                            bubble = "Я чистенький! Спасибо!"
                                        }
                                    }
                                }
                            }
                        } else {
                            detectTapGestures { p ->
                                if (creatureRect[0].inflate(20f).contains(p)) {
                                    if (c.sleeping) {
                                        bubble = "Тсс… я сплю…"
                                    } else {
                                        vm.pet(creatureId)
                                        startEmote(if (Math.random() < 0.5) Action.HAPPY else Action.WAVE)
                                        Sounds.play(Sfx.HAPPY, rate = 0.9f + (Math.random() * 0.3).toFloat())
                                        particles.burst(p.x, p.y, "💖", 4, 44f)
                                        bubble = happyPhrases.random()
                                    }
                                }
                            }
                        }
                    }
            ) {
                val t = time
                drawCareRoom(room)
                val h = minOf(size.height * 0.68f, size.width * 0.7f)
                val feet = Offset(size.width / 2, size.height * 0.85f)
                val act: Action? = when {
                    c.sleeping -> null
                    emote[0] != null -> emote[0]
                    t - lastRub[0] < 0.4f -> Action.HAPPY
                    Voice.speaking -> Action.TALK
                    else -> null
                }
                creatureRect[0] = drawCreature(
                    sprite = sprite, feet = feet, height = h, time = t, seed = c.id.hashCode(),
                    sleeping = c.sleeping, sad = c.mood < 30, outfit = c.outfit,
                    swimming = c.habitat == Habitat.POND && !c.sleeping,
                    action = act, actionTime = emoteT[0],
                )
                // грязь
                if (c.cleanliness < 60f && !c.sleeping) {
                    val r = creatureRect[0]
                    val spots = ((60f - c.cleanliness) / 12f).toInt() + 1
                    for (i in 0 until spots) {
                        val fx = ((i * 37 + 11) % 80) / 100f + 0.1f
                        val fy = ((i * 53 + 23) % 70) / 100f + 0.15f
                        drawCircle(Color(0xFF7A5230).copy(alpha = 0.45f), r.width * 0.05f, Offset(r.left + r.width * fx, r.top + r.height * fy))
                    }
                    if ((t % 1.5f) < 0.75f) drawEmoji("🪰", Offset(r.right - 10f + sin(t * 6f) * 20f, r.top + 20f), 34f)
                }
                // летящая еда
                for (f in flying) {
                    val r = creatureRect[0]
                    val sx = size.width / 2
                    val sy = size.height
                    val ex = r.center.x
                    val ey = r.top + r.height * 0.45f
                    val k = f.t
                    val x = sx + (ex - sx) * k
                    val y = sy + (ey - sy) * k - sin(PI.toFloat() * k) * size.height * 0.15f
                    val icon = CareIcon.food(f.foodId)
                    val foodSize = 38.dp.toPx() * (1f - 0.3f * k)
                    if (icon != null) drawCareIcon(atlas, icon, Offset(x, y), foodSize)
                    else drawEmoji(f.emoji, Offset(x, y), foodSize)
                }
                drawParticles(particles)
                if (mode == Mode.WASH && !night) {
                    drawCareFoam(creatureRect[0], t, (1f - (t - lastRub[0]) / 2.5f).coerceIn(0f, 1f))
                }
                drawCareNight(atlas, nightShade, t)
            }
            val say = bubble ?: c.wish()
            if (say != null) {
                CareCaption(say, Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 8.dp))
            }
            if (mode == Mode.WASH) {
                CareCaption("Потри питомца пальцем", Modifier.align(Alignment.BottomCenter).padding(8.dp))
            }
        }
        CareStats(c, atlas)
        if (mode == Mode.FEED) {
            CareFoodTray(atlas, Catalog.foods.filter { (vm.state.inventory[it.id] ?: 0) > 0 },
                vm.state.inventory, c.favoriteFood) { food ->
                when (vm.feed(creatureId, food.id)) {
                    FeedResult.OK -> { flying += FlyingFood(food.id, food.emoji); bubble = "Вкусно!" }
                    FeedResult.FAVORITE -> { flying += FlyingFood(food.id, food.emoji); bubble = "Моя любимая еда!" }
                    FeedResult.FULL -> { bubble = "Я уже наелся!"; Sounds.play(Sfx.WRONG) }
                    FeedResult.SLEEPING -> bubble = "Тсс… я сплю…"
                    FeedResult.NO_FOOD -> { bubble = "Такой еды больше нет"; Sounds.play(Sfx.WRONG) }
                }
            }
        } else {
            Text(if (night) "Питомец отдыхает" else "Погладь питомца или выбери действие",
                color = Palette.Ink.copy(alpha = 0.75f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
        }
        CareActions(atlas, listOf(
            CareAction(CareIcon.APPLE, "Кормить", mode == Mode.FEED) {
                if (c.sleeping) bubble = "Сначала разбуди меня!"
                else mode = if (mode == Mode.FEED) Mode.NONE else Mode.FEED
            },
            CareAction(CareIcon.WASH, "Мыть", mode == Mode.WASH) {
                if (c.sleeping) bubble = "Сначала разбуди меня!"
                else { mode = if (mode == Mode.WASH) Mode.NONE else Mode.WASH; rubbed = 0f }
            },
            CareAction(if (night) CareIcon.SUN else CareIcon.MOON, if (night) "Разбудить" else "Спать", night) {
                Sounds.play(if (c.sleeping) Sfx.BOING else Sfx.YAWN)
                if (!c.sleeping) bubble = "Спокойной ночи!"
                vm.toggleSleep(creatureId)
                mode = Mode.NONE
            },
            CareAction(CareIcon.PLAY, "Играть") { onPlay(creatureId) },
            CareAction(CareIcon.OUTFIT, "Наряды") { onOutfit(creatureId) },
        ))
    }

    if (showMore) {
        AlertDialog(
            onDismissRequest = { showMore = false },
            title = { Text("Ещё о питомце") },
            text = {
                Column {
                    TextButton(onClick = { showMore = false; showRename = true }, modifier = Modifier.fillMaxWidth()) { Text("Изменить имя") }
                    TextButton(onClick = { showMore = false; onRig(creatureId) }, modifier = Modifier.fillMaxWidth()) { Text("Настроить движения") }
                    TextButton(onClick = { showMore = false; showMove = true }, modifier = Modifier.fillMaxWidth()) { Text("Переселить") }
                    TextButton(onClick = { showMore = false; showRelease = true }, modifier = Modifier.fillMaxWidth()) { Text("Отпустить путешествовать") }
                }
            },
            confirmButton = { TextButton(onClick = { showMore = false }) { Text("Готово") } },
        )
    }
    if (showRename) {
        var newName by remember { mutableStateOf(c.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Новое имя") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { if (it.length <= 20) newName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { vm.rename(creatureId, newName); showRename = false }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Отмена") } },
        )
    }
    if (showMove) {
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text("Где будет жить ${c.name}?") },
            text = {
                Column {
                    Habitat.entries.forEach { h ->
                        Text(
                            "${h.emoji} ${h.title}" + if (h == c.habitat) "  — сейчас здесь" else "",
                            fontSize = 18.sp,
                            fontWeight = if (h == c.habitat) FontWeight.Bold else FontWeight.Normal,
                            color = Palette.Ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = h != c.habitat) {
                                    vm.moveTo(creatureId, h)
                                    Sounds.play(
                                        when (h) {
                                            Habitat.POND -> Sfx.SPLASH
                                            Habitat.MARS -> Sfx.MAGIC
                                            else -> Sfx.BOING
                                        }
                                    )
                                    bubble = when (h) {
                                        Habitat.POND -> "Буль-буль! Я теперь плаваю!"
                                        Habitat.MARS -> "Бип-бип! Я на Марсе! Тут так легко прыгать!"
                                        Habitat.HOME -> "Как уютно дома!"
                                        Habitat.YARD -> "Ура, я во дворе!"
                                    }
                                    showMove = false
                                }
                                .padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMove = false }) { Text("Отмена") } },
        )
    }
    if (showRelease) {
        AlertDialog(
            onDismissRequest = { showRelease = false },
            title = { Text("Отпустить ${c.name}?") },
            text = { Text("Персонаж уйдёт путешествовать, и его больше не будет в игре. Точно?") },
            confirmButton = {
                TextButton(onClick = { showRelease = false; vm.release(creatureId); onBack() }) { Text("Отпустить") }
            },
            dismissButton = { TextButton(onClick = { showRelease = false }) { Text("Оставить") } },
        )
    }
}
