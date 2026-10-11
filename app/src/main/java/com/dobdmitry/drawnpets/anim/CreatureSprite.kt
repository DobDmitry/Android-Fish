package com.dobdmitry.drawnpets.anim

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Картинка персонажа вместе с найденным «скелетом» и аниматором. */
/** [manual] — ручная разметка, если её задавали (иначе скелет найден автоматически). */
class CreatureSprite private constructor(val bitmap: Bitmap, val rig: Rig, val manual: ManualRig?) {
    val image: ImageBitmap = bitmap.asImageBitmap()
    val animator = Animator(rig)
    val verts = FloatArray(Animator.VERTS * 2)

    companion object {
        fun create(bitmap: Bitmap, manual: ManualRig? = null): CreatureSprite {
            val px = pixels(bitmap)
            val rig = try {
                RigAnalyzer.analyze(px, bitmap.width, bitmap.height, manual)
            } catch (e: Exception) {
                Rig.empty(bitmap.width, bitmap.height)
            }
            return CreatureSprite(bitmap, rig, manual)
        }

        /** Автоматическая разметка — для кнопки «Авто» в редакторе. */
        fun autoManual(bitmap: Bitmap): ManualRig = try {
            RigAnalyzer.toManual(RigAnalyzer.analyze(pixels(bitmap), bitmap.width, bitmap.height))
        } catch (e: Exception) {
            ManualRig(emptyList(), emptyList())
        }

        private fun pixels(bitmap: Bitmap): IntArray {
            val px = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return px
        }
    }
}
