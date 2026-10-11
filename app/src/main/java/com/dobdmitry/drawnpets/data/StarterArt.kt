package com.dobdmitry.drawnpets.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Рисует «детские» стартовые персонажи прямо в коде, чтобы в игре было
 * кого кормить ещё до первого сфотографированного рисунка.
 */
object StarterArt {

    private fun wobblyBlob(cx: Float, cy: Float, rx: Float, ry: Float, rnd: Random, points: Int = 14): Path {
        val path = Path()
        val pts = (0 until points).map { i ->
            val a = 2 * PI * i / points
            val k = 1f + (rnd.nextFloat() - 0.5f) * 0.12f
            Pair(cx + (cos(a) * rx * k).toFloat(), cy + (sin(a) * ry * k).toFloat())
        }
        val first = pts[0]
        val last = pts[points - 1]
        path.moveTo((first.first + last.first) / 2, (first.second + last.second) / 2)
        for (i in 0 until points) {
            val p = pts[i]
            val n = pts[(i + 1) % points]
            path.quadTo(p.first, p.second, (p.first + n.first) / 2, (p.second + n.second) / 2)
        }
        path.close()
        return path
    }

    private fun strokePaint(w: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = w
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    /** Рисует фигуру в стиле наклейки: белая обводка, заливка, карандашный контур. */
    private fun Canvas.sticker(path: Path, fill: Int, ink: Int = Color.rgb(58, 42, 85)) {
        drawPath(path, strokePaint(16f, Color.WHITE))
        drawPath(path, fillPaint(Color.WHITE))
        drawPath(path, fillPaint(fill))
        drawPath(path, strokePaint(7f, ink))
    }

    private fun Canvas.eyes(cx: Float, cy: Float, gap: Float, r: Float) {
        val ink = Color.rgb(58, 42, 85)
        for (dx in listOf(-gap, gap)) {
            drawCircle(cx + dx, cy, r, fillPaint(Color.WHITE))
            drawCircle(cx + dx, cy, r, strokePaint(5f, ink))
            drawCircle(cx + dx + r * 0.2f, cy + r * 0.15f, r * 0.45f, fillPaint(ink))
            drawCircle(cx + dx + r * 0.35f, cy - r * 0.1f, r * 0.15f, fillPaint(Color.WHITE))
        }
    }

    /** Розовый пушистик «Каляка» для двора. */
    fun kalyaka(): Bitmap {
        val bmp = Bitmap.createBitmap(420, 440, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rnd = Random(7)
        val pink = Color.rgb(255, 143, 184)
        val ink = Color.rgb(58, 42, 85)
        // ножки
        val legs = Path().apply {
            addRoundRect(130f, 330f, 175f, 410f, 20f, 20f, Path.Direction.CW)
            addRoundRect(245f, 330f, 290f, 410f, 20f, 20f, Path.Direction.CW)
        }
        c.sticker(legs, Color.rgb(255, 196, 120))
        // ушки
        val ears = Path().apply {
            moveTo(120f, 150f); lineTo(95f, 40f); lineTo(190f, 110f); close()
            moveTo(300f, 150f); lineTo(330f, 40f); lineTo(230f, 110f); close()
        }
        c.sticker(ears, pink)
        // тело
        c.sticker(wobblyBlob(210f, 230f, 150f, 135f, rnd), pink)
        // животик
        c.drawPath(wobblyBlob(210f, 285f, 70f, 50f, rnd, 10), fillPaint(Color.rgb(255, 214, 230)))
        c.drawPath(wobblyBlob(210f, 285f, 70f, 50f, rnd, 10), strokePaint(4f, ink))
        c.eyes(210f, 195f, 52f, 30f)
        // щёчки
        c.drawCircle(120f, 250f, 18f, fillPaint(Color.rgb(255, 92, 138)))
        c.drawCircle(300f, 250f, 18f, fillPaint(Color.rgb(255, 92, 138)))
        // улыбка
        val smile = Path().apply { moveTo(175f, 245f); quadTo(210f, 280f, 245f, 245f) }
        c.drawPath(smile, strokePaint(7f, ink))
        return bmp
    }

    /** Голубая рыбка «Пузырик» для пруда (смотрит вправо). */
    fun puzyrik(): Bitmap {
        val bmp = Bitmap.createBitmap(460, 320, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rnd = Random(11)
        val blue = Color.rgb(110, 196, 255)
        val orange = Color.rgb(255, 170, 80)
        val ink = Color.rgb(58, 42, 85)
        // хвост (справа, рыбка смотрит влево)
        val tail = Path().apply {
            moveTo(320f, 160f); lineTo(440f, 70f); quadTo(410f, 160f, 440f, 250f); close()
        }
        c.sticker(tail, orange)
        // плавники
        val fins = Path().apply {
            moveTo(170f, 75f); quadTo(220f, 10f, 270f, 80f); close()
            moveTo(190f, 245f); quadTo(230f, 300f, 265f, 240f); close()
        }
        c.sticker(fins, orange)
        c.sticker(wobblyBlob(200f, 160f, 160f, 100f, rnd), blue)
        // полоски
        for (x in listOf(230f, 280f)) {
            val stripe = Path().apply { moveTo(x, 80f); quadTo(x + 18f, 160f, x, 240f) }
            c.drawPath(stripe, strokePaint(10f, Color.rgb(70, 140, 230)))
        }
        // глаз
        c.drawCircle(110f, 140f, 30f, fillPaint(Color.WHITE))
        c.drawCircle(110f, 140f, 30f, strokePaint(5f, ink))
        c.drawCircle(102f, 145f, 14f, fillPaint(ink))
        c.drawCircle(97f, 138f, 5f, fillPaint(Color.WHITE))
        // ротик
        val mouth = Path().apply { moveTo(60f, 185f); quadTo(80f, 205f, 100f, 190f) }
        c.drawPath(mouth, strokePaint(6f, ink))
        // пузырьки рисунка
        c.drawCircle(30f, 90f, 14f, strokePaint(5f, Color.rgb(110, 196, 255)))
        c.drawCircle(45f, 45f, 9f, strokePaint(5f, Color.rgb(110, 196, 255)))
        // Разворачиваем носом вправо — так в пруду считается «по умолчанию».
        val m = android.graphics.Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
