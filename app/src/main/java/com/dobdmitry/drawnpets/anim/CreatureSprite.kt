package com.dobdmitry.drawnpets.anim

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Картинка персонажа вместе с найденным «скелетом» и аниматором. */
class CreatureSprite private constructor(val bitmap: Bitmap, val rig: Rig) {
    val image: ImageBitmap = bitmap.asImageBitmap()
    val animator = Animator(rig)
    val verts = FloatArray(Animator.VERTS * 2)

    companion object {
        fun create(bitmap: Bitmap): CreatureSprite {
            val w = bitmap.width
            val h = bitmap.height
            val px = IntArray(w * h)
            bitmap.getPixels(px, 0, w, 0, 0, w, h)
            val rig = try {
                RigAnalyzer.analyze(px, w, h)
            } catch (e: Exception) {
                Rig.empty(w, h)
            }
            return CreatureSprite(bitmap, rig)
        }
    }
}
