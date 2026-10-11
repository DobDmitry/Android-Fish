package com.dobdmitry.drawnpets.anim

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class LimbKind { LEG, ARM, ANTENNA }

/**
 * Конечность: тонкая часть рисунка, прикреплённая к телу (рука, нога, щупальце,
 * рожка, хвост). Качается вокруг точки крепления (ax, ay); (tx, ty) — кончик.
 * [order] — порядковый номер среди конечностей того же типа слева направо.
 */
data class Limb(
    val kind: LimbKind,
    val ax: Float,
    val ay: Float,
    val tx: Float,
    val ty: Float,
    val side: Int,
    val order: Int,
) {
    val length: Float get() = kotlin.math.hypot(tx - ax, ty - ay)
}

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
    val limbs: List<Limb>,
    val eyes: List<Eye>,
    /** Для каждой вершины сетки — номер конечности, к которой она относится (-1 — тело). */
    val vertexLimb: IntArray,
    /** Для каждой вершины — насколько она далеко от крепления конечности (0..1). */
    val vertexAlong: FloatArray,
) {
    val cx: Float get() = (left + right) / 2
    val legs: List<Limb> get() = limbs.filter { it.kind == LimbKind.LEG }

    companion object {
        fun empty(w: Int, h: Int) = Rig(
            w, h, 0f, h.toFloat(), 0f, w.toFloat(), h.toFloat(), w / 2f, 0f, emptyList(), emptyList(),
            IntArray(Animator.VERTS) { -1 }, FloatArray(Animator.VERTS),
        )
    }
}

/** Находит у рисунка конечности и глаза. Работает с ARGB-пикселями, без Android. */
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

        val eyes = findEyes(px, inner, w, h, top, bodyH, bodyW, area)
        val limbMap = IntArray(n) { -1 }
        val limbs = findLimbs(inner, limbMap, w, h, top, bodyW, bodyH, eyes)
        val legs = limbs.filter { it.kind == LimbKind.LEG }
        val hipY = legs.minOfOrNull { it.ay } ?: bottom.toFloat()
        val (vLimb, vAlong) = assignVertices(inner, limbMap, limbs, w, h)

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
            hipY, headX, headY, limbs, eyes, vLimb, vAlong,
        )
    }

    /** Расстояние (chamfer 3-4, в пикселях) от каждого пикселя рисунка до пустоты. */
    internal fun distanceToEmpty(m: BooleanArray, w: Int, h: Int, outsideEmpty: Boolean = true): FloatArray {
        val inf = 1_000_000
        val edge = if (outsideEmpty) 3 else inf
        val d = IntArray(m.size) { if (m[it]) inf else 0 }
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            if (d[i] == 0) continue
            var v = d[i]
            v = min(v, if (x > 0) d[i - 1] + 3 else edge)
            v = min(v, if (y > 0) d[i - w] + 3 else edge)
            if (x > 0 && y > 0) v = min(v, d[i - w - 1] + 4)
            if (x < w - 1 && y > 0) v = min(v, d[i - w + 1] + 4)
            d[i] = v
        }
        for (y in h - 1 downTo 0) for (x in w - 1 downTo 0) {
            val i = y * w + x
            if (d[i] == 0) continue
            var v = d[i]
            v = min(v, if (x < w - 1) d[i + 1] + 3 else edge)
            v = min(v, if (y < h - 1) d[i + w] + 3 else edge)
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

    /** Морфологическое «открытие»: что останется от маски, если стереть всё тоньше 2·r. */
    internal fun opening(m: BooleanArray, w: Int, h: Int, r: Float): BooleanArray {
        val d = distanceToEmpty(m, w, h)
        val core = BooleanArray(m.size) { m[it] && d[it] > r }
        if (core.none { it }) return core
        val notCore = BooleanArray(m.size) { !core[it] }
        val toCore = distanceToEmpty(notCore, w, h, outsideEmpty = false)
        return BooleanArray(m.size) { m[it] && (core[it] || toCore[it] <= r + 1.5f) }
    }

    private fun components(m: BooleanArray, w: Int, h: Int): List<IntArray> {
        val seen = BooleanArray(m.size)
        val queue = IntArray(m.size)
        val out = mutableListOf<IntArray>()
        for (s in m.indices) {
            if (!m[s] || seen[s]) continue
            var head = 0; var tail = 0
            queue[tail++] = s; seen[s] = true
            while (head < tail) {
                val i = queue[head++]
                val x = i % w; val y = i / w
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val j = ny * w + nx
                    if (m[j] && !seen[j]) { seen[j] = true; queue[tail++] = j }
                }
            }
            out += queue.copyOf(tail)
        }
        return out
    }

    private class Candidate(val pixels: IntArray, val ax: Float, val ay: Float, val tx: Float, val ty: Float)

    /**
     * Тонкие части рисунка, прикреплённые к толстому «телу». Если тонкая часть
     * крепится широко (как нога улитки), смотрим уже её тонкие отростки (рожки).
     */
    private fun limbCandidates(region: BooleanArray, body: BooleanArray, w: Int, h: Int, minLen: Float, minArea: Int, depth: Int): List<Candidate> {
        val thin = BooleanArray(region.size) { region[it] && !body[it] }
        val out = mutableListOf<Candidate>()
        for (comp in components(thin, w, h)) {
            if (comp.size < minArea) continue
            var sx = 0f; var sy = 0f; var cnt = 0
            var cx0 = w; var cy0 = h; var cx1 = 0; var cy1 = 0
            for (i in comp) {
                val x = i % w; val y = i / w
                var touches = false
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx; val ny = y + dy
                    if (nx in 0 until w && ny in 0 until h && body[ny * w + nx]) touches = true
                }
                if (touches) {
                    sx += x; sy += y; cnt++
                    if (x < cx0) cx0 = x; if (x > cx1) cx1 = x; if (y < cy0) cy0 = y; if (y > cy1) cy1 = y
                }
            }
            if (cnt == 0) continue // оторванный кусочек — не двигаем
            val ax = sx / cnt; val ay = sy / cnt
            var best = comp[0]; var bestD = -1f
            for (i in comp) {
                val dx = i % w - ax; val dy = i / w - ay
                val d = dx * dx + dy * dy
                if (d > bestD) { bestD = d; best = i }
            }
            val len = sqrt(bestD)
            val contact = max(cx1 - cx0, cy1 - cy0).toFloat()
            if (contact > len * 0.75f || len < minLen) {
                // Широкое крепление: это скорее часть тела. Ищем на ней тонкие отростки.
                if (depth == 0 && len >= minLen) {
                    val sub = BooleanArray(region.size)
                    for (i in comp) sub[i] = true
                    val d = distanceToEmpty(sub, w, h)
                    var md = 0f
                    for (i in comp) md = max(md, d[i])
                    val subBody = opening(sub, w, h, max(1.5f, md * 0.45f))
                    val body2 = BooleanArray(region.size) { body[it] || subBody[it] }
                    out += limbCandidates(sub, body2, w, h, minLen * 0.8f, minArea, depth + 1)
                }
                continue
            }
            out += Candidate(comp, ax, ay, (best % w).toFloat(), (best / w).toFloat())
        }
        return out
    }

    /**
     * Если конечность внизу расходится на несколько «ветвей» (щупальца медузы,
     * соединённые оборкой), делим её на отдельные конечности.
     */
    private fun splitBranches(c: Candidate, w: Int): List<Candidate> {
        val dy = c.ty - c.ay
        val len = kotlin.math.hypot(c.tx - c.ax, dy)
        if (len < 20f || dy < len * 0.6f) return listOf(c)
        var y0 = Int.MAX_VALUE; var y1 = Int.MIN_VALUE
        for (i in c.pixels) { val y = i / w; if (y < y0) y0 = y; if (y > y1) y1 = y }
        val rows = HashMap<Int, MutableList<Int>>()
        for (i in c.pixels) rows.getOrPut(i / w) { mutableListOf() } += i % w
        fun runsAt(y: Int): List<Float> {
            val xs = rows[y]?.sorted() ?: return emptyList()
            val out = mutableListOf<Float>()
            var start = xs[0]; var prev = xs[0]
            for (k in 1 until xs.size) {
                if (xs[k] - prev > 3) { out += (start + prev) / 2f; start = xs[k] }
                prev = xs[k]
            }
            out += (start + prev) / 2f
            return out
        }
        // Ищем строку, где ветви уже разошлись
        val startRow = (y0 + (y1 - y0) * 0.35f).toInt()
        val counts = (startRow..y1).map { runsAt(it).size }.sorted()
        val typical = counts[counts.size / 2]
        if (typical < 2) return listOf(c)
        var row = startRow
        while (row < y1 && runsAt(row).size != typical) row++
        var centers = runsAt(row).toMutableList()
        if (centers.size < 2) return listOf(c)
        // Проводим каждую ветвь вниз, следуя за ближайшим отрезком
        val trackX = HashMap<Int, List<Float>>()
        trackX[row] = centers.toList()
        for (y in row + 1..y1) {
            val rs = runsAt(y)
            if (rs.size == centers.size) centers = rs.toMutableList()
            else if (rs.isNotEmpty()) centers = centers.map { cx -> rs.minByOrNull { abs(it - cx) }!!.let { r -> if (abs(r - cx) < 12f) r else cx } }.toMutableList()
            trackX[y] = centers.toList()
        }
        val first = trackX[row]!!
        val parts = List(first.size) { mutableListOf<Int>() }
        for (i in c.pixels) {
            val x = i % w; val y = i / w
            val cs = trackX[y] ?: first
            var best = 0
            for (k in cs.indices) if (abs(cs[k] - x) < abs(cs[best] - x)) best = k
            parts[best] += i
        }
        return parts.filter { it.size > c.pixels.size / (first.size * 4) }.map { p ->
            var minY = Int.MAX_VALUE; var maxI = p[0]
            for (i in p) { if (i / w < minY) minY = i / w; if (i / w > maxI / w) maxI = i }
            val attachY = max(minY.toFloat(), c.ay)
            val topXs = p.filter { abs(it / w - attachY) <= 3 }.map { it % w }.ifEmpty { p.map { it % w } }
            Candidate(p.toIntArray(), topXs.average().toFloat(), attachY, (maxI % w).toFloat(), (maxI / w).toFloat())
        }
    }

    private fun findLimbs(
        inner: BooleanArray, limbMap: IntArray, w: Int, h: Int, top: Int,
        bodyW: Float, bodyH: Float, eyes: List<Eye>,
    ): List<Limb> {
        val dist = distanceToEmpty(inner, w, h)
        var maxD = 0f
        var innerArea = 0
        for (i in inner.indices) if (inner[i]) { innerArea++; if (dist[i] > maxD) maxD = dist[i] }
        if (innerArea < 50) return emptyList()
        val body = opening(inner, w, h, max(2.5f, maxD * 0.33f))
        val minLen = max(bodyH, bodyW) * 0.07f
        val minArea = max(12, (innerArea * 0.0015f).toInt())
        var cands = limbCandidates(inner, body, w, h, minLen, minArea, 0)
        // Кусок с глазом — это голова, а не рука.
        cands = cands.filter { c ->
            eyes.none { e -> c.pixels.any { i -> abs(i % w - e.cx) < e.rx && abs(i / w - e.cy) < e.ry } }
        }
        cands = cands.flatMap { splitBranches(it, w) }
        cands = cands.sortedByDescending { it.pixels.size }.take(10)
        val bodyCx = run {
            var s = 0L; var c = 0
            for (i in body.indices) if (body[i]) { s += i % w; c++ }
            if (c > 0) s / c.toFloat() else w / 2f
        }
        val raw = cands.map { c ->
            val dx = c.tx - c.ax; val dy = c.ty - c.ay
            val len = max(1f, sqrt(dx * dx + dy * dy))
            val kind = when {
                dy / len > 0.55f && c.ay > top + bodyH * 0.42f -> LimbKind.LEG
                dy / len < -0.8f -> LimbKind.ANTENNA
                else -> LimbKind.ARM
            }
            val side = when {
                abs(dx) > len * 0.2f -> if (dx < 0) -1 else 1
                else -> if (c.ax < bodyCx) -1 else 1
            }
            Triple(c, kind, side)
        }
        val limbs = mutableListOf<Limb>()
        for (kind in LimbKind.entries) {
            raw.filter { it.second == kind }.sortedBy { it.first.ax }.forEachIndexed { order, (c, k, side) ->
                for (i in c.pixels) limbMap[i] = limbs.size
                limbs += Limb(k, c.ax, c.ay, c.tx, c.ty, side, order)
            }
        }
        return limbs
    }

    /** Каждой вершине сетки — ближайшая часть рисунка (тело или конечность). */
    private fun assignVertices(inner: BooleanArray, limbMap: IntArray, limbs: List<Limb>, w: Int, h: Int): Pair<IntArray, FloatArray> {
        val n = w * h
        val label = IntArray(n) { -1 }
        val steps = IntArray(n) { Int.MAX_VALUE }
        val queue = IntArray(n)
        var head = 0; var tail = 0
        for (i in 0 until n) if (inner[i]) {
            label[i] = if (limbMap[i] >= 0) limbMap[i] else -2
            steps[i] = 0
            queue[tail++] = i
        }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w; val y = i / w
            val s = steps[i] + 1
            if (x > 0 && steps[i - 1] > s) { steps[i - 1] = s; label[i - 1] = label[i]; queue[tail++] = i - 1 }
            if (x < w - 1 && steps[i + 1] > s) { steps[i + 1] = s; label[i + 1] = label[i]; queue[tail++] = i + 1 }
            if (y > 0 && steps[i - w] > s) { steps[i - w] = s; label[i - w] = label[i]; queue[tail++] = i - w }
            if (y < h - 1 && steps[i + w] > s) { steps[i + w] = s; label[i + w] = label[i]; queue[tail++] = i + w }
        }
        val gx = Animator.GX; val gy = Animator.GY
        val reach = (w.toFloat() / gx + h.toFloat() / gy) * 1.2f
        val vLimb = IntArray(Animator.VERTS) { -1 }
        val vAlong = FloatArray(Animator.VERTS)
        var k = 0
        for (j in 0..gy) for (i in 0..gx) {
            val x = (w.toFloat() * i / gx).toInt().coerceIn(0, w - 1)
            val y = (h.toFloat() * j / gy).toInt().coerceIn(0, h - 1)
            val idx = y * w + x
            val l = label[idx]
            if (l >= 0 && steps[idx] <= reach) {
                val limb = limbs[l]
                vLimb[k] = l
                val d = kotlin.math.hypot(x - limb.ax, y - limb.ay)
                vAlong[k] = (d / max(1f, limb.length)).coerceIn(0f, 1.2f)
            }
            k++
        }
        return vLimb to vAlong
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

    private fun findEyes(px: IntArray, inner: BooleanArray, w: Int, h: Int, top: Int, bodyH: Float, bodyW: Float, area: Int): List<Eye> {
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
            ?: cands.filter { it.cy < top + bodyH * 0.5f && it.fill > 0.5f && hasWhiteAround(it, px, inner, w, h) }
                .maxByOrNull { it.area }?.let { listOf(it) }
            ?: return emptyList()
        val maxR = max(5f, min(bodyH * 0.09f, bodyW * 0.13f))
        val made = chosen.sortedBy { it.cx }.map { makeEye(it, px, inner, w, h) }
            .map { it.copy(rx = it.rx.coerceAtMost(maxR), ry = it.ry.coerceAtMost(maxR)) }
        if (made.size < 2) return made
        // Глаза обычно рисуют одинаковыми — берём больший размер для обоих.
        val rx = made.maxOf { it.rx }; val ry = made.maxOf { it.ry }
        return made.map { it.copy(rx = rx, ry = ry) }
    }

    /** Одиночный глаз засчитываем, только если вокруг зрачка есть белок (иначе это просто точка). */
    private fun hasWhiteAround(b: Blob, px: IntArray, inner: BooleanArray, w: Int, h: Int): Boolean {
        val r = max(b.bw, b.bh) / 2f + 3f
        var white = 0; var total = 0
        for (k in 0 until 16) {
            val a = k * Math.PI / 8
            val x = (b.cx + kotlin.math.cos(a) * r).toInt(); val y = (b.cy + kotlin.math.sin(a) * r).toInt()
            if (x !in 0 until w || y !in 0 until h || !inner[y * w + x]) continue
            total++
            if (lum(px[y * w + x]) > 200) white++
        }
        return total >= 8 && white >= total * 0.6f
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
        val r = (if (reach.size >= 5) reach.sorted()[reach.size / 2] + 1.5f else pr + 2f).coerceAtMost(pr * 2.6f + 3f)
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
