package com.dobdmitry.drawnpets.ui.npc

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dobdmitry.drawnpets.R
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Npc(val title: String, val role: String, val emoji: String, val voicePitch: Float) {
    OWL("Дедушка Филин", "Мудрый советчик", "🦉", 0.75f),
    RACCOON("Енот Тимофей", "Хозяин лавки", "🦝", 1.0f),
    HEDGEHOG("Доктор Ёжик", "Лечит питомцев", "🦔", 1.2f),
    CAT("Кошка Мурка", "Затейница игр", "🐱", 1.45f),
    FROG("Лягушка Квака", "Хозяйка пруда", "🐸", 1.3f),
    ROBOT("Робот Винтик", "Исследователь Марса", "🤖", 0.9f),
}


data class NpcArtwork(val sheet: ImageBitmap, val owl: ImageBitmap)

@Composable
fun rememberNpcArtwork() = NpcArtwork(ImageBitmap.imageResource(R.drawable.paper_npcs), ImageBitmap.imageResource(R.drawable.paper_owl))

// Tight alpha bounds in the 1536x1024 source; padding never changes apparent character scale.
private val regions = mapOf(
    Npc.RACCOON to Rect(38f, 8f, 501f, 511f),
    Npc.HEDGEHOG to Rect(560f, 20f, 1010f, 506f),
    Npc.CAT to Rect(1034f, 16f, 1520f, 509f),
    Npc.FROG to Rect(67f, 518f, 499f, 1014f),
    Npc.ROBOT to Rect(615f, 510f, 1006f, 1003f),
)

/** A shared paper cutout in every scene and portrait, with subtle breathing and grounded feet. */
fun DrawScope.drawNpc(npc: Npc, feet: Offset, h: Float, time: Float, facingLeft: Boolean = false, art: NpcArtwork): Rect {
    val image = if (npc == Npc.OWL) art.owl else art.sheet
    val region = regions[npc]
    val x = region?.left?.times(image.width / 1536f)?.roundToInt() ?: 0
    val y = region?.top?.times(image.height / 1024f)?.roundToInt() ?: 0
    val sw = region?.width?.times(image.width / 1536f)?.roundToInt() ?: image.width
    val sh = region?.height?.times(image.height / 1024f)?.roundToInt() ?: image.height
    val height = h * (1f + sin(time * 2f + npc.ordinal) * .012f)
    val width = height * sw / sh
    drawOval(Color(0xFF514738).copy(alpha = .15f), Offset(feet.x-width*.28f,feet.y-h*.025f), Size(width*.56f,h*.05f))
    scale(if (facingLeft) -1f else 1f, 1f, pivot=feet) {
        drawImage(image, srcOffset=IntOffset(x,y), srcSize=IntSize(sw,sh),
            dstOffset=IntOffset((feet.x-width/2).roundToInt(),(feet.y-height).roundToInt()),
            dstSize=IntSize(width.roundToInt().coerceAtLeast(1),height.roundToInt().coerceAtLeast(1)), filterQuality=FilterQuality.High)
    }
    return Rect(feet.x-width/2,feet.y-height,feet.x+width/2,feet.y)
}
