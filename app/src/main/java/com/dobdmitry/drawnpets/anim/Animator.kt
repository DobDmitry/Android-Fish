package com.dobdmitry.drawnpets.anim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Что сейчас делает персонаж. */
enum class Action { IDLE, WALK, RUN, JUMP, HAPPY, WAVE, EAT, TALK, SLEEP, SAD, SWIM }

/** Выражение глаз поверх рисунка. */
enum class EyeState { OPEN, BLINK, HAPPY, SLEEP }

/**
 * Превращает «скелет» рисунка в сетку для деформации (drawBitmapMesh).
 * Чистый Kotlin — можно проверять без телефона.
 */
class Animator(val rig: Rig) {
    companion object {
        const val GX = 20
        const val GY = 20
        const val VERTS = (GX + 1) * (GY + 1)
    }

    var action = Action.IDLE
    /** Общее время, сек. */
    var t = 0f
    /** Время с начала текущего действия, сек. */
    var actionT = 0f
    var seed = 0

    private val w = rig.w.toFloat()
    private val h = rig.h.toFloat()
    private val bodyH = max(1f, rig.bottom - rig.top)
    private val bodyW = max(1f, rig.right - rig.left)

    // Параметры позы, вычисляются один раз на кадр
    private var legAngles = FloatArray(rig.legs.size)
    private var legLift = FloatArray(rig.legs.size)
    private var armAngle = FloatArray(rig.arms.size)
    private var breath = 0f
    private var sway = 0f
    private var squash = 0f
    private var droop = 0f
    private var swim = 0f
    private var waddle = 0f

    private val phase: Float get() = t * (if (action == Action.RUN) 13f else 8.5f) + seed * 0.7f

    /** Пересчитать позу для текущих action/t/actionT. */
    fun update() {
        val p = phase
        val s = seed * 0.37f
        breath = when (action) {
            Action.SLEEP -> 0.04f * sin(t * 1.5f + s)
            Action.WALK, Action.RUN, Action.SWIM -> 0.01f * sin(t * 3f + s)
            else -> 0.025f * sin(t * 2.4f + s)
        }
        sway = when (action) {
            Action.WALK -> sin(p * 2f) * 0.018f
            Action.RUN -> sin(p * 2f) * 0.025f
            Action.HAPPY -> sin(t * 13f) * 0.035f
            Action.TALK -> sin(t * 4f) * 0.012f
            Action.IDLE -> sin(t * 1.1f + s) * 0.008f
            else -> 0f
        }
        squash = when (action) {
            Action.EAT -> 0.07f * abs(sin(actionT * 11f))
            Action.TALK -> 0.02f * abs(sin(t * 9f))
            else -> 0f
        }
        droop = when (action) {
            Action.SAD -> 0.05f
            Action.SLEEP -> 0.03f
            else -> 0f
        }
        swim = if (action == Action.SWIM) 1f else 0f
        // Без ног персонаж ходит вразвалочку: низ качается из стороны в сторону.
        waddle = if (rig.legs.isEmpty()) when (action) {
            Action.WALK -> sin(p) * 0.08f
            Action.RUN -> sin(p) * 0.1f
            Action.HAPPY -> sin(t * 12f) * 0.04f
            else -> 0f
        } else 0f

        for (i in rig.legs.indices) {
            val opposite = if (i % 2 == 0) 0f else PI.toFloat()
            legAngles[i] = when (action) {
                Action.WALK -> sin(p + opposite) * 0.42f
                Action.RUN -> sin(p + opposite) * 0.6f
                Action.HAPPY -> sin(t * 12f + opposite) * 0.18f
                Action.JUMP -> if (i % 2 == 0) 0.28f else -0.28f
                Action.SWIM -> sin(t * 7f + opposite) * 0.35f
                else -> 0f
            }
            legLift[i] = when (action) {
                Action.WALK, Action.RUN -> max(0f, -cos(p + opposite)) * (if (action == Action.RUN) 0.2f else 0.13f)
                Action.HAPPY -> max(0f, sin(t * 12f + opposite)) * 0.08f
                else -> 0f
            }
        }
        for (i in rig.arms.indices) {
            val arm = rig.arms[i]
            val left = arm.side < 0
            val off = if (left) 0f else PI.toFloat()
            // «Поднять руку» — положительный угол
            armAngle[i] = when (action) {
                Action.IDLE -> 0.12f * sin(t * 1.6f + off + s)
                Action.WALK -> 0.38f * sin(p + off)
                Action.RUN -> 0.6f * sin(p + off)
                Action.HAPPY -> 1.25f + 0.3f * sin(t * 14f + off)
                Action.WAVE -> if (!left || rig.arms.size == 1) 1.15f + 0.45f * sin(actionT * 11f) else 0.15f
                Action.JUMP -> 1.2f
                Action.EAT -> 0.45f + 0.2f * sin(actionT * 11f)
                Action.TALK -> 0.25f + 0.2f * sin(t * 5f + off)
                Action.SLEEP -> -0.2f
                Action.SAD -> -0.35f
                Action.SWIM -> 0.6f * sin(t * 9f + off * 0.5f)
            }
        }
    }

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val k = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return k * k * (3 - 2 * k)
    }

    /** Деформация одной точки картинки. Результат пишется в out[0], out[1]. */
    fun map(x0: Float, y0: Float, out: FloatArray) {
        var x = x0
        var y = y0

        // 1. Ноги — поворот вокруг бедра ближайшей ноги.
        if (rig.legs.isNotEmpty() && y > rig.hipY - bodyH * 0.02f) {
            var best = -1
            var bestD = Float.MAX_VALUE
            for (i in rig.legs.indices) {
                val l = rig.legs[i]
                val d = if (x < l.xMin) l.xMin - x else if (x > l.xMax) x - l.xMax else 0f
                if (d < bestD) { bestD = d; best = i }
            }
            val leg = rig.legs[best]
            val legW = leg.xMax - leg.xMin
            val wx = 1f - smooth(0f, legW * 0.6f + 4f, bestD)
            val len = max(1f, leg.footY - leg.hipY)
            val depth = (y - leg.hipY) / len
            val wy = smooth(-0.05f, 0.3f, depth)
            val a = legAngles[best] * wx * wy
            if (a != 0f) {
                val dx = x - leg.cx
                val dy = y - leg.hipY
                val c = cos(a); val sn = sin(a)
                x = leg.cx + dx * c - dy * sn
                y = leg.hipY + dx * sn + dy * c
            }
            y -= legLift[best] * len * wx * depth.coerceIn(0f, 1f)
        }

        // 2. Руки — поворот вокруг плеча.
        for (i in rig.arms.indices) {
            val arm = rig.arms[i]
            val span = abs(arm.tipX - arm.shoulderX)
            if (span < 1f) continue
            val out01 = if (arm.side < 0) (arm.shoulderX - x0) / span else (x0 - arm.shoulderX) / span
            if (out01 <= -0.05f) continue
            val margin = (arm.bottom - arm.top) * 0.35f + bodyH * 0.04f
            val wy = smooth(arm.top - margin, arm.top, y0) * (1f - smooth(arm.bottom, arm.bottom + margin, y0))
            val wx = smooth(0.02f, 0.4f, out01)
            val angle = armAngle[i] * wx * wy * (if (arm.side < 0) 1f else -1f)
            if (angle == 0f) continue
            val dx = x - arm.shoulderX
            val dy = y - arm.shoulderY
            val c = cos(angle); val sn = sin(angle)
            x = arm.shoulderX + dx * c - dy * sn
            y = arm.shoulderY + dx * sn + dy * c
        }

        // 3. Тело: дыхание, покачивание, «чавканье», грусть — всё выше бёдер.
        val hip = rig.hipY
        val up = ((hip - y0) / bodyH).coerceIn(0f, 1.2f)
        if (up > 0f) {
            val k = breath - squash
            y = hip - (hip - y) * (1f + k)
            x = rig.cx + (x - rig.cx) * (1f - k * 0.6f + squash * 0.5f)
            x += sway * bodyW * up
            y += droop * bodyH * up * up
        }

        if (waddle != 0f) {
            val down = ((y0 - rig.top) / bodyH).coerceIn(0f, 1f)
            x += waddle * bodyW * (down - 0.5f)
            y -= abs(waddle) * bodyH * 0.25f * down * down
        }

        // 4. Плавание: волна от головы (справа) к хвосту (слева).
        if (swim > 0f) {
            val u = ((x0 - rig.left) / bodyW).coerceIn(0f, 1f)
            val tail = 1f - u
            y += sin(u * 5.5f - t * 6f) * bodyH * 0.07f * (0.25f + tail)
            x += sin(t * 6f) * bodyW * 0.015f * tail
        }
        out[0] = x
        out[1] = y
    }

    private val tmp = FloatArray(2)

    /** Заполняет массив вершин сетки (GX+1)×(GY+1) для drawBitmapMesh. */
    fun fillMesh(verts: FloatArray) {
        var k = 0
        for (j in 0..GY) {
            val y = h * j / GY
            for (i in 0..GX) {
                val x = w * i / GX
                map(x, y, tmp)
                verts[k++] = tmp[0]
                verts[k++] = tmp[1]
            }
        }
    }

    fun eyeState(): EyeState = when (action) {
        Action.SLEEP -> EyeState.SLEEP
        Action.HAPPY, Action.WAVE, Action.JUMP -> EyeState.HAPPY
        else -> {
            val period = 3.3f + (abs(seed) % 7) * 0.25f
            if (((t + abs(seed) % 5) % period) < 0.13f) EyeState.BLINK else EyeState.OPEN
        }
    }

    /** Насколько открыт рот (0..1), если персонаж ест или говорит. */
    fun mouthOpen(): Float = when (action) {
        Action.EAT -> abs(sin(actionT * 11f))
        Action.TALK -> abs(sin(t * 9f))
        else -> 0f
    }

    /** Точка рта в координатах картинки (до деформации) — между глазами и ниже. */
    fun mouthPoint(): Pair<Float, Float>? {
        val e = rig.eyes
        if (e.isEmpty()) return null
        val cx = e.map { it.cx }.average().toFloat()
        val cy = e.map { it.cy }.average().toFloat()
        val span = if (e.size >= 2) abs(e.last().cx - e.first().cx) else e[0].rx * 3f
        val ry = e.maxOf { it.ry }
        return cx to min(rig.bottom - 4f, cy + ry + span * 0.45f)
    }

    fun mouthWidth(): Float {
        val e = rig.eyes
        val span = if (e.size >= 2) abs(e.last().cx - e.first().cx) else (e.firstOrNull()?.rx ?: 10f) * 3f
        return max(span * 0.42f, bodyW * 0.06f)
    }
}
