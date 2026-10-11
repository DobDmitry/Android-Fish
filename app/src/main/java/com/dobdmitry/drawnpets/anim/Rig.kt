package com.dobdmitry.drawnpets.anim

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Нога: вертикальная полоса снизу рисунка, которая качается вокруг «бедра». */
data class Leg(val xMin: Float, val xMax: Float, val hipY: Float, val footY: Float) {
    val cx: Float get() = (xMin + xMax) / 2
}

/** Рука (или плавник): выступ сбоку тела. side = -1 слева, +1 справа. */
data class Arm(val side: Int, val shoulderX: Float, val shoulderY: Float, val top: Float, val bottom: Float, val tipX: Float)

/** Глаз: центр, радиусы и цвет «века» (цвет кожи вокруг глаза). */
data class Eye(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val lidColor: Int)

/**
 * «Скелет» нарисованного персонажа, найденный автоматически по картинке.
 * Все координаты — в пикселях исходной картинки.
 */
data class Rig(
    val w: Int,
    val h: Int,
    val top: Float,
    val bottom: Float,
    val left: Float,
    val right: Float,
    val hipY: Float,
    val headX: Float,
    val headY: Float,
    val legs: List<Leg>,
    val arms: List<Arm>,
    val eyes: List<Eye>,
) {
    val cx: Float get() = (left + right) / 2

    companion object {
        fun empty(w: Int, h: Int) = Rig(w, h, 0f, h.toFloat(), 0f, w.toFloat(), h.toFloat(), w / 2f, 0f, emptyList(), emptyList(), emptyList())
    }
}

/** Находит у рисунка ноги, руки и глаза. Работает с ARGB-пикселями, без Android. */
object RigAnalyzer {

    private const val INK_LUM = 95

    fun analyze(px: IntArray, w: Int, h: Int): Rig {
        val n = w * h
        val solid = BooleanArray(n) { (px[it] ushr 24) > 140 }
        var top = h; var bottom = -1; var left = w; var right = -1
        var area = 0
        for (y in 0 until h) for (x in 0 until w) if (solid[y * w + x]) {
            area++
            if (y < top) top = y; if (y > bottom) bottom = y
            if (x < left) left = x; if (x > right) right = x
        }
        if (bottom < 0 || area < 50) return Rig.empty(w, h)

        // Убираем белую обводку-«наклейку», чтобы близко стоящие ноги не сливались.
        val dist = distanceToEmpty(solid, w, h)
        val outline = estimateOutline(px, solid, dist, w, h)
        val inner = BooleanArray(n) { dist[it] > outline }

        val bodyW = (right - left + 1).toFloat()
        val bodyH = (bottom - top + 1).toFloat()

        val legs = findLegs(inner, w, h, top, bottom, bodyW, bodyH, outline)
        val hipY = legs.minOfOrNull { it.hipY } ?: bottom.toFloat()
        val arms = findArms(inner, w, top, left, right, bodyW, bodyH, hipY)
        val eyes = findEyes(px, inner, w, h, top, bodyH, area)

        // Макушка — для шляпок: верхняя точка в средней трети рисунка.
        var headY = top.toFloat()
        var headX = (left + right) / 2f
        run {
            val x0 = (left + bodyW * 0.3f).toInt(); val x1 = (left + bodyW * 0.7f).toInt()
            for (y in top..bottom) {
                var s = 0; var c = 0
                for (x in x0..x1) if (solid[y * w + x]) { s += x; c++ }
                if (c > 0) { headY = y.toFloat(); headX = s / c.toFloat(); return@run }
            }
        }
        return Rig(
            w, h, top.toFloat(), bottom.toFloat() + 1, left.toFloat(), right.toFloat() + 1,
            hipY, headX, headY, legs, arms, eyes,
        )
    }

    /** Расстояние (chamfer 3-4, в пикселях) от каждого пикселя рисунка до пустоты. */
    private fun distanceToEmpty(m: BooleanArray, w: Int, h: Int): FloatArray {
        val inf = 1_000_000
        val d = IntArray(m.size) { if (m[it]) inf else 0 }
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            if (d[i] == 0) continue
            var v = d[i]
            v = min(v, if (x > 0) d[i - 1] + 3 else 3)
            v = min(v, if (y > 0) d[i - w] + 3 else 3)
            if (x > 0 && y > 0) v = min(v, d[i - w - 1] + 4)
            if (x < w - 1 && y > 0) v = min(v, d[i - w + 1] + 4)
            d[i] = v
        }
        for (y in h - 1 downTo 0) for (x in w - 1 downTo 0) {
            val i = y * w + x
            if (d[i] == 0) continue
            var v = d[i]
            v = min(v, if (x < w - 1) d[i + 1] + 3 else 3)
            v = min(v, if (y < h - 1) d[i + w] + 3 else 3)
            if (x < w - 1 && y < h - 1) v = min(v, d[i + w + 1] + 4)
            if (x > 0 && y < h - 1) v = min(v, d[i + w - 1] + 4)
            d[i] = v
        }
        return FloatArray(m.size) { d[it] / 3f }
    }

    /** Толщина белой обводки: насколько глубоко от края тянутся почти белые пиксели. */
    private fun estimateOutline(px: IntArray, solid: BooleanArray, dist: FloatArray, w: Int, h: Int): Float {
        val hist = IntArray(32)
        var samples = 0
        for (i in px.indices) {
            if (!solid[i]) continue
            val d = dist[i]
            if (d < 1f || d > 30f) continue
            val c = px[i]
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            if (r > 235 && g > 235 && b > 235) hist[d.toInt().coerceAtMost(31)]++
            samples++
        }
        if (samples == 0) return 2f
        // Обводка — это непрерывный «белый слой» у края: ищем, где доля белого резко падает.
        var depth = 1
        val base = max(1, hist[1])
        while (depth < 20 && hist[depth + 1] > base * 0.35f) depth++
        return (depth + 1f).coerceIn(2f, 16f)
    }

    private class Run(val x0: Int, val x1: Int) {
        val c get() = (x0 + x1) / 2f
        val width get() = x1 - x0 + 1
    }

    private fun runs(m: BooleanArray, w: Int, y: Int, minGap: Int, minWidth: Int): List<Run> {
        val out = mutableListOf<Run>()
        var x = 0
        var start = -1
        var lastOn = -10_000
        while (x < w) {
            if (m[y * w + x]) {
                if (start < 0) start = x
                else if (x - lastOn > minGap) {
                    out += Run(start, lastOn); start = x
                }
                lastOn = x
            }
            x++
        }
        if (start >= 0) out += Run(start, lastOn)
        return out.filter { it.width >= minWidth }
    }

    private fun findLegs(m: BooleanArray, w: Int, h: Int, top: Int, bottom: Int, bodyW: Float, bodyH: Float, outline: Float): List<Leg> {
        val minGap = max(2, (bodyW * 0.02f).toInt())
        val minWidth = max(2, (bodyW * 0.025f).toInt())
        // Ищем самую нижнюю строку «внутренней» маски.
        var innerBottom = bottom
        while (innerBottom > top && (0 until w).none { m[innerBottom * w + it] }) innerBottom--
        val sampleY = (innerBottom - bodyH * 0.05f).toInt().coerceAtLeast(top)
        val feet = runs(m, w, sampleY, minGap, minWidth).sortedByDescending { it.width }.take(4).sortedBy { it.x0 }
        if (feet.size < 2) return emptyList()

        // Поднимаемся вверх, пока ноги не сольются в одно тело.
        var hipY = sampleY
        val maxLeg = (bodyH * 0.6f).toInt()
        val xMin = IntArray(feet.size) { feet[it].x0 }
        val xMax = IntArray(feet.size) { feet[it].x1 }
        var y = sampleY
        while (y > top && sampleY - y < maxLeg) {
            val rs = runs(m, w, y, minGap, minWidth)
            // Сколько ног ещё видно по отдельности?
            val matched = feet.indices.map { i -> rs.filter { r -> r.x1 >= xMin[i] - minGap && r.x0 <= xMax[i] + minGap } }
            val distinct = matched.map { it.firstOrNull() }.toSet()
            if (matched.any { it.isEmpty() } || distinct.size < feet.size) break
            feet.indices.forEach { i ->
                val r = matched[i].first()
                // ноги могут чуть расширяться к телу, но не больше чем вдвое
                if (r.width < feet[i].width * 2.5f) {
                    xMin[i] = min(xMin[i], r.x0); xMax[i] = max(xMax[i], r.x1)
                }
            }
            hipY = y
            y--
        }
        val legLen = innerBottom - hipY
        if (legLen < bodyH * 0.08f) return emptyList()
        // Ноги примерно одинаковые и не слишком широкие (иначе это хвост и брюхо рыбы и т.п.).
        val widths = feet.indices.map { xMax[it] - xMin[it] + 1 }
        if (widths.max() > bodyW * 0.42f || widths.max() > widths.min() * 3.2f) return emptyList()
        val footY = bottom.toFloat() + 1
        val hip = hipY.toFloat() + legLen * 0.08f
        return feet.indices.map { i ->
            Leg(xMin[i] - outline, xMax[i] + outline, hip, footY)
        }
    }

    /**
     * Рука — тонкий выступ сбоку: в столбцах за пределами туловища рисунок
     * занимает по высоте мало места. У круглого тела такого нет.
     */
    private fun findArms(m: BooleanArray, w: Int, top: Int, left: Int, right: Int, bodyW: Float, bodyH: Float, hipY: Float): List<Arm> {
        val y0 = (top + bodyH * 0.12f).toInt()
        val y1 = (hipY - bodyH * 0.03f).toInt()
        if (y1 - y0 < 6) return emptyList()
        val cover = IntArray(w)
        for (x in 0 until w) {
            var c = 0
            for (y in y0..y1) if (m[y * w + x]) c++
            cover[x] = c
        }
        val thin = bodyH * 0.24f
        val out = mutableListOf<Arm>()
        for (side in intArrayOf(-1, 1)) {
            // крайний столбец «внутренней» маски с этой стороны
            var edge = if (side < 0) left else right
            while (edge in 0 until w && cover[edge] == 0 && (if (side < 0) edge < right else edge > left)) edge -= side
            var x = edge
            var steps = 0
            while (x in 0 until w && cover[x] in 1 until thin.toInt().coerceAtLeast(2)) { x -= side; steps++ }
            // небольшие разрывы (тонкая линия руки) не должны обрывать поиск
            if (x in 0 until w && cover[x] == 0) continue
            val len = steps.toFloat()
            if (len < bodyW * 0.07f || len > bodyW * 0.6f) continue
            val shoulderX = x.toFloat()
            var aTop = Int.MAX_VALUE; var aBottom = -1
            var sy = 0f; var sc = 0
            // Границы руки берём по внешним 70% её длины — ближе к плечу там ещё туловище.
            var cx = edge
            while (cx != x) {
                val fromShoulder = abs(cx - x) / len
                for (y in y0..y1) if (m[y * w + cx]) {
                    if (fromShoulder > 0.3f) { if (y < aTop) aTop = y; if (y > aBottom) aBottom = y }
                    if (fromShoulder in 0.3f..0.6f) { sy += y; sc++ }
                }
                cx -= side
            }
            if (aBottom < 0 || sc == 0) continue
            out += Arm(side, shoulderX, sy / sc, aTop.toFloat(), aBottom.toFloat(), edge.toFloat())
        }
        return out
    }

    private class Blob(val pixels: IntArray, val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
        val area get() = pixels.size
        val bw get() = x1 - x0 + 1
        val bh get() = y1 - y0 + 1
        val cx get() = (x0 + x1) / 2f
        val cy get() = (y0 + y1) / 2f
        val fill get() = area / (bw * bh).toFloat()
    }

    private fun lum(c: Int): Int {
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun findEyes(px: IntArray, inner: BooleanArray, w: Int, h: Int, top: Int, bodyH: Float, area: Int): List<Eye> {
        val yLimit = (top + bodyH * 0.7f).toInt().coerceAtMost(h - 1)
        val dark = BooleanArray(w * h)
        for (y in top..yLimit) for (x in 0 until w) {
            val i = y * w + x
            if (inner[i] && lum(px[i]) < INK_LUM) dark[i] = true
        }
        val seen = BooleanArray(w * h)
        val queue = IntArray(w * h)
        val blobs = mutableListOf<Blob>()
        for (s in dark.indices) {
            if (!dark[s] || seen[s]) continue
            var head = 0; var tail = 0
            queue[tail++] = s; seen[s] = true
            var x0 = w; var y0 = h; var x1 = 0; var y1 = 0
            while (head < tail) {
                val i = queue[head++]
                val x = i % w; val y = i / w
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
                if (x > 0 && dark[i - 1] && !seen[i - 1]) { seen[i - 1] = true; queue[tail++] = i - 1 }
                if (x < w - 1 && dark[i + 1] && !seen[i + 1]) { seen[i + 1] = true; queue[tail++] = i + 1 }
                if (y > 0 && dark[i - w] && !seen[i - w]) { seen[i - w] = true; queue[tail++] = i - w }
                if (y < h - 1 && dark[i + w] && !seen[i + w]) { seen[i + w] = true; queue[tail++] = i + w }
            }
            if (tail > area * 0.04f) continue
            blobs += Blob(queue.copyOf(tail), x0, y0, x1, y1)
        }
        val cands = blobs.filter { b ->
            b.area >= max(6f, area * 0.0003f) && b.bw >= 3 && b.bh >= 3 &&
                b.bw.toFloat() / b.bh in 0.33f..3f && b.fill >= 0.38f
        }
        if (cands.isEmpty()) return emptyList()

        var best: Pair<Blob, Blob>? = null
        var bestScore = 0f
        for (i in cands.indices) for (j in i + 1 until cands.size) {
            val a = cands[i]; val b = cands[j]
            val ratio = max(a.area, b.area) / min(a.area, b.area).toFloat()
            if (ratio > 3f) continue
            val size = (a.bw + a.bh + b.bw + b.bh) / 4f
            val dy = abs(a.cy - b.cy)
            val dx = abs(a.cx - b.cx)
            if (dy > size * 1.2f || dx < size * 1.3f || dx > size * 10f) continue
            val height = 1f + (1f - ((a.cy + b.cy) / 2 - top) / bodyH)
            val score = (a.area + b.area) * height * (1f - dy / (size * 2.4f)) / ratio
            if (score > bestScore) { bestScore = score; best = a to b }
        }
        val chosen = best?.let { listOf(it.first, it.second) }
            ?: cands.filter { it.cy < top + bodyH * 0.5f && it.fill > 0.5f }.maxByOrNull { it.area }?.let { listOf(it) }
            ?: return emptyList()
        return chosen.sortedBy { it.cx }.map { makeEye(it, px, inner, w, h) }
    }

    /** Если зрачок нарисован внутри белого кружка — берём размер всего кружка. */
    private fun makeEye(b: Blob, px: IntArray, inner: BooleanArray, w: Int, h: Int): Eye {
        val pr = max(b.bw, b.bh) / 2f
        val cx = b.cx; val cy = b.cy
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to 1, 1 to -1, -1 to -1)
        val reach = mutableListOf<Float>()
        for ((dx, dy) in dirs) {
            val step = if (dx != 0 && dy != 0) 1.414f else 1f
            var k = (pr + 1).toInt()
            var inWhite = false
            var found = -1f
            while (k < pr * 4 + 6) {
                val x = (cx + dx * k / step).toInt(); val y = (cy + dy * k / step).toInt()
                if (x !in 0 until w || y !in 0 until h || !inner[y * w + x]) break
                val l = lum(px[y * w + x])
                if (l > 200) inWhite = true
                else if (inWhite && l < INK_LUM + 20) { found = k.toFloat(); break }
                k++
            }
            if (found > 0) reach += found
        }
        val r = if (reach.size >= 5) reach.sorted()[reach.size / 2] + 1.5f else pr + 2f
        val rx = max(r, b.bw / 2f + 2f)
        val ry = max(r, b.bh / 2f + 2f)
        // Цвет «века» — средний светлый цвет вокруг глаза.
        var sr = 0L; var sg = 0L; var sb = 0L; var cnt = 0
        val ring0 = max(rx, ry) * 1.25f
        val ring1 = max(rx, ry) * 1.6f + 2
        val x0 = (cx - ring1).toInt().coerceAtLeast(0); val x1 = (cx + ring1).toInt().coerceAtMost(w - 1)
        val y0 = (cy - ring1).toInt().coerceAtLeast(0); val y1 = (cy + ring1).toInt().coerceAtMost(h - 1)
        for (y in y0..y1) for (x in x0..x1) {
            val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
            if (d < ring0 || d > ring1) continue
            val i = y * w + x
            if (!inner[i]) continue
            val c = px[i]
            if (lum(c) < INK_LUM + 30) continue
            sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF; cnt++
        }
        val lid = if (cnt > 0) {
            (0xFF shl 24) or ((sr / cnt).toInt() shl 16) or ((sg / cnt).toInt() shl 8) or (sb / cnt).toInt()
        } else 0xFFFFFFFF.toInt()
        return Eye(cx, cy, rx, ry, lid)
    }
}
