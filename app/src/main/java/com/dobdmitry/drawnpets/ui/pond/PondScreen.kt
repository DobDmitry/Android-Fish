package com.dobdmitry.drawnpets.ui.pond

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.ui.common.LocationTabs
import com.dobdmitry.drawnpets.ui.common.ScenePan
import com.dobdmitry.drawnpets.ui.common.drawSceneBackground
import com.dobdmitry.drawnpets.ui.common.scenePan
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.CreatureInfoCard
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Particle
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private var frogSpoken = false

private const val SURFACE = 0.24f
private const val BOTTOM = 0.82f

/** Пловец: и рыбки-NPC, и нарисованные персонажи. Координаты в пикселях. */
private class Swimmer(var x: Float, var y: Float) {
    var tx = x
    var ty = y
    var facingLeft = false
    var speed = 60f + Random.nextFloat() * 50f
    var rect = Rect.Zero
    var phase = Random.nextFloat() * 10f
    var targetPellet: Pellet? = null
    var happyT = -1f
}

private class Pellet(var x: Float, var y: Float) {
    var age = 0f
}

private data class FishLook(val body: Color, val fin: Color, val length: Float, val striped: Boolean)

private val fishLooks = listOf(
    FishLook(Color(0xFFFF9A3C), Color(0xFFFF6B3C), 0.16f, false),
    FishLook(Color(0xFFFFD84D), Color(0xFFFFA62B), 0.11f, false),
    FishLook(Color(0xFF6EC4FF), Color(0xFF2F7ED8), 0.15f, true),
    FishLook(Color(0xFFFF8FB8), Color(0xFFE0507F), 0.12f, false),
    FishLook(Color(0xFFB59CFF), Color(0xFF7A5CE0), 0.13f, true),
    FishLook(Color(0xFFFF9A3C), Color(0xFFFFFFFF), 0.10f, true),
    FishLook(Color(0xFF8BE07A), Color(0xFF3FA34D), 0.12f, false),
)

@Composable
fun PondScreen(vm: GameViewModel, onBack: () -> Unit, onCare: (String) -> Unit, onLocation: (Habitat) -> Unit) {
    val state = vm.state
    val pond = state.creatures.filter { it.habitat == Habitat.POND }
    val currentPond by rememberUpdatedState(pond)

    val fish = remember { mutableListOf<Swimmer>() }
    val creatures = remember { mutableMapOf<String, Swimmer>() }
    val pellets = remember { mutableListOf<Pellet>() }
    val particles = remember { mutableListOf<Particle>() }
    val bubbles = remember { MutableList(30) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    var crabX by remember { mutableFloatStateOf(0.3f) }
    var time by remember { mutableFloatStateOf(0f) }
    val canvasSizeRef = remember { arrayOf(Size.Zero) }
    val pan = remember { ScenePan(0.4f) }
    val background = ImageBitmap.imageResource(R.drawable.bg_pond)
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showFrog by remember { mutableStateOf(false) }
    val frogRectRef = remember { arrayOf(Rect.Zero) }
    var hint by remember { mutableStateOf(true) }

    fun randomTarget(s: Swimmer, size: Size) {
        s.tx = size.width * (0.08f + Random.nextFloat() * 0.84f)
        s.ty = size.height * (SURFACE + 0.08f + Random.nextFloat() * (BOTTOM - SURFACE - 0.14f))
    }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val start = last
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                pan.step(dt)
                val sz = canvasSizeRef[0]
                if (sz.width > 0f) {
                    if (fish.isEmpty()) {
                        repeat(fishLooks.size) {
                            fish += Swimmer(sz.width * Random.nextFloat(), sz.height * (0.35f + Random.nextFloat() * 0.45f))
                                .also { s -> randomTarget(s, sz) }
                        }
                    }
                    val ids = currentPond.map { it.id }.toSet()
                    creatures.keys.retainAll(ids)
                    for (c in currentPond) {
                        creatures.getOrPut(c.id) {
                            Swimmer(sz.width * (0.2f + Random.nextFloat() * 0.6f), sz.height * 0.5f).also {
                                it.speed = 55f; randomTarget(it, sz)
                            }
                        }
                    }
                    // корм тонет
                    val pit = pellets.iterator()
                    while (pit.hasNext()) {
                        val p = pit.next()
                        p.age += dt
                        if (p.y < sz.height * (BOTTOM + 0.02f)) p.y += 28f * dt
                        if (p.age > 12f) pit.remove()
                    }
                    fun swim(s: Swimmer, onEat: () -> Unit) {
                        val tp = s.targetPellet
                        if (tp != null && tp !in pellets) s.targetPellet = null
                        if (s.targetPellet == null && pellets.isNotEmpty()) {
                            val near = pellets.minByOrNull { hypot(it.x - s.x, it.y - s.y) }
                            if (near != null && hypot(near.x - s.x, near.y - s.y) < sz.height * 0.5f) s.targetPellet = near
                        }
                        val target = s.targetPellet
                        val gx = target?.x ?: s.tx
                        val gy = target?.y ?: s.ty
                        val dx = gx - s.x
                        val dy = gy - s.y
                        val d = hypot(dx, dy)
                        val sp = if (target != null) s.speed * 2.2f else s.speed
                        if (d < 14f) {
                            if (target != null) {
                                pellets.remove(target)
                                s.targetPellet = null
                                particles.burst(s.x - pan.offset, s.y, "🫧", 2, 26f)
                                if (Random.nextFloat() < 0.5f) Sounds.play(Sfx.BUBBLE, volume = 0.5f, rate = 0.8f + Random.nextFloat() * 0.5f)
                                onEat()
                            } else randomTarget(s, sz)
                        } else {
                            s.x += dx / d * sp * dt
                            s.y += dy / d * sp * dt
                            if (abs(dx) > 3f) s.facingLeft = dx < 0
                        }
                    }
                    fish.forEach { swim(it) {} }
                    for (c in currentPond) {
                        val s = creatures[c.id] ?: continue
                        if (s.happyT >= 0f) {
                            s.happyT += dt
                            if (s.happyT > 1.6f) s.happyT = -1f
                        }
                        if (c.sleeping) continue
                        swim(s) {
                            vm.pondSnack(c.id)
                            Sounds.play(Sfx.EAT, volume = 0.6f)
                        }
                    }
                    crabX = (crabX + dt * 0.04f) % 1.2f
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
                        if (frogRectRef[0].contains(pos)) {
                            Sounds.play(Sfx.TAP)
                            showFrog = true
                            return@detectTapGestures
                        }
                        val hit = currentPond.firstOrNull { c -> creatures[c.id]?.rect?.inflate(16f)?.contains(pos) == true }
                        if (hit != null) {
                            selectedId = hit.id
                            if (!hit.sleeping) {
                                vm.pet(hit.id)
                                creatures[hit.id]?.happyT = 0f
                                Sounds.play(Sfx.HAPPY)
                            }
                            particles.burst(pos.x, pos.y - 30f, "💖", 4, 36f)
                            return@detectTapGestures
                        }
                        if (pos.y > size.height * SURFACE) {
                            repeat(3) {
                                pellets += Pellet(pos.x + pan.offset + (Random.nextFloat() - 0.5f) * 50f, pos.y + (Random.nextFloat() - 0.5f) * 20f)
                            }
                            particles.burst(pos.x, pos.y, "✨", 2, 24f)
                            Sounds.play(Sfx.SPLASH, volume = 0.7f)
                            vm.fishFed()
                            hint = false
                        }
                        selectedId = null
                    }
                }
        ) {
            val t = time
            drawSceneBackground(background, pan)
            // Координаты рыб и жителей — в пикселях всего пруда (он шире экрана)
            canvasSizeRef[0] = Size(pan.worldW, size.height)
            val ox = pan.offset
            drawBubbles(t, bubbles, pan)
            // Квака на кувшинке с картинки
            frogRectRef[0] = drawNpc(Npc.FROG, Offset(pan.sx(0.87f), size.height * 0.245f + sin(t * 1.5f) * 3f), size.height * 0.085f, t)
            // краб на дне
            drawEmoji("🦀", Offset(pan.sx(crabX - 0.1f), size.height * (BOTTOM + 0.06f) + sin(t * 10f) * 2f), size.width * 0.09f)
            // корм
            for (p in pellets) drawCircle(Color(0xFFC98B55), 7f, Offset(p.x - ox, p.y))
            // рыбки
            fish.forEachIndexed { i, s ->
                val look = fishLooks[i % fishLooks.size]
                drawFish(Offset(s.x - ox, s.y), size.width * look.length, look, s.facingLeft, t + s.phase)
            }
            // нарисованные жители
            for (c in currentPond) {
                val s = creatures[c.id] ?: continue
                val h = size.width * 0.24f
                val feet = Offset(s.x - ox, s.y + h / 2)
                s.rect = drawCreature(
                    sprite = vm.sprite(c.id), feet = feet, height = h, time = t, seed = c.id.hashCode(),
                    facingLeft = s.facingLeft, swimming = true, sleeping = c.sleeping, sad = c.mood < 30,
                    outfit = c.outfit, shadow = false,
                    action = if (s.happyT >= 0f && !c.sleeping) Action.HAPPY else null, actionTime = s.happyT,
                )
                drawLabel(c.name, Offset(feet.x, feet.y + 30f), 30f, if (c.id == selectedId) Palette.Coral else Palette.Ink)
            }
            drawParticles(particles)
            val a = 0.35f + 0.25f * sin(t * 3f)
            if (pan.offset > 4f) drawEmoji("◀️", Offset(28f, size.height * 0.5f), 48f, a)
            if (pan.offset < pan.worldW - pan.viewW - 4f) drawEmoji("▶️", Offset(size.width - 28f, size.height * 0.5f), 48f, a)
        }

        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            ScreenHeader("Пруд", state.coins, onBack)
            LocationTabs(Habitat.POND, onSelect = onLocation)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val selected = selectedId?.let { state.creature(it) }
            if (selected != null) {
                CreatureInfoCard(selected, vm.bitmap(selected.id), onCare = { onCare(selected.id) }, onClose = { selectedId = null })
            } else if (hint) {
                SpeechBubble(
                    if (pond.isEmpty()) "🐸 Ква! Нажми на воду, чтобы покормить рыбок. А ещё можно поселить сюда рисунки!"
                    else "🐸 Нажми на воду — рыбки приплывут за кормом!"
                )
            }
        }

        LaunchedEffect(Unit) {
            if (!frogSpoken) {
                frogSpoken = true
                kotlinx.coroutines.delay(600)
                Voice.say("Ква! Нажми на воду, и рыбки приплывут за кормом!", pitch = Npc.FROG.voicePitch)
            }
        }

        if (showFrog) {
            NpcDialog(
                npc = Npc.FROG, vm = vm, onClose = { showFrog = false },
                onFeedFish = { hint = true },
            )
        }
    }
}

/** Пузырьки поднимаются со дна по всей ширине пруда. */
private fun DrawScope.drawBubbles(t: Float, bubbles: List<Offset>, pan: ScenePan) {
    val surface = size.height * SURFACE
    val bottom = size.height * BOTTOM
    for (i in bubbles.indices) {
        val b = bubbles[i]
        val speed = 0.04f + (i % 4) * 0.015f
        val y = 1f - ((b.y + t * speed) % 1f)
        val yy = surface + (bottom - surface) * y
        val xx = pan.sx(b.x) + sin(t * 2f + i) * 8f
        drawCircle(Color.White.copy(alpha = 0.5f), 5f + (i % 3) * 3f, Offset(xx, yy), style = Stroke(2.5f))
    }
}

private fun DrawScope.drawFish(c: Offset, len: Float, look: FishLook, facingLeft: Boolean, t: Float) {
    val ink = Palette.Ink
    scale(if (facingLeft) -1f else 1f, 1f, pivot = c) {
        val bodyH = len * 0.55f
        val wag = sin(t * 9f) * 18f
        // хвост
        rotate(wag, pivot = Offset(c.x - len * 0.38f, c.y)) {
            val tail = Path().apply {
                moveTo(c.x - len * 0.38f, c.y)
                lineTo(c.x - len * 0.7f, c.y - bodyH * 0.55f)
                lineTo(c.x - len * 0.62f, c.y)
                lineTo(c.x - len * 0.7f, c.y + bodyH * 0.55f)
                close()
            }
            drawPath(tail, look.fin)
            drawPath(tail, ink, style = Stroke(2.5f))
        }
        // спинной плавник
        val fin = Path().apply {
            moveTo(c.x - len * 0.15f, c.y - bodyH * 0.4f)
            quadraticBezierTo(c.x, c.y - bodyH * 0.95f, c.x + len * 0.15f, c.y - bodyH * 0.4f)
            close()
        }
        drawPath(fin, look.fin)
        drawPath(fin, ink, style = Stroke(2.5f))
        drawOval(look.body, Offset(c.x - len / 2, c.y - bodyH / 2), Size(len, bodyH))
        if (look.striped) {
            for (k in 0..1) {
                val x = c.x - len * 0.05f + k * len * 0.15f
                drawLine(look.fin.copy(alpha = 0.8f), Offset(x, c.y - bodyH * 0.4f), Offset(x, c.y + bodyH * 0.4f), strokeWidth = len * 0.06f, cap = StrokeCap.Round)
            }
        }
        drawOval(ink, Offset(c.x - len / 2, c.y - bodyH / 2), Size(len, bodyH), style = Stroke(2.5f))
        val eye = Offset(c.x + len * 0.28f, c.y - bodyH * 0.1f)
        drawCircle(Color.White, len * 0.085f, eye)
        drawCircle(ink, len * 0.045f, Offset(eye.x + len * 0.015f, eye.y))
        drawArc(ink, -40f, 80f, false, Offset(c.x + len * 0.34f, c.y + bodyH * 0.02f), Size(len * 0.1f, len * 0.08f), style = Stroke(2f))
    }
}
