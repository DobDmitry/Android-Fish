package com.dobdmitry.drawnpets.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.data.Creature
import kotlin.math.roundToInt
import kotlin.math.sin

internal val PaperCream = Color(0xFFFFF5E2)
internal val PaperMint = Color(0xFFD4E4C5)
internal val PaperCoral = Color(0xFFE87958)
internal val PaperType = FontFamily(Font(R.font.nunito))
private val LocalPaperAtlas = staticCompositionLocalOf<ImageBitmap> { error("Paper artwork is not provided") }

@Composable
fun PaperArtwork(atlas: ImageBitmap, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPaperAtlas provides atlas, content = content)
}

enum class PaperIcon(val cell: Int) {
    YARD(0), HOME(1), POND(2), MARS(3), CREATE(4), GAMES(5), SHOP(6), ALBUM(7), COIN(8)
}

/** Shared atlas: each icon occupies one third of each dimension, with transparent padding. */
fun DrawScope.drawPaperIcon(atlas: ImageBitmap, icon: PaperIcon, center: Offset, edge: Float) {
    val col = icon.cell % 3
    val row = icon.cell / 3
    val x = col * atlas.width / 3
    val y = row * atlas.height / 3
    drawImage(
        atlas, srcOffset = IntOffset(x, y),
        srcSize = IntSize((col + 1) * atlas.width / 3 - x, (row + 1) * atlas.height / 3 - y),
        dstOffset = IntOffset((center.x - edge / 2).roundToInt(), (center.y - edge / 2).roundToInt()),
        dstSize = IntSize(edge.roundToInt().coerceAtLeast(1), edge.roundToInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.High,
    )
}

@Composable
fun PaperIconView(icon: PaperIcon, modifier: Modifier = Modifier) {
    val atlas = LocalPaperAtlas.current
    Canvas(modifier) { drawPaperIcon(atlas, icon, center, minOf(size.width, size.height)) }
}

/** Subtle deterministic paper flecks; drawn inside the clipped panel, without per-frame randomness. */
internal fun Modifier.paperSurface(color: Color = PaperCream, radius: Dp = 20.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return shadow(3.dp, shape).clip(shape).background(color).drawBehind {
        val step = 8.dp.toPx()
        for (i in 0..(size.width / step).toInt()) {
            for (j in 0..(size.height / step).toInt()) {
                val dx = ((i * 17 + j * 7) % 11) / 11f * step
                drawCircle(Color(0xFF866A49).copy(alpha = 0.045f), 0.55.dp.toPx(), Offset(i * step + dx, j * step))
            }
        }
    }
}

@Composable
fun PaperYardHeader(coins: Int, coinsAt: Set<Habitat>, onLocation: (Habitat) -> Unit, onSettings: () -> Unit, selected: Habitat = Habitat.YARD) {
    Column(Modifier.fillMaxWidth().background(PaperCream).statusBarsPadding().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Живые рисунки", Modifier.weight(1f), fontFamily = PaperType, color = Palette.Ink, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 2)
            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PaperIconView(PaperIcon.COIN, Modifier.size(28.dp))
                Text("$coins", fontFamily = PaperType, color = Palette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть настройки") { Sounds.play(Sfx.TAP); onSettings() }
                .semantics { contentDescription = "Настройки" }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(25.dp)) {
                    val r = size.minDimension * 0.29f
                    drawCircle(Palette.Ink.copy(alpha = 0.7f), r, style = Stroke(3.dp.toPx()))
                    for (i in 0..7) {
                        val a = i * Math.PI / 4
                        val v = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                        drawLine(Palette.Ink.copy(alpha = 0.7f), center + v * (r + 1.dp.toPx()),
                            center + v * (r + 5.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Habitat.entries.forEach { habitat ->
                val icon = when (habitat) {
                    Habitat.YARD -> PaperIcon.YARD
                    Habitat.HOME -> PaperIcon.HOME
                    Habitat.POND -> PaperIcon.POND
                    Habitat.MARS -> PaperIcon.MARS
                }
                Column(Modifier.weight(1f).paperSurface(if (habitat == selected) PaperMint else PaperCream, 14.dp)
                    .selectable(selected = habitat == selected, role = Role.Tab,
                        onClick = { Sounds.play(Sfx.TAP); onLocation(habitat) })
                    .padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        PaperIconView(icon, Modifier.size(34.dp))
                        if (habitat in coinsAt) PaperIconView(PaperIcon.COIN,
                            Modifier.size(14.dp).align(Alignment.TopEnd).semantics { contentDescription = "Есть монетки" })
                    }
                    Text(habitat.title, fontFamily = PaperType, color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PaperYardActions(onCreate: () -> Unit, onGames: () -> Unit, onShop: () -> Unit, onAlbum: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(PaperCream).navigationBarsPadding().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).paperSurface(PaperCoral)
            .clickable(role = Role.Button) { Sounds.play(Sfx.TAP); onCreate() }
            .padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            PaperIconView(PaperIcon.CREATE, Modifier.size(50.dp))
            Spacer(Modifier.width(10.dp))
            Text("Оживить рисунок", fontFamily = PaperType, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(PaperIcon.GAMES, "Игры", onGames), Triple(PaperIcon.SHOP, "Лавка", onShop),
                Triple(PaperIcon.ALBUM, "Альбом", onAlbum)).forEach { (icon, label, action) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { Sounds.play(Sfx.TAP); action() }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    PaperIconView(icon, Modifier.size(35.dp))
                    Text(label, fontFamily = PaperType, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PaperYardHint(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text("Твой рисунок может ожить!", fontFamily = PaperType, color = Palette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.paperSurface().clickable(role = Role.Button, onClickLabel = "Оживить рисунок", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp), textAlign = TextAlign.Center)
}

@Composable
fun PaperCreatureCard(creature: Creature, bitmap: ImageBitmap?, onCare: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).paperSurface().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bitmap != null) Image(bitmap, null, Modifier.size(46.dp))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(creature.name, fontFamily = PaperType, color = Palette.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 2)
                Text(creature.wish() ?: "Уровень ${creature.level} · Мне хорошо!", fontFamily = PaperType, color = Palette.Ink, fontSize = 13.sp)
            }
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = "Закрыть карточку" }, contentAlignment = Alignment.Center) {
                Text("×", fontFamily = PaperType, color = Palette.Ink, fontSize = 28.sp)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(PaperMint)
            .clickable(role = Role.Button) { Sounds.play(Sfx.TAP); onCare() }.padding(12.dp), contentAlignment = Alignment.Center) {
            Text("Ухаживать", fontFamily = PaperType, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

fun DrawScope.drawPaperOwl(image: ImageBitmap, feet: Offset, height: Float, time: Float): androidx.compose.ui.geometry.Rect {
    val h = height * (1f + sin(time * 2f) * 0.012f)
    val w = h * image.width / image.height
    drawOval(Color(0xFF343C29).copy(alpha = 0.18f), Offset(feet.x - w * 0.3f, feet.y - h * 0.035f), Size(w * 0.6f, h * 0.07f))
    drawImage(image, dstOffset = IntOffset((feet.x - w / 2).roundToInt(), (feet.y - h).roundToInt()),
        dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)), filterQuality = FilterQuality.High)
    return androidx.compose.ui.geometry.Rect(feet.x - w / 2, feet.y - h, feet.x + w / 2, feet.y)
}

/** Decorative foreground stays outside character hit targets; subtle motion adds depth. */
fun DrawScope.drawPaperForeground(time: Float) {
    for (side in listOf(-1f, 1f)) {
        val x = if (side < 0) 0f else size.width
        for (i in 0..3) {
            val sway = sin(time * 0.7f + i) * 2.dp.toPx()
            val tip = Offset(x - side * (12 + i * 7).dp.toPx() + sway, size.height - (12 + i * 12).dp.toPx())
            val base = Offset(x, size.height)
            val path = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(tip.x - side * 15.dp.toPx(), tip.y + 10.dp.toPx(), tip.x, tip.y)
                quadraticTo(tip.x + side * 10.dp.toPx(), tip.y + 22.dp.toPx(), base.x, base.y)
                close()
            }
            drawPath(path, if (i % 2 == 0) Color(0xFF537C59) else Color(0xFF7D9A65))
        }
    }
}

fun DrawScope.drawPaperPanArrow(x: Float, right: Boolean, alpha: Float) {
    val p = Offset(x, size.height * 0.45f)
    drawCircle(PaperCream.copy(alpha = 0.9f), 16.dp.toPx(), p)
    val dx = (if (right) 1 else -1) * 4.dp.toPx()
    val path = Path().apply { moveTo(p.x - dx, p.y - 6.dp.toPx()); lineTo(p.x + dx, p.y); lineTo(p.x - dx, p.y + 6.dp.toPx()) }
    drawPath(path, Palette.Ink.copy(alpha = alpha), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
