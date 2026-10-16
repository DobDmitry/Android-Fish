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
import com.dobdmitry.drawnpets.ui.common.drawFieldCoins
import com.dobdmitry.drawnpets.ui.common.drawPaperAtmosphere
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
import com.dobdmitry.drawnpets.ui.common.drawPaperPanArrow
import com.dobdmitry.drawnpets.ui.npc.drawNpc
import com.dobdmitry.drawnpets.ui.npc.rememberNpcArtwork
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private var frogSpoken = false

private const val SURFACE = 0.30f
private const val BOTTOM = 0.82f

/** Пловец: и рыбки-NPC, и нарисованные персонажи. Координаты в пикселях. */
private class Swimmer(var x: Float, var y: Float) {
    var tx = x
    var ty = y
    var facingLeft = false
    var facingBlend = 1f
    var speed = 60f + Random.nextFloat() * 50f
    var rect = Rect.Zero
    var phase = Random.nextFloat() * 10f
    var targetPellet: Pellet? = null
    var happyT = -1f
}

private class Pellet(var x: Float, var y: Float) {
    var age = 0f
}

private val fishLengths = listOf(.16f, .11f, .15f, .12f, .13f, .10f, .12f)

/**
 * Сцена пруда: рыбки, корм, пузырьки и нарисованные жители. Шапка, вкладки
 * и нижние кнопки — общие для всех мест (см. WorldScreen).
 */
@Composable
fun PondScene(
    vm: GameViewModel,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onFrog: () -> Unit,
    onFed: () -> Unit,
) {
    val fishArt = rememberStoryFish()
    val npcArt = rememberNpcArtwork()
    val paperIcons = ImageBitmap.imageResource(R.drawable.paper_icons)
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
    val background = ImageBitmap.imageResource(R.drawable.bg_pond_paper)
    val frogRectRef = remember { arrayOf(Rect.Zero) }
    val coinRects = remember { mutableMapOf<Long, Rect>() }
    val coinsHere = state.fieldCoins.filter { it.location == Habitat.POND }

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
                        repeat(fishLengths.size) {
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
                    fish.forEach {
                        swim(it) {}
                        val targetFacing = if (it.facingLeft) -1f else 1f
                        it.facingBlend += (targetFacing - it.facingBlend) * (1f - kotlin.math.exp(-dt * 7f))
                    }
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
                        val coin = coinRects.entries.firstOrNull { it.value.inflate(20f).contains(pos) }?.key
                        if (coin != null) {
                            val got = vm.collectFieldCoin(coin)
                            Sounds.play(Sfx.COIN)
                            particles += Particle(pos.x, pos.y, "+$got 🪙", 0f, -110f, 44f, isLabel = true, color = Palette.Coral)
                            return@detectTapGestures
                        }
                        if (frogRectRef[0].contains(pos)) {
                            Sounds.play(Sfx.TAP)
                            onFrog()
                            return@detectTapGestures
                        }
                        val hit = currentPond.firstOrNull { c -> creatures[c.id]?.rect?.inflate(16f)?.contains(pos) == true }
                        if (hit != null) {
                            onSelect(hit.id)
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
                            onFed()
                        }
                        onSelect(null)
                    }
                }
        ) {
            val t = time
            drawSceneBackground(background, pan)
            drawPaperAtmosphere(Habitat.POND, t, pan)
            // Координаты рыб и жителей — в пикселях всего пруда (он шире экрана)
            canvasSizeRef[0] = Size(pan.worldW, size.height)
            val ox = pan.offset
            drawBubbles(t, bubbles, pan)
            // Квака на кувшинке с картинки
            frogRectRef[0] = drawNpc(Npc.FROG, Offset(pan.sx(0.87f), size.height * SURFACE + sin(t * 1.5f) * 3f), size.height * 0.18f, t, art = npcArt)
            // краб на дне
            drawPaperCrab(Offset(pan.sx(crabX - 0.1f), size.height * (BOTTOM + 0.06f)), size.width * 0.10f, t)
            // корм
            for (p in pellets) drawCircle(Color(0xFFC98B55), 7f, Offset(p.x - ox, p.y))
            // рыбки
            fish.forEachIndexed { i, s ->
                fishArt[i % fishArt.size].draw(this, Offset(s.x - ox, s.y), size.width * fishLengths[i] * 1.2f, s.facingBlend, t + s.phase)
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
            drawFieldCoins(coinsHere, pan, BOTTOM + 0.02f, BOTTOM + 0.1f, t, coinRects, paperIcons)
            drawParticles(particles)
            val a = 0.35f + 0.25f * sin(t * 3f)
            if (pan.offset > 4f) drawPaperPanArrow(22.dp.toPx(), false, a + .3f)
            if (pan.offset < pan.worldW - pan.viewW - 4f) drawPaperPanArrow(size.width - 22.dp.toPx(), true, a + .3f)
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

private fun DrawScope.drawPaperCrab(c: Offset, w: Float, time: Float) {
    val shell = Color(0xFFC97B60)
    val ink = Color(0xFF70503C)
    for (side in listOf(-1f, 1f)) {
        for (leg in 0..2) {
            val y = c.y + w * (.05f + leg*.11f)
            val sway = sin(time*7f + leg)*w*.025f
            drawLine(ink, Offset(c.x+side*w*.2f,y),Offset(c.x+side*w*.48f,y+w*.11f+sway),w*.045f,StrokeCap.Round)
        }
        val claw=Offset(c.x+side*w*.4f,c.y-w*.15f+sin(time*2f)*w*.025f)
        drawLine(ink,Offset(c.x+side*w*.18f,c.y),claw,w*.055f,StrokeCap.Round)
        drawArc(shell,30f,300f,true,Offset(claw.x-w*.15f,claw.y-w*.15f),Size(w*.3f,w*.3f))
    }
    drawOval(ink,Offset(c.x-w*.33f,c.y-w*.2f),Size(w*.66f,w*.45f))
    drawOval(shell,Offset(c.x-w*.30f,c.y-w*.18f),Size(w*.60f,w*.4f))
    for (side in listOf(-1f,1f)) {
        val eye=Offset(c.x+side*w*.12f,c.y-w*.2f)
        drawCircle(Color(0xFFFFF2D9),w*.065f,eye)
        drawCircle(ink,w*.028f,eye)
    }
    for (i in -2..2) drawCircle(Color(0xFFE6B390),w*.018f,Offset(c.x+i*w*.09f,c.y+w*.04f))
}
