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
        const val GX = 30
        const val GY = 30
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
    private val limbAngle = FloatArray(rig.limbs.size)
    private val limbLift = FloatArray(rig.limbs.size)
    private val legCount = rig.limbs.count { it.kind == LimbKind.LEG }
    private val hasLegs = legCount > 0
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
        waddle = if (!hasLegs) when (action) {
            Action.WALK -> sin(p) * 0.08f
            Action.RUN -> sin(p) * 0.1f
            Action.HAPPY -> sin(t * 12f) * 0.04f
            else -> 0f
        } else 0f

        for (i in rig.limbs.indices) {
            val limb = rig.limbs[i]
            val opposite = if (limb.order % 2 == 0) 0f else PI.toFloat()
            when (limb.kind) {
                LimbKind.LEG -> {
                    // Две или четыре ноги шагают поочерёдно, щупальца — волной.
                    val wave = legCount == 3 || legCount > 4
                    val opposite = if (wave) limb.order * 0.8f else opposite
                    val soft = (bodyH * 0.35f / max(1f, limb.length)).coerceIn(0.45f, 1f)
                    limbAngle[i] = soft * when (action) {
                        Action.WALK -> sin(p + opposite) * 0.42f
                        Action.RUN -> sin(p + opposite) * 0.6f
                        Action.HAPPY -> sin(t * 12f + opposite) * 0.18f
                        Action.JUMP -> if (wave) sin(limb.order * 1.7f) * 0.25f else if (limb.order % 2 == 0) 0.28f else -0.28f
                        // щупальца и лапки в воде колышутся волной
                        Action.SWIM -> sin(t * 5f + limb.order * 1.3f) * 0.35f
                        Action.IDLE -> sin(t * 1.3f + limb.order * 1.1f + s) * 0.03f
                        else -> 0f
                    }
                    limbLift[i] = when (action) {
                        Action.WALK, Action.RUN -> max(0f, -cos(p + opposite)) * (if (action == Action.RUN) 0.2f else 0.13f)
                        Action.HAPPY -> max(0f, sin(t * 12f + opposite)) * 0.08f
                        else -> 0f
                    }
                }
                LimbKind.ARM -> {
                    val left = limb.side < 0
                    val off = if (left) 0f else PI.toFloat()
                    // «Поднять руку» — положительный угол
                    val raise = when (action) {
                        Action.IDLE -> 0.12f * sin(t * 1.6f + off + s)
                        Action.WALK -> 0.38f * sin(p + off)
                        Action.RUN -> 0.6f * sin(p + off)
                        Action.HAPPY -> 1.1f + 0.3f * sin(t * 14f + off)
                        Action.WAVE -> if (!left || rig.limbs.count { it.kind == LimbKind.ARM } == 1) 1.1f + 0.45f * sin(actionT * 11f) else 0.15f
                        Action.JUMP -> 1.1f
                        Action.EAT -> 0.45f + 0.2f * sin(actionT * 11f)
                        Action.TALK -> 0.25f + 0.2f * sin(t * 5f + off)
                        Action.SLEEP -> -0.15f
                        Action.SAD -> -0.3f
                        Action.SWIM -> 0.6f * sin(t * 9f + off * 0.5f)
                    }
                    // Длинные руки и щупальца двигаем мягче, чтобы не «улетали»
                    var soft = (bodyH * 0.45f / max(1f, limb.length)).coerceIn(0.35f, 1f)
                    // Опущенная вдоль тела рука только покачивается — иначе тянет за собой бок
                    if ((limb.ty - limb.ay) > limb.length * 0.7f) soft *= 0.35f
                    limbAngle[i] = (if (left) raise else -raise) * soft
                    limbLift[i] = 0f
                }
                LimbKind.ANTENNA -> {
                    limbAngle[i] = when (action) {
                        Action.HAPPY, Action.WAVE -> 0.3f * sin(t * 11f + limb.order * 2f)
                        Action.WALK, Action.RUN -> 0.2f * sin(p + limb.order * 2f)
                        Action.SWIM -> 0.3f * sin(t * 6f + limb.order)
                        Action.SAD, Action.SLEEP -> 0.25f * (if (limb.side < 0) -1f else 1f)
                        else -> 0.1f * sin(t * 2.5f + limb.order * 2f + s)
                    }
                    limbLift[i] = 0f
                }
            }
        }
    }

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val k = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return k * k * (3 - 2 * k)
    }

    /** Деформация одной точки тела (глаза, рот, макушка). Результат — в out[0], out[1]. */
    fun map(x0: Float, y0: Float, out: FloatArray) = mapFrom(x0, y0, x0, y0, out)

    /** Деформация тела: (x0, y0) — исходная точка (для весов), (x, y) — уже сдвинутая конечностью. */
    private fun mapFrom(x0: Float, y0: Float, xs: Float, ys: Float, out: FloatArray) {
        var x = xs
        var y = ys

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
        var v = 0
        for (j in 0..GY) {
            val y0 = h * j / GY
            for (i in 0..GX) {
                val x0 = w * i / GX
                var x = x0
                var y = y0
                val li = rig.vertexLimb[v]
                if (li >= 0) {
                    // Конечность поворачивается вокруг точки крепления; у самого крепления — плавно.
                    val limb = rig.limbs[li]
                    val along = rig.vertexAlong[v]
                    val a = limbAngle[li] * smooth(0.05f, 0.35f, along)
                    if (a != 0f) {
                        val dx = x - limb.ax
                        val dy = y - limb.ay
                        val c = cos(a); val sn = sin(a)
                        x = limb.ax + dx * c - dy * sn
                        y = limb.ay + dx * sn + dy * c
                    }
                    y -= limbLift[li] * limb.length * along.coerceAtMost(1f)
                }
                mapFrom(x0, y0, x, y, tmp)
                verts[k++] = tmp[0]
                verts[k++] = tmp[1]
                v++
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
