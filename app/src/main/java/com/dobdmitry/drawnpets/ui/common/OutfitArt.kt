package com.dobdmitry.drawnpets.ui.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas

/** Shared vector artwork: identical geometry in the shop, fitting room and world. */
object OutfitArt {
    private data class Layer(val path: Path, val fill: Int?, val stroke: Int?, val width: Float)
    private val layers by lazy {
        outfitPaths.mapValues { (_, paths) -> paths.map {
            Layer(PathParser.createPathFromPathData(it.d)!!.apply { fillType = Path.FillType.EVEN_ODD },
                if (it.fill == "none") null else Color.parseColor(it.fill),
                if (it.stroke == "none") null else Color.parseColor(it.stroke), it.width)
        } }
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun draw(canvas: Canvas, id: String, x: Float, y: Float, size: Float, rotation: Float = 0f, alpha: Float = 1f) {
        if (!size.isFinite() || size <= 0f) return
        val parts = layers[id] ?: return
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.scale(size / 100f, size / 100f)
        canvas.translate(-50f, -50f)
        for (part in parts) {
            part.fill?.let {
                paint.style = Paint.Style.FILL; paint.color = it; paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawPath(part.path, paint)
            }
            part.stroke?.let {
                paint.style = Paint.Style.STROKE; paint.color = it; paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
                paint.strokeWidth = part.width
                canvas.drawPath(part.path, paint)
            }
        }
        canvas.restore()
    }
}

@Composable
fun OutfitIcon(id: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        OutfitArt.draw(drawContext.canvas.nativeCanvas, id, center.x, center.y, size.minDimension * 0.94f)
    }
}
