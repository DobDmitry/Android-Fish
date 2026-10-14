package com.dobdmitry.drawnpets.ui.care

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.Creature
import com.dobdmitry.drawnpets.data.Food
import com.dobdmitry.drawnpets.ui.common.*
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal enum class CareIcon(val cell: Int) {
    APPLE(0), CARROT(1), BANANA(2), MILK(3), CHEESE(4), COOKIE(5), PIZZA(6), ICECREAM(7),
    CAKE(8), FISH(9), WASH(10), MOON(11), SUN(12), PLAY(13), OUTFIT(14), HEART(15);

    companion object {
        fun food(id: String): CareIcon? = when (id) {
            "apple" -> APPLE; "carrot" -> CARROT; "banana" -> BANANA; "milk" -> MILK
            "cheese" -> CHEESE; "cookie" -> COOKIE; "pizza" -> PIZZA; "icecream" -> ICECREAM
            "cake" -> CAKE; "fish" -> FISH; else -> null
        }
    }
}

// Bounds in the 1254px source artwork: handmade illustrations do not fit exact grid divisions.
// In particular, the sun and pinwheel reach above the nominal fourth row.
private val careIconBounds = listOf(
    Rect(25f, 30f, 312f, 324f), Rect(370f, 30f, 600f, 329f),
    Rect(640f, 40f, 925f, 325f), Rect(985f, 45f, 1210f, 325f),
    Rect(32f, 350f, 315f, 620f), Rect(344f, 350f, 625f, 626f),
    Rect(640f, 340f, 941f, 633f), Rect(978f, 330f, 1214f, 637f),
    Rect(30f, 640f, 315f, 895f), Rect(340f, 655f, 620f, 890f),
    Rect(650f, 635f, 928f, 920f), Rect(970f, 640f, 1222f, 917f),
    Rect(20f, 895f, 330f, 1210f), Rect(345f, 894f, 621f, 1210f),
    Rect(641f, 925f, 930f, 1190f), Rect(970f, 935f, 1230f, 1185f),
)

internal fun DrawScope.drawCareIcon(atlas: ImageBitmap, icon: CareIcon, center: Offset, edge: Float) {
    val bounds = careIconBounds[icon.cell]
    val x = (bounds.left * atlas.width / 1254f).roundToInt()
    val y = (bounds.top * atlas.height / 1254f).roundToInt()
    val sourceWidth = (bounds.width * atlas.width / 1254f).roundToInt()
    val sourceHeight = (bounds.height * atlas.height / 1254f).roundToInt()
    val scale = minOf(edge / sourceWidth, edge / sourceHeight)
    val width = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val height = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    drawImage(atlas, srcOffset = IntOffset(x, y), srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset((center.x - width / 2f).roundToInt(), (center.y - height / 2f).roundToInt()),
        dstSize = IntSize(width, height), filterQuality = FilterQuality.High)
}

@Composable
internal fun CareIconView(atlas: ImageBitmap, icon: CareIcon, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawCareIcon(atlas, icon, center, size.minDimension) }
}

/** A bounded column keeps the stage usable; smaller phones and large text can scroll the whole page. */
@Composable
internal fun CarePage(content: @Composable ColumnScope.() -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize().background(PaperCream).statusBarsPadding().navigationBarsPadding()) {
        val pageHeight = maxOf(maxHeight, 700.dp + (max(0f, fontScale - 1f) * 300).dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().height(pageHeight), content = content)
        }
    }
}

@Composable
internal fun CareHeader(c: Creature, coins: Int, onBack: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Вернуться во двор", onClick = onBack)
            .semantics { contentDescription = "Назад" }, contentAlignment = Alignment.Center) {
            Text("‹", fontSize = 36.sp, color = Palette.Ink, modifier = Modifier.clearAndSetSemantics { })
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(c.name, fontFamily = PaperType, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, maxLines = 2)
            Text("Уровень ${c.level} · $coins монет", fontFamily = PaperType, fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.75f))
            val progress by animateFloatAsState(c.levelProgress.coerceIn(0f, 1f), tween(400), label = "careLevel")
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(PaperMint)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(PaperCoral))
            }
        }
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Открыть действия с питомцем", onClick = onMore)
            .semantics { contentDescription = "Ещё действия" }, contentAlignment = Alignment.Center) {
            Text("···", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Composable
internal fun CareStats(c: Creature, atlas: ImageBitmap) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).paperSurface().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareStat(atlas, CareIcon.APPLE, "Сытость", c.hunger, Color(0xFFD89A50), Modifier.weight(1f))
            CareStat(atlas, CareIcon.HEART, "Радость", c.happiness, Color(0xFFD97E88), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareStat(atlas, CareIcon.SUN, "Энергия", c.energy, Color(0xFFBE9B37), Modifier.weight(1f))
            CareStat(atlas, CareIcon.WASH, "Чистота", c.cleanliness, Color(0xFF5D9EA8), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CareStat(atlas: ImageBitmap, icon: CareIcon, label: String, value: Float, color: Color, modifier: Modifier) {
    val target = value.coerceIn(0f, 100f)
    val progress by animateFloatAsState(target / 100f, tween(450), label = label)
    Column(modifier.clearAndSetSemantics {
        contentDescription = "$label: ${target.roundToInt()} из 100" + if (target < 25f) ", нужно внимание" else ""
        progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..100f)
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CareIconView(atlas, icon, Modifier.size(23.dp))
            Text(label, Modifier.weight(1f).padding(start = 3.dp), fontFamily = PaperType, fontSize = 12.sp, color = Palette.Ink)
            Text("${target.roundToInt()}", fontFamily = PaperType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        }
        Box(Modifier.padding(top = 3.dp).fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8DDC9))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (target < 25f) PaperCoral else color))
        }
    }
}

internal data class CareAction(val icon: CareIcon, val label: String, val active: Boolean = false, val onClick: () -> Unit)

@Composable
internal fun CareActions(atlas: ImageBitmap, actions: List<CareAction>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        actions.forEach { action ->
            Column(Modifier.weight(1f).paperSurface(if (action.active) PaperMint else PaperCream, 15.dp)
                .clickable(role = Role.Button) { Sounds.play(Sfx.TAP); action.onClick() }
                .semantics { selected = action.active }.padding(horizontal = 2.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                CareIconView(atlas, action.icon, Modifier.size(36.dp))
                Text(action.label, fontFamily = PaperType, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Palette.Ink, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun CareFoodTray(atlas: ImageBitmap, foods: List<Food>, inventory: Map<String, Int>, favorite: String, onFeed: (Food) -> Unit) {
    if (foods.isEmpty()) {
        CareCaption("Еда закончилась — загляни в лавку к Еноту.", Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        foods.forEach { food ->
            val count = inventory[food.id] ?: 0
            val isFavorite = food.id == favorite
            Column(Modifier.width(76.dp).paperSurface(if (isFavorite) Color(0xFFFFE4C9) else PaperCream, 16.dp)
                .clickable(role = Role.Button, onClickLabel = "Покормить") { onFeed(food) }
                .semantics(mergeDescendants = true) { contentDescription = "${food.name}, осталось $count" + if (isFavorite) ", любимая еда" else "" }
                .padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val icon = CareIcon.food(food.id)
                if (icon != null) CareIconView(atlas, icon, Modifier.size(33.dp)) else Text(food.emoji, fontSize = 28.sp)
                Text(food.name, fontFamily = PaperType, color = Palette.Ink, fontSize = 10.sp, textAlign = TextAlign.Center)
                Text("×$count", fontFamily = PaperType, color = Palette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun CareCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.paperSurface().padding(horizontal = 13.dp, vertical = 8.dp), fontFamily = PaperType,
        color = Palette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

internal fun DrawScope.drawCareRoom(image: ImageBitmap) {
    val scale = max(size.width / image.width, size.height / image.height)
    val w = (image.width * scale).roundToInt()
    val h = (image.height * scale).roundToInt()
    drawImage(image, dstOffset = IntOffset(((size.width - w) / 2).roundToInt(), (size.height - h).roundToInt()),
        dstSize = IntSize(w, h), filterQuality = FilterQuality.High)
}

internal fun DrawScope.drawCareFoam(r: Rect, time: Float, opacity: Float) {
    if (opacity <= 0f) return
    for (i in 0..13) {
        val phase = (time * 0.25f + i * 0.137f) % 1f
        val center = Offset(r.left + r.width * (0.12f + ((i * 37) % 75) / 100f), r.bottom - r.height * phase * 0.85f)
        val radius = (5 + i % 4 * 3).dp.toPx()
        val alpha = opacity * (1f - phase)
        drawCircle(Color(0xFFD9F3ED).copy(alpha = alpha * 0.65f), radius, center)
        drawCircle(Color.White.copy(alpha = alpha), radius, center, style = Stroke(1.5.dp.toPx()))
        drawCircle(Color.White.copy(alpha = alpha), radius * 0.2f, center + Offset(-radius * 0.3f, -radius * 0.3f))
    }
}

internal fun DrawScope.drawCareNight(atlas: ImageBitmap, opacity: Float, time: Float) {
    if (opacity <= 0f) return
    drawRect(Color(0xFF182340).copy(alpha = opacity * 0.72f))
    for (i in 0..13) {
        val center = Offset(size.width * (((i * 37 + 9) % 90) / 100f + 0.05f), size.height * ((i * 17 % 45) / 100f + 0.06f))
        drawCircle(Color(0xFFFFF0BE).copy(alpha = opacity * (0.45f + sin(time * 1.5f + i) * 0.25f)), (1.2f + i % 2).dp.toPx(), center)
    }
    // The moon arrives only once the room has dimmed, avoiding a bright flash on wake-up.
    if (opacity > 0.35f) drawCareIcon(atlas, CareIcon.MOON, Offset(size.width * 0.84f, size.height * 0.18f), 42.dp.toPx() * opacity)
}
