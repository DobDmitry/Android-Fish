package com.dobdmitry.drawnpets.ui.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt

internal const val JUMP_GROUND_FRACTION = .78f

/** The inspected grass edge in the artwork is pinned to the physics landing plane. */
internal fun DrawScope.drawRunnerScene(image: ImageBitmap, ground: Float, distance: Float) {
    val edge = 1176
    val cropWidth = minOf(image.width, (edge * size.width / ground).roundToInt()).coerceAtLeast(1)
    drawImage(image, IntOffset((image.width-cropWidth)/2,0), IntSize(cropWidth,edge),
        IntOffset.Zero, IntSize(size.width.roundToInt(),ground.roundToInt()))
    // Alternating mirrored soil tiles join continuously and move right-to-left under the runner.
    val tileWidth = size.width * 1.6f
    val first = floor(distance / tileWidth).toInt()
    val shift = distance % tileWidth
    clipRect(top = ground) {
        for (i in 0..2) {
            val x = i*tileWidth-shift
            scale(if ((first+i)%2 == 0) 1f else -1f, 1f, Offset(x+tileWidth/2,ground)) {
                drawImage(image, IntOffset(0,edge), IntSize(image.width,image.height-edge),
                    IntOffset(x.roundToInt(),ground.roundToInt()),
                    IntSize(tileWidth.roundToInt()+1,(size.height-ground).roundToInt()+1))
            }
        }
    }
}
