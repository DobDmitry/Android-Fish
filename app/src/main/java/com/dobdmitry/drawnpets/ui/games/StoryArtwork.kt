package com.dobdmitry.drawnpets.ui.games

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dobdmitry.drawnpets.R
import kotlin.math.roundToInt

/** Immutable decoded artwork shared by menus and rounds; never decoded in a draw loop. */
internal class StoryArtwork(resources: Resources) {
    val scenes = BitmapFactory.decodeResource(resources, R.drawable.story_scenes).asImageBitmap()
    val items: List<ImageBitmap> = BitmapFactory.decodeResource(resources, R.drawable.story_items).let { atlas ->
        List(20) { i ->
            // The cactus cell contains the edge of the neighboring cloud: isolate its painted bounds.
            if (i == 17) return@List trimSprite(atlas, 655, 738, 235, 270).asImageBitmap()
            val left = i % 5 * atlas.width / 5
            val rows = intArrayOf(0, 272, 515, 735, 1024)
            val top = rows[i / 5]
            trimSprite(atlas, left, top, (i % 5 + 1) * atlas.width / 5 - left, rows[i / 5 + 1] - top).asImageBitmap()
        }
    }
    companion object {
        @Volatile private var cached: StoryArtwork? = null
        fun get(resources: Resources): StoryArtwork = cached ?: synchronized(this) {
            cached ?: StoryArtwork(resources).also { cached = it }
        }
    }
}

/** Trim only transparent padding; retain soft painted edges. */
internal fun trimSprite(atlas: Bitmap, x: Int, y: Int, w: Int, h: Int): Bitmap {
    val pixels = IntArray(w * h)
    atlas.getPixels(pixels, 0, w, x, y, w, h)
    var left = w; var top = h; var right = -1; var bottom = -1
    for (row in 0 until h) for (col in 0 until w) {
        if ((pixels[row * w + col] ushr 24) > 8) {
            left = minOf(left, col); right = maxOf(right, col)
            top = minOf(top, row); bottom = maxOf(bottom, row)
        }
    }
    if (right < left) return Bitmap.createBitmap(atlas, x, y, w, h)
    return Bitmap.createBitmap(atlas, x + left, y + top, right - left + 1, bottom - top + 1)
}

@Composable
internal fun rememberStoryArtwork(): StoryArtwork {
    val context = LocalContext.current
    return remember(context) { StoryArtwork.get(context.resources) }
}

internal fun DrawScope.drawStoryScene(art: StoryArtwork, panel: Int) {
    val w = art.scenes.width / 3
    val h = art.scenes.height / 2
    // Center crop, never distort painted trees or the cottage on tall screens.
    val ratio = size.width / size.height
    val sw = minOf(w, (h * ratio).roundToInt()).coerceAtLeast(1)
    val sh = minOf(h, (w / ratio).roundToInt()).coerceAtLeast(1)
    drawImage(art.scenes, IntOffset(panel % 3 * w + (w-sw)/2, panel/3*h + (h-sh)/2),
        IntSize(sw,sh), IntOffset.Zero, IntSize(size.width.roundToInt(),size.height.roundToInt()))
}

private val tokenIndices = mapOf(
    "🍎" to 0, "🍐" to 1, "🥕" to 2, "🍪" to 3, "🍌" to 4,
    "🍓" to 5, "🧀" to 6, "🥛" to 7, "🍰" to 8, "🪨" to 9,
    "🐝" to 10, "⭐" to 11, "🌟" to 11, "✨" to 11, "🏆" to 11,
    "🪙" to 12, "🧺" to 13, "🌳" to 14, "🎈" to 15, "⛈️" to 16,
    "🌵" to 17, "🍄" to 18,
)

internal fun DrawScope.drawGameToken(art: StoryArtwork, token: String, c: Offset, edge: Float, rotation: Float = 0f) {
    val index = tokenIndices[token]
    if (index == null) { drawGameToken(token, c, edge, rotation); return }
    val bitmap = art.items[index]
    val scale = edge / maxOf(bitmap.width,bitmap.height)
    rotate(rotation, c) { drawStoryItem(art,index,c,Size(bitmap.width*scale,bitmap.height*scale)) }
}

internal fun DrawScope.drawStoryItem(art: StoryArtwork, index: Int, center: Offset, extent: Size) {
    val bitmap = art.items[index]
    drawImage(bitmap, IntOffset.Zero, IntSize(bitmap.width,bitmap.height),
        IntOffset((center.x-extent.width/2).roundToInt(),(center.y-extent.height/2).roundToInt()),
        IntSize(extent.width.roundToInt().coerceAtLeast(1),extent.height.roundToInt().coerceAtLeast(1)))
}
