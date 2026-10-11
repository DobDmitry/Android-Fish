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
import kotlin.math.sqrt

/**
 * Вырезает детский рисунок с фотографии листа бумаги.
 *
 * Идея: оцениваем локальный цвет бумаги (он неравномерен из-за теней и света),
 * считаем «чернилами» всё, что заметно отличается от бумаги, закрываем
 * небольшие разрывы в контуре, заливаем «дырки» внутри (дети часто рисуют
 * только контур) и оставляем крупные связные области. На выходе — картинка
 * с прозрачным фоном и белой обводкой, как у наклейки.
 */
object DrawingExtractor {

    private const val WORK_SIZE = 520
    private const val OUTLINE = 6

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
        val scale = WORK_SIZE.toFloat() / max(source.width, source.height)
        val w = max(1, (source.width * min(1f, scale)).roundToInt())
        val h = max(1, (source.height * min(1f, scale)).roundToInt())
        val bmp = if (w != source.width || h != source.height) Bitmap.createScaledBitmap(source, w, h, true) else source
        val n = w * h
        val px = IntArray(n)
        bmp.getPixels(px, 0, w, 0, 0, w, h)

        // Лёгкое размытие 3x3, чтобы убрать шум и артефакты JPEG.
        val r = FloatArray(n); val g = FloatArray(n); val b = FloatArray(n)
        for (i in 0 until n) {
            val c = px[i]
            r[i] = ((c shr 16) and 0xFF).toFloat()
            g[i] = ((c shr 8) and 0xFF).toFloat()
            b[i] = (c and 0xFF).toFloat()
        }
        val rs = boxBlur(r, w, h, 1); val gs = boxBlur(g, w, h, 1); val bs = boxBlur(b, w, h, 1)
        val lum = FloatArray(n) { 0.299f * rs[it] + 0.587f * gs[it] + 0.114f * bs[it] }

        // 1. Оценка цвета бумаги по ячейкам сетки.
        val cells = 14
        val cw = (w + cells - 1) / cells
        val ch = (h + cells - 1) / cells
        val gx = (w + cw - 1) / cw
        val gy = (h + ch - 1) / ch
        val cellL = FloatArray(gx * gy)
        val cellR = FloatArray(gx * gy); val cellG = FloatArray(gx * gy); val cellB = FloatArray(gx * gy)
        val hist = IntArray(256)
        for (cy in 0 until gy) for (cx in 0 until gx) {
            hist.fill(0)
            var count = 0
            val x0 = cx * cw; val x1 = min(w, x0 + cw)
            val y0 = cy * ch; val y1 = min(h, y0 + ch)
            for (y in y0 until y1) for (x in x0 until x1) {
                hist[lum[y * w + x].toInt().coerceIn(0, 255)]++; count++
            }
            // 85-й перцентиль яркости — это почти наверняка бумага
            var acc = 0; var p = 255
            val need = (count * 0.85f).toInt()
            for (v in 0..255) { acc += hist[v]; if (acc >= need) { p = v; break } }
            var sr = 0f; var sg = 0f; var sb = 0f; var k = 0
            for (y in y0 until y1) for (x in x0 until x1) {
                val i = y * w + x
                if (lum[i] >= p - 4) { sr += rs[i]; sg += gs[i]; sb += bs[i]; k++ }
            }
            val ci = cy * gx + cx
            cellL[ci] = p.toFloat()
            if (k > 0) { cellR[ci] = sr / k; cellG[ci] = sg / k; cellB[ci] = sb / k }
            else { cellR[ci] = p.toFloat(); cellG[ci] = p.toFloat(); cellB[ci] = p.toFloat() }
        }
        // Ячейка, целиком закрашенная ребёнком, темнее соседей — подтягиваем её к соседям.
        repeat(2) {
            val copyL = cellL.copyOf()
            val copyR = cellR.copyOf(); val copyG = cellG.copyOf(); val copyB = cellB.copyOf()
            for (cy in 0 until gy) for (cx in 0 until gx) {
                var best = -1
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = cx + dx; val ny = cy + dy
                    if (nx in 0 until gx && ny in 0 until gy) {
                        val ni = ny * gx + nx
                        if (best < 0 || copyL[ni] > copyL[best]) best = ni
                    }
                }
                val ci = cy * gx + cx
                if (copyL[best] - 30 > copyL[ci]) {
                    cellL[ci] = copyL[best] - 30
                    cellR[ci] = copyR[best]; cellG[ci] = copyG[best]; cellB[ci] = copyB[best]
                }
            }
        }
        val bgR = FloatArray(n); val bgG = FloatArray(n); val bgB = FloatArray(n)
        for (y in 0 until h) for (x in 0 until w) {
            val fx = ((x + 0.5f) / cw - 0.5f).coerceIn(0f, (gx - 1).toFloat())
            val fy = ((y + 0.5f) / ch - 0.5f).coerceIn(0f, (gy - 1).toFloat())
            val x0 = fx.toInt(); val y0 = fy.toInt()
            val x1 = min(gx - 1, x0 + 1); val y1 = min(gy - 1, y0 + 1)
            val tx = fx - x0; val ty = fy - y0
            fun lerp2(a: FloatArray): Float {
                val top = a[y0 * gx + x0] * (1 - tx) + a[y0 * gx + x1] * tx
                val bot = a[y1 * gx + x0] * (1 - tx) + a[y1 * gx + x1] * tx
                return top * (1 - ty) + bot * ty
            }
            val i = y * w + x
            bgR[i] = lerp2(cellR); bgG[i] = lerp2(cellG); bgB[i] = lerp2(cellB)
        }

        // 2. Маска «чернил».
        val thr = 72f - 54f * sensitivity.coerceIn(0f, 1f)
        val ink = BooleanArray(n)
        for (i in 0 until n) {
            val dr = bgR[i] - rs[i]; val dg = bgG[i] - gs[i]; val db = bgB[i] - bs[i]
            val darker = (dr + dg + db) / 3f
            val mean = (dr + dg + db) / 3f
            val chroma = sqrt(((dr - mean) * (dr - mean) + (dg - mean) * (dg - mean) + (db - mean) * (db - mean)) / 3f)
            val dist = max(0f, darker) + chroma * 2.2f
            ink[i] = dist > thr
        }

        // 3. Закрываем мелкие разрывы контура.
        val closeR = max(2, (w + h) / 300)
        var mask = erode(dilate(ink, w, h, closeR), w, h, closeR)
        // убираем «соль» — одиночные точки
        mask = dilate(erode(mask, w, h, 1), w, h, 1)

        // 4. Заливаем всё, что внутри контура.
        mask = fillHoles(mask, w, h)

        // 5. Оставляем крупные связные области.
        mask = keepMainComponents(mask, w, h) ?: return null

        // 6. Обрезка по рамке.
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) if (mask[y * w + x]) {
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        if (maxX < 0) return null
        val pad = OUTLINE + 3
        val ow = maxX - minX + 1 + pad * 2
        val oh = maxY - minY + 1 + pad * 2

        // Маска в координатах выходной картинки.
        val om = BooleanArray(ow * oh)
        for (y in minY..maxY) for (x in minX..maxX) {
            if (mask[y * w + x]) om[(y - minY + pad) * ow + (x - minX + pad)] = true
        }
        val alpha = boxBlur(FloatArray(ow * oh) { if (om[it]) 1f else 0f }, ow, oh, 1)
        val dist = chamferDistance(om, ow, oh)

        val out = IntArray(ow * oh)
        for (oy in 0 until oh) for (ox in 0 until ow) {
            val oi = oy * ow + ox
            val sx = ox - pad + minX; val sy = oy - pad + minY
            val a = alpha[oi]
            val outlineA = (OUTLINE + 1f - dist[oi]).coerceIn(0f, 1f)
            val fa = max(a, outlineA)
            if (fa <= 0.01f) { out[oi] = 0; continue }
            var cr = 255f; var cg = 255f; var cb = 255f
            if (a > 0f && sx in 0 until w && sy in 0 until h) {
                val si = sy * w + sx
                // «Баланс белого» относительно бумаги — фон внутри становится белым, цвета ярче.
                var pr = (r[si] * 255f / max(40f, bgR[si])).coerceIn(0f, 255f)
                var pg = (g[si] * 255f / max(40f, bgG[si])).coerceIn(0f, 255f)
                var pb = (b[si] * 255f / max(40f, bgB[si])).coerceIn(0f, 255f)
                // немного насыщенности
                val m = (pr + pg + pb) / 3f
                pr = (m + (pr - m) * 1.25f).coerceIn(0f, 255f)
                pg = (m + (pg - m) * 1.25f).coerceIn(0f, 255f)
                pb = (m + (pb - m) * 1.25f).coerceIn(0f, 255f)
                cr = 255f + (pr - 255f) * a
                cg = 255f + (pg - 255f) * a
                cb = 255f + (pb - 255f) * a
            }
            out[oi] = ((fa * 255).toInt() shl 24) or (cr.toInt() shl 16) or (cg.toInt() shl 8) or cb.toInt()
        }
        return Bitmap.createBitmap(out, ow, oh, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(src: FloatArray, w: Int, h: Int, rad: Int): FloatArray {
        val tmp = FloatArray(src.size)
        val dst = FloatArray(src.size)
        for (y in 0 until h) {
            var sum = 0f; var cnt = 0
            for (x in -rad..rad) if (x in 0 until w) { sum += src[y * w + x]; cnt++ }
            for (x in 0 until w) {
                tmp[y * w + x] = sum / cnt
                val out = x - rad; val inn = x + rad + 1
                if (out >= 0) { sum -= src[y * w + out]; cnt-- }
                if (inn < w) { sum += src[y * w + inn]; cnt++ }
            }
        }
        for (x in 0 until w) {
            var sum = 0f; var cnt = 0
            for (y in -rad..rad) if (y in 0 until h) { sum += tmp[y * w + x]; cnt++ }
            for (y in 0 until h) {
                dst[y * w + x] = sum / cnt
                val out = y - rad; val inn = y + rad + 1
                if (out >= 0) { sum -= tmp[out * w + x]; cnt-- }
                if (inn < h) { sum += tmp[inn * w + x]; cnt++ }
            }
        }
        return dst
    }

    private fun dilate(m: BooleanArray, w: Int, h: Int, rad: Int): BooleanArray = morph(m, w, h, rad, true)
    private fun erode(m: BooleanArray, w: Int, h: Int, rad: Int): BooleanArray = morph(m, w, h, rad, false)

    /** Квадратная морфология, разделённая на два прохода через счётчики окна. */
    private fun morph(m: BooleanArray, w: Int, h: Int, rad: Int, dilate: Boolean): BooleanArray {
        val tmp = BooleanArray(m.size)
        val dst = BooleanArray(m.size)
        for (y in 0 until h) {
            var cnt = 0; var total = 0
            for (x in -rad..rad) if (x in 0 until w) { total++; if (m[y * w + x]) cnt++ }
            for (x in 0 until w) {
                tmp[y * w + x] = if (dilate) cnt > 0 else cnt == total
                val out = x - rad; val inn = x + rad + 1
                if (out >= 0) { total--; if (m[y * w + out]) cnt-- }
                if (inn < w) { total++; if (m[y * w + inn]) cnt++ }
            }
        }
        for (x in 0 until w) {
            var cnt = 0; var total = 0
            for (y in -rad..rad) if (y in 0 until h) { total++; if (tmp[y * w + x]) cnt++ }
            for (y in 0 until h) {
                dst[y * w + x] = if (dilate) cnt > 0 else cnt == total
                val out = y - rad; val inn = y + rad + 1
                if (out >= 0) { total--; if (tmp[out * w + x]) cnt-- }
                if (inn < h) { total++; if (tmp[inn * w + x]) cnt++ }
            }
        }
        return dst
    }

    /** Всё, до чего нельзя дойти от края по фону, считается частью рисунка. */
    private fun fillHoles(m: BooleanArray, w: Int, h: Int): BooleanArray {
        val outside = BooleanArray(m.size)
        val queue = IntArray(m.size)
        var head = 0; var tail = 0
        fun push(i: Int) {
            if (!m[i] && !outside[i]) { outside[i] = true; queue[tail++] = i }
        }
        for (x in 0 until w) { push(x); push((h - 1) * w + x) }
        for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w; val y = i / w
            if (x > 0) push(i - 1)
            if (x < w - 1) push(i + 1)
            if (y > 0) push(i - w)
            if (y < h - 1) push(i + w)
        }
        return BooleanArray(m.size) { !outside[it] }
    }

    private class Component(val pixels: IntArray, val area: Int, val borderTouches: Int)

    private fun keepMainComponents(m: BooleanArray, w: Int, h: Int): BooleanArray? {
        val label = IntArray(m.size) { -1 }
        val comps = mutableListOf<Component>()
        val queue = IntArray(m.size)
        for (start in m.indices) {
            if (!m[start] || label[start] >= 0) continue
            val id = comps.size
            var head = 0; var tail = 0
            queue[tail++] = start; label[start] = id
            var border = 0
            while (head < tail) {
                val i = queue[head++]
                val x = i % w; val y = i / w
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border++
                if (x > 0 && m[i - 1] && label[i - 1] < 0) { label[i - 1] = id; queue[tail++] = i - 1 }
                if (x < w - 1 && m[i + 1] && label[i + 1] < 0) { label[i + 1] = id; queue[tail++] = i + 1 }
                if (y > 0 && m[i - w] && label[i - w] < 0) { label[i - w] = id; queue[tail++] = i - w }
                if (y < h - 1 && m[i + w] && label[i + w] < 0) { label[i + w] = id; queue[tail++] = i + w }
            }
            comps += Component(queue.copyOf(tail), tail, border)
        }
        val total = w * h
        var candidates = comps.filter { it.area >= total * 0.002f }
        if (candidates.isEmpty()) return null
        // Если есть заметный рисунок, не касающийся края, — края фото (стол, тень) выбрасываем.
        val inner = candidates.filter { it.borderTouches == 0 }
        if (inner.any { it.area >= total * 0.01f }) candidates = inner
        // Огромная область во весь кадр — это, скорее всего, не рисунок, а фон.
        candidates = candidates.filter { it.area < total * 0.97f }
        if (candidates.isEmpty()) return null
        val largest = candidates.maxOf { it.area }
        val result = BooleanArray(m.size)
        candidates.filter { it.area >= largest * 0.08f }.forEach { c -> c.pixels.forEach { result[it] = true } }
        return result
    }

    /** Приблизительное евклидово расстояние до маски (chamfer 3-4), в пикселях. */
    private fun chamferDistance(m: BooleanArray, w: Int, h: Int): FloatArray {
        val inf = 1_000_000
        val d = IntArray(m.size) { if (m[it]) 0 else inf }
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            var v = d[i]
            if (x > 0) v = min(v, d[i - 1] + 3)
            if (y > 0) {
                v = min(v, d[i - w] + 3)
                if (x > 0) v = min(v, d[i - w - 1] + 4)
                if (x < w - 1) v = min(v, d[i - w + 1] + 4)
            }
            d[i] = v
        }
        for (y in h - 1 downTo 0) for (x in w - 1 downTo 0) {
            val i = y * w + x
            var v = d[i]
            if (x < w - 1) v = min(v, d[i + 1] + 3)
            if (y < h - 1) {
                v = min(v, d[i + w] + 3)
                if (x < w - 1) v = min(v, d[i + w + 1] + 4)
                if (x > 0) v = min(v, d[i + w - 1] + 4)
            }
            d[i] = v
        }
        return FloatArray(m.size) { d[it] / 3f }
    }
}
