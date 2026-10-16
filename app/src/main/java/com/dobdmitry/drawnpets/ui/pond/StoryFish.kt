package com.dobdmitry.drawnpets.ui.pond

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.ui.games.trimSprite
import kotlin.math.sin

internal class StoryFish(private val bitmap: android.graphics.Bitmap) {
    private val vertices = FloatArray((20 + 1) * (8 + 1) * 2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun draw(scope: DrawScope, center: Offset, width: Float, facing: Float, time: Float) {
        val height = width * bitmap.height / bitmap.width
        var index = 0
        for (row in 0..8) for (col in 0..20) {
            val u = col / 20f
            val v = row / 8f
            val tail = (1f-u)*(1f-u)*(1f-u)
            vertices[index++] = center.x + (u-.5f)*width*facing
            vertices[index++] = center.y + (v-.5f)*height +
                sin(time*7f-u*4f)*height*(.012f+.11f*tail)
        }
        scope.drawContext.canvas.nativeCanvas.drawBitmapMesh(bitmap,20,8,vertices,0,null,0,paint)
    }
}

@Composable
internal fun rememberStoryFish(): List<StoryFish> {
    val context = LocalContext.current
    return remember(context) {
        val atlas = BitmapFactory.decodeResource(context.resources,R.drawable.story_fish)
        val rows = intArrayOf(0,350,663,1024)
        List(3) { i -> StoryFish(trimSprite(atlas,0,rows[i],atlas.width,rows[i+1]-rows[i])) }
    }
}
