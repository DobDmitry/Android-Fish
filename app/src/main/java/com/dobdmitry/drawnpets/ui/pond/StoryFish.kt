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

    fun draw(scope: DrawScope, center: Offset, width: Float, facingLeft: Boolean, tilt: Float, time: Float) {
        val height = width * bitmap.height / bitmap.width
        val facing = if (facingLeft) -1f else 1f
        var index = 0
        for (row in 0..8) for (col in 0..20) {
            val u = col / 20f
            val v = row / 8f
            val tail = (1f-u)*(1f-u)*(1f-u)
            vertices[index++] = center.x + (u-.5f)*width*facing
            vertices[index++] = center.y + (v-.5f)*height +
                sin(time*7f-u*4f)*height*(.006f+.045f*tail)
        }
        val canvas = scope.drawContext.canvas.nativeCanvas
        canvas.save()
        canvas.rotate(-tilt * facing, center.x, center.y)
        canvas.drawBitmapMesh(bitmap,20,8,vertices,0,null,0,paint)
        canvas.restore()
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
