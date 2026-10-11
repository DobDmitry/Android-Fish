package com.dobdmitry.drawnpets.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Загрузка фото и вырезание рисунка (Android-обёртка над [ExtractCore]). */
object DrawingExtractor {

    fun loadBitmap(context: Context, uri: Uri, maxSize: Int = 1600): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (decoded != null && degrees != 0f) rotate(decoded, degrees) else decoded
    } catch (e: Exception) {
        null
    }

    fun rotate(src: Bitmap, degrees: Float): Bitmap {
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /**
     * @param sensitivity 0..1 — чем больше, тем более бледные линии считаются рисунком.
     * @return вырезанный персонаж или null, если рисунок не найден.
     */
    fun extract(source: Bitmap, sensitivity: Float): Bitmap? {
        val scale = ExtractCore.WORK_SIZE.toFloat() / max(source.width, source.height)
        val w = max(1, (source.width * min(1f, scale)).roundToInt())
        val h = max(1, (source.height * min(1f, scale)).roundToInt())
        val bmp = if (w != source.width || h != source.height) Bitmap.createScaledBitmap(source, w, h, true) else source
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val r = ExtractCore.extract(px, w, h, sensitivity) ?: return null
        return Bitmap.createBitmap(r.pixels, r.width, r.height, Bitmap.Config.ARGB_8888)
    }
}
