package com.dobdmitry.drawnpets.ui.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.data.Creature
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.CoinBadge
import com.dobdmitry.drawnpets.ui.common.CreatureInfoCard
import com.dobdmitry.drawnpets.ui.common.EmojiButton
import com.dobdmitry.drawnpets.ui.common.LocationTabs
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.ScenePan
import com.dobdmitry.drawnpets.ui.common.SpeechBubble
import com.dobdmitry.drawnpets.ui.common.burst
import com.dobdmitry.drawnpets.ui.common.drawCreature
import com.dobdmitry.drawnpets.ui.common.drawEmoji
import com.dobdmitry.drawnpets.ui.common.drawLabel
import com.dobdmitry.drawnpets.ui.common.drawParticles
import com.dobdmitry.drawnpets.ui.common.drawSceneBackground
import com.dobdmitry.drawnpets.ui.common.scenePan
import com.dobdmitry.drawnpets.ui.common.step
import com.dobdmitry.drawnpets.ui.npc.Npc
import com.dobdmitry.drawnpets.ui.npc.NpcDialog
import com.dobdmitry.drawnpets.ui.npc.drawNpc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Состояние «гуляющего» персонажа: x — доля ширины мира, y — доля высоты экрана. */
private class Walker(var x: Float, var y: Float) {
    var tx = x
    var ty = y
    var wait = Random.nextFloat() * 2f
    var facingLeft = Random.nextBoolean()
    var moving = false
    var jumpT = -1f
    var rect = Rect.Zero
    /** Эмоция поверх ходьбы: радость после касания или приветствие. */
    var emote: Action? = null
    var emoteT = 0f
}

private data class NpcSpot(val npc: Npc, val x: Float, val y: Float, val size: Float, val facingLeft: Boolean)

/**
 * Настройки «ходячей» локации: фон, полоса, по которой гуляют персонажи
 * (доли высоты), NPC (x — доля ширины фона, y — доля высоты, size — рост в долях высоты).
 */
private class LocationSpec(
    val bg: Int,
    val minY: Float,
    val maxY: Float,
    val npcs: List<NpcSpot>,
    val start: Float,
    val lowGravity: Boolean = false,
)

private val specs = mapOf(
    Habitat.YARD to LocationSpec(
        R.drawable.bg_yard, 0.68f, 0.92f,
        listOf(
            NpcSpot(Npc.OWL, 0.835f, 0.335f, 0.085f, true),
            NpcSpot(Npc.RACCOON, 0.70f, 0.70f, 0.13f, false),
            NpcSpot(Npc.HEDGEHOG, 0.24f, 0.67f, 0.085f, false),
        ),
        start = 0.35f,
    ),
    Habitat.HOME to LocationSpec(
        R.drawable.bg_home, 0.64f, 0.93f,
        listOf(NpcSpot(Npc.CAT, 0.37f, 0.66f, 0.12f, false)),
        start = 0.45f,
    ),
    Habitat.MARS to LocationSpec(
        R.drawable.bg_mars, 0.56f, 0.92f,
        listOf(NpcSpot(Npc.ROBOT, 0.60f, 0.6f, 0.14f, true)),
        start = 0.3f,
        lowGravity = true,
    ),
)

private class SceneItem(val y: Float, val draw: DrawScope.() -> Unit)

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
    val location = vm.location
    // Для каждой локации — своя панорама и свои гуляющие
    key(location) {
        LocationScene(vm, location, onCreate, onPond, onGames, onShop, onAlbum, onCare)
    }
}

@Composable
private fun LocationScene(
    vm: GameViewModel,
    location: Habitat,
    onCreate: () -> Unit,
    onPond: () -> Unit,
    onGames: () -> Unit,
    onShop: () -> Unit,
    onAlbum: () -> Unit,
    onCare: (String) -> Unit,
) {
    val spec = specs[location] ?: specs.getValue(Habitat.YARD)
    val state = vm.state
    val here = state.creatures.filter { it.habitat == location }
    val currentHere by rememberUpdatedState(here)
    val background: ImageBitmap = ImageBitmap.imageResource(spec.bg)

    val pan = remember { ScenePan(spec.start) }
    val walkers = remember { mutableMapOf<String, Walker>() }
    val particles = remember { mutableListOf<Particle>() }
    val npcRects = remember { mutableMapOf<Npc, Rect>() }
    var time by remember { mutableFloatStateOf(0f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var openNpc by remember { mutableStateOf<Npc?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val start = last
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                pan.step(dt)
                val ids = currentHere.map { it.id }.toSet()
                walkers.keys.retainAll(ids)
                for (c in currentHere) {
                    val w = walkers.getOrPut(c.id) {
                        Walker(0.1f + Random.nextFloat() * 0.8f, spec.minY + Random.nextFloat() * (spec.maxY - spec.minY))
                    }
                    updateWalker(w, c, dt, spec)
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
                .scenePan(pan)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val hit = currentHere
                            .mapNotNull { c -> walkers[c.id]?.let { c to it } }
                            .sortedByDescending { it.second.y }
                            .firstOrNull { it.second.rect.inflate(12f).contains(pos) }
                        if (hit != null) {
                            val (c, w) = hit
                            selectedId = c.id
                            if (!c.sleeping) {
                                vm.pet(c.id)
                                w.emote = if (Random.nextBoolean()) Action.HAPPY else Action.WAVE
                                w.emoteT = 0f
                                Sounds.play(Sfx.HAPPY, rate = 0.9f + Random.nextFloat() * 0.3f)
                            } else {
                                Sounds.play(Sfx.TAP)
                            }
                            particles.burst(pos.x, w.rect.top, if (c.sleeping) "💤" else "💖", 4, 40f)
                            return@detectTapGestures
                        }
                        val npc = npcRects.entries.firstOrNull { it.value.contains(pos) }?.key
                        if (npc != null) {
                            Sounds.play(Sfx.TAP)
                            openNpc = npc
                            return@detectTapGestures
                        }
                        selectedId = null
                    }
                }
        ) {
            val t = time
            drawSceneBackground(background, pan)
            val items = mutableListOf<SceneItem>()
            for (spot in spec.npcs) {
                items += SceneItem(spot.y) {
                    npcRects[spot.npc] = drawNpc(
                        spot.npc, Offset(pan.sx(spot.x), size.height * spot.y),
                        size.height * spot.size, t, spot.facingLeft,
                    )
                }
            }
            for (c in currentHere) {
                val w = walkers[c.id] ?: continue
                items += SceneItem(w.y) {
                    val depth = 0.75f + (w.y - spec.minY) / (spec.maxY - spec.minY) * 0.35f
                    val h = size.height * 0.15f * depth
                    var jump = if (w.jumpT >= 0f) sin(PI.toFloat() * w.jumpT / 0.5f) * h * 0.45f else 0f
                    // На Марсе притяжение слабое — персонажи прыгают высоко и плавно
                    if (spec.lowGravity && w.moving) jump += abs(sin(t * 3f + c.id.hashCode())) * h * 0.35f
                    val feet = Offset(pan.sx(w.x), size.height * w.y)
                    w.rect = drawCreature(
                        sprite = vm.sprite(c.id), feet = feet, height = h, time = t,
                        seed = c.id.hashCode(), facingLeft = w.facingLeft, moving = w.moving,
                        sleeping = c.sleeping, sad = c.mood < 30, outfit = c.outfit, jump = jump,
                        action = if (c.sleeping) null else w.emote, actionTime = w.emoteT,
                    )
                    val labelColor = if (c.id == selectedId) Palette.Coral else Palette.Ink
                    drawLabel(c.name, Offset(feet.x, feet.y + 34f), 34f, labelColor)
                    c.wish()?.let { if (!c.sleeping && (t + c.id.hashCode() % 7) % 7f < 2.5f) drawEmoji("💭", Offset(w.rect.right, w.rect.top), 44f) }
                }
            }
            items.sortedBy { it.y }.forEach { it.draw(this) }
            drawParticles(particles)
            // Подсказка, что сцену можно двигать: стрелки у краёв
            val a = 0.35f + 0.25f * sin(t * 3f)
            if (pan.offset > 4f) drawEmoji("◀️", Offset(28f, size.height * 0.5f), 48f, a)
            if (pan.offset < pan.worldW - pan.viewW - 4f) drawEmoji("▶️", Offset(size.width - 28f, size.height * 0.5f), 48f, a)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Живые рисунки",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Palette.Ink,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                CoinBadge(state.coins)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable { Sounds.play(Sfx.TAP); showSettings = true },
                    contentAlignment = Alignment.Center,
                ) { Text("⚙️", fontSize = 22.sp) }
            }
            LocationTabs(location, onSelect = { h ->
                selectedId = null
                if (h == Habitat.POND) onPond() else vm.location = h
            })
            val showHint = location == Habitat.YARD && state.creatures.size <= 2 && selectedId == null && openNpc == null
            LaunchedEffect(showHint) {
                if (showHint && !hintSpoken) {
                    hintSpoken = true
                    kotlinx.coroutines.delay(800)
                    Voice.say("Привет! Нарисуй персонажа на бумаге и нажми на кнопку с камерой. И он оживёт!", pitch = Npc.OWL.voicePitch)
                }
            }
            if (showHint) {
                SpeechBubble(
                    "🦉 Нарисуй персонажа на бумаге и нажми «Рисунок» — он оживёт! Двигай картинку пальцем, чтобы осмотреться.",
                    Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
            }
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
                EmojiButton("🎮", "Игры", color = Color(0xFFFFD6E5), onClick = onGames)
                EmojiButton("🏪", "Лавка", color = Color(0xFFD8F5E6), onClick = onShop)
                EmojiButton("📖", "Альбом", color = Color(0xFFEDE1FF), onClick = onAlbum)
            }
        }

        if (showSettings) SettingsDialog(onClose = { showSettings = false })

        openNpc?.let { npc ->
            NpcDialog(
                npc = npc, vm = vm, onClose = { openNpc = null },
                onOpenShop = onShop, onOpenGames = onGames, onFeedFish = onPond,
            )
        }
    }
}

private fun updateWalker(w: Walker, c: Creature, dt: Float, spec: LocationSpec) {
    if (w.jumpT >= 0f) {
        w.jumpT += dt
        if (w.jumpT > 0.5f) w.jumpT = -1f
    }
    if (w.emote != null) {
        w.emoteT += dt
        if (w.emoteT > 1.8f) w.emote = null
        else return
    }
    if (c.sleeping) {
        w.moving = false
        return
    }
    if (w.wait > 0f) {
        w.wait -= dt
        w.moving = false
        if (w.wait <= 0f && Random.nextFloat() < 0.2f && c.mood > 40f) {
            // иногда машет рукой просто так
            w.emote = Action.WAVE; w.emoteT = 0f
        }
        if (w.wait <= 0f) {
            // гуляем неподалёку, чтобы не убегать далеко за край экрана
            w.tx = (w.x + (Random.nextFloat() - 0.5f) * 0.35f).coerceIn(0.05f, 0.95f)
            w.ty = spec.minY + Random.nextFloat() * (spec.maxY - spec.minY)
        }
        return
    }
    // x — доля ширины мира (он в 3–4 раза шире экрана), поэтому по x шагаем медленнее
    val dx = (w.tx - w.x) * 3.5f
    val dy = w.ty - w.y
    val dist = hypot(dx, dy)
    val tired = c.energy < 25f
    val speed = (if (tired) 0.035f else 0.08f) * (if (spec.lowGravity) 0.7f else 1f)
    if (dist < 0.01f) {
        w.wait = 1.5f + Random.nextFloat() * 4f
        w.moving = false
        return
    }
    w.moving = true
    w.x += dx / dist * speed * dt / 3.5f
    w.y += dy / dist * speed * dt
    if (abs(dx) > 0.005f) w.facingLeft = dx < 0
}

/** Чтобы подсказка Филина звучала один раз за запуск. */
private var hintSpoken = false

@Composable
private fun SettingsDialog(onClose: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Настройки", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                SettingRow("🎵", "Музыка", Sounds.musicOn) { Sounds.setMusic(it) }
                SettingRow("🔔", "Звуки", Sounds.soundOn) { Sounds.setSound(it) }
                SettingRow("🗣️", "Голос героев", Sounds.voiceOn) { Sounds.setVoice(it) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
                }
                Text("Версия $version", fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Готово") } },
    )
}

@Composable
private fun SettingRow(emoji: String, title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 18.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange)
    }
}

