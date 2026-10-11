package com.dobdmitry.drawnpets.ui.care

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
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
import com.dobdmitry.drawnpets.ui.common.EmojiButton
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Panel
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import com.dobdmitry.drawnpets.ui.common.SpeechBubble
import com.dobdmitry.drawnpets.ui.common.StatBar
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.step
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private enum class Mode { NONE, FEED, WASH, STYLE }

/** Еда, летящая ко рту персонажа. */
private class FlyingFood(val emoji: String, var t: Float = 0f)

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
) {
    val creature = vm.state.creature(creatureId)
    // Персонажа отпустили — экран сейчас закрывается, рисовать нечего.
    if (creature == null) return
    val c by rememberUpdatedState(creature)
    val sprite = vm.sprite(creatureId)

    var mode by remember { mutableStateOf(Mode.NONE) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var showRelease by remember { mutableStateOf(false) }
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
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (night) listOf(Color(0xFF1E2A5A), Color(0xFF3B3F86))
                    else listOf(Color(0xFFFFF1D6), Color(0xFFFFD9C2))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ScreenHeader("${c.name}  •  ур. ${c.level}", vm.state.coins, onBack)

            // Сцена с персонажем
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(mode) {
                            if (mode == Mode.WASH) {
                                detectDragGestures { change, drag ->
                                    val p = change.position
                                    if (creatureRect[0].inflate(30f).contains(p)) {
                                        rubbed += hypot(drag.x, drag.y)
                                        lastRub[0] = time
                                        if (Math.random() < 0.35) particles.burst(p.x, p.y, "🫧", 1, 40f)
                                        if (rubbed > 260f) {
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
                    if (night) {
                        for (i in 0 until 20) {
                            val x = ((i * 137) % 100) / 100f * size.width
                            val y = ((i * 71) % 100) / 100f * size.height * 0.6f
                            drawCircle(Color.White.copy(alpha = 0.4f + 0.4f * sin(t * 2f + i)), 3f, Offset(x, y))
                        }
                        drawEmoji("🌙", Offset(size.width * 0.82f, size.height * 0.12f), size.width * 0.14f)
                    } else {
                        // пол и коврик
                        drawRect(Color(0xFFE8B98A), Offset(0f, size.height * 0.78f), androidx.compose.ui.geometry.Size(size.width, size.height * 0.22f))
                        drawOval(Color(0xFFFF8FB8).copy(alpha = 0.5f), Offset(size.width * 0.15f, size.height * 0.8f), androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.12f))
                    }
                    val h = minOf(size.height * 0.62f, size.width * 0.7f)
                    val feet = Offset(size.width / 2, size.height * 0.88f)
                    val act: Action? = when {
                        c.sleeping -> null
                        emote[0] != null -> emote[0]
                        t - lastRub[0] < 0.4f -> Action.HAPPY
                        Voice.speaking -> Action.TALK
                        else -> null
                    }
                    creatureRect[0] = drawCreature(
                        sprite = sprite, feet = feet, height = h, time = t, seed = c.id.hashCode(),
                        sleeping = c.sleeping, sad = c.mood < 30, accessory = c.accessory,
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
                        drawEmoji(f.emoji, Offset(x, y), 70f * (1f - 0.3f * k), rotation = k * 360f)
                    }
                    drawParticles(particles)
                }
                val say = bubble ?: c.wish()
                if (say != null) {
                    SpeechBubble(say, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                }
                if (mode == Mode.WASH) {
                    Text(
                        "Потри персонажа пальцем! 🧽",
                        fontWeight = FontWeight.Bold, color = Palette.Ink,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                }
            }

            // Показатели
            Panel(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBar("🍎", "Сытость", c.hunger, Color(0xFFFFA94D), Modifier.weight(1f))
                        StatBar("😊", "Радость", c.happiness, Color(0xFFFF8FB8), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBar("⚡", "Энергия", c.energy, Color(0xFFFFD84D), Modifier.weight(1f))
                        StatBar("🫧", "Чистота", c.cleanliness, Color(0xFF6EC4FF), Modifier.weight(1f))
                    }
                    StatBar("⭐", "Уровень ${c.level}", c.levelProgress * 100f, Palette.Mint)
                }
            }

            // Подменю: еда или наряды
            when (mode) {
                Mode.FEED -> {
                    val inv = Catalog.foods.filter { (vm.state.inventory[it.id] ?: 0) > 0 }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (inv.isEmpty()) {
                            Text("Еда закончилась! Загляни в лавку к Еноту 🦝", color = Palette.Ink, fontWeight = FontWeight.Bold)
                        }
                        inv.forEach { food ->
                            val count = vm.state.inventory[food.id] ?: 0
                            Column(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, Palette.Ink.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        when (vm.feed(creatureId, food.id)) {
                                            FeedResult.OK -> { flying += FlyingFood(food.emoji); bubble = "Вкусно!" }
                                            FeedResult.FAVORITE -> { flying += FlyingFood(food.emoji); bubble = "Это моя любимая еда!!! 😍" }
                                            FeedResult.FULL -> { bubble = "Я уже наелся!"; Sounds.play(Sfx.WRONG) }
                                            FeedResult.SLEEPING -> bubble = "Тсс… я сплю…"
                                            FeedResult.NO_FOOD -> { bubble = "Такой еды больше нет"; Sounds.play(Sfx.WRONG) }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(food.emoji, fontSize = 30.sp)
                                Text("×$count", fontWeight = FontWeight.Bold, color = Palette.Ink, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Mode.STYLE -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StyleChip("🚫", c.accessory == null) { vm.setAccessory(creatureId, null) }
                        val owned = Catalog.accessories.filter { it.id in vm.state.ownedAccessories }
                        if (owned.isEmpty()) {
                            Text("Наряды продаются в лавке у Енота 🦝", color = Palette.Ink, fontWeight = FontWeight.Bold)
                        }
                        owned.forEach { acc ->
                            StyleChip(acc.emoji, c.accessory == acc.id) {
                                vm.setAccessory(creatureId, acc.id)
                                startEmote(Action.HAPPY)
                                Sounds.play(Sfx.HAPPY)
                                bubble = "Мне идёт ${acc.name.lowercase()}?"
                            }
                        }
                    }
                }
                else -> Spacer(Modifier.height(8.dp))
            }

            // Кнопки действий
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                EmojiButton("🍎", "Кормить", color = if (mode == Mode.FEED) Palette.Lemon else Color.White, size = 52.dp) {
                    mode = if (mode == Mode.FEED) Mode.NONE else Mode.FEED
                }
                EmojiButton("🧽", "Мыть", color = if (mode == Mode.WASH) Palette.Lemon else Color.White, size = 52.dp) {
                    mode = if (mode == Mode.WASH) Mode.NONE else Mode.WASH
                    if (c.sleeping) bubble = "Сначала разбуди меня!"
                }
                EmojiButton(if (c.sleeping) "☀️" else "🌙", if (c.sleeping) "Разбудить" else "Спать", size = 52.dp) {
                    Sounds.play(if (c.sleeping) Sfx.BOING else Sfx.YAWN)
                    if (!c.sleeping) bubble = "Спокойной ночи!"
                    vm.toggleSleep(creatureId)
                    mode = Mode.NONE
                }
                EmojiButton("🎮", "Играть", size = 52.dp) { onPlay(creatureId) }
                EmojiButton("🎩", "Наряд", color = if (mode == Mode.STYLE) Palette.Lemon else Color.White, size = 52.dp) {
                    mode = if (mode == Mode.STYLE) Mode.NONE else Mode.STYLE
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallAction("✏️ Имя") { showRename = true }
                SmallAction("🦴 Движения") { onRig(creatureId) }
                val other = if (c.habitat == Habitat.YARD) Habitat.POND else Habitat.YARD
                SmallAction("${other.emoji} В ${if (other == Habitat.POND) "пруд" else "двор"}") {
                    vm.moveTo(creatureId, other)
                    Sounds.play(if (other == Habitat.POND) Sfx.SPLASH else Sfx.BOING)
                    bubble = if (other == Habitat.POND) "Буль-буль! Я теперь плаваю!" else "Ура, я во дворе!"
                }
                SmallAction("👋 Отпустить") { showRelease = true }
            }
        }
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

@Composable
private fun StyleChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.Mint else Color.White)
            .border(2.dp, Palette.Ink.copy(alpha = if (selected) 0.9f else 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 28.sp) }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Palette.Ink.copy(alpha = 0.8f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
