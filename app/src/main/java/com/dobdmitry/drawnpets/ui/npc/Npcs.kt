package com.dobdmitry.drawnpets.ui.npc

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.dobdmitry.drawnpets.ui.common.Palette
import kotlin.math.sin

/** [voicePitch] — высота голоса синтезатора речи для этого героя. */
enum class Npc(val title: String, val role: String, val emoji: String, val voicePitch: Float) {
    OWL("Дедушка Филин", "Мудрый советчик", "🦉", 0.75f),
    RACCOON("Енот Тимофей", "Хозяин лавки", "🦝", 1.0f),
    HEDGEHOG("Доктор Ёжик", "Лечит питомцев", "🦔", 1.2f),
    CAT("Кошка Мурка", "Затейница игр", "🐱", 1.45f),
    FROG("Лягушка Квака", "Хозяйка пруда", "🐸", 1.3f),
    ROBOT("Робот Винтик", "Исследователь Марса", "🤖", 0.9f),
}

private val ink = Palette.Ink

private fun blink(time: Float, phase: Float) = ((time + phase) % 4f) < 0.13f

private fun DrawScope.eye(c: Offset, r: Float, closed: Boolean, look: Float = 0f) {
    if (closed) {
        drawLine(ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = r * 0.35f, cap = StrokeCap.Round)
        return
    }
    drawCircle(Color.White, r, c)
    drawCircle(ink, r, c, style = Stroke(r * 0.18f))
    drawCircle(ink, r * 0.5f, Offset(c.x + look * r * 0.3f, c.y + r * 0.1f))
    drawCircle(Color.White, r * 0.17f, Offset(c.x + look * r * 0.3f + r * 0.18f, c.y - r * 0.1f))
}

private fun DrawScope.outlinedOval(color: Color, center: Offset, w: Float, h: Float, stroke: Float) {
    val tl = Offset(center.x - w / 2, center.y - h / 2)
    drawOval(color, tl, Size(w, h))
    drawOval(ink, tl, Size(w, h), style = Stroke(stroke))
}

private fun DrawScope.outlinedPath(color: Color, path: Path, stroke: Float) {
    drawPath(path, color)
    drawPath(path, ink, style = Stroke(stroke))
}

/**
 * Рисует NPC так, что [feet] — точка, где он стоит, а [h] — его рост.
 * Возвращает прямоугольник для нажатий.
 */
fun DrawScope.drawNpc(npc: Npc, feet: Offset, h: Float, time: Float, facingLeft: Boolean = false): Rect {
    val bob = sin(time * 2.2f + npc.ordinal) * h * 0.015f
    val s = h * 0.03f // толщина линий
    translate(feet.x, feet.y + bob) {
        val flip = if (facingLeft) -1f else 1f
        when (npc) {
            Npc.OWL -> drawOwl(h, s, time, flip)
            Npc.RACCOON -> drawRaccoon(h, s, time, flip)
            Npc.HEDGEHOG -> drawHedgehog(h, s, time, flip)
            Npc.CAT -> drawCat(h, s, time, flip)
            Npc.FROG -> drawFrog(h, s, time)
            Npc.ROBOT -> drawRobot(h, s, time, flip)
        }
    }
    return Rect(feet.x - h * 0.45f, feet.y - h, feet.x + h * 0.45f, feet.y)
}

private fun DrawScope.drawOwl(h: Float, s: Float, t: Float, flip: Float) {
    val brown = Color(0xFF9C6B45)
    val belly = Color(0xFFE8C9A0)
    val flap = sin(t * 3f) * 8f
    // крылья
    rotate(-10f - flap, pivot = Offset(-h * 0.28f, -h * 0.5f)) {
        outlinedOval(Color(0xFF7D5233), Offset(-h * 0.3f, -h * 0.38f), h * 0.2f, h * 0.42f, s)
    }
    rotate(10f + flap, pivot = Offset(h * 0.28f, -h * 0.5f)) {
        outlinedOval(Color(0xFF7D5233), Offset(h * 0.3f, -h * 0.38f), h * 0.2f, h * 0.42f, s)
    }
    // ушки-пёрышки
    val tufts = Path().apply {
        moveTo(-h * 0.26f, -h * 0.78f); lineTo(-h * 0.3f, -h * 1.0f); lineTo(-h * 0.1f, -h * 0.84f); close()
        moveTo(h * 0.26f, -h * 0.78f); lineTo(h * 0.3f, -h * 1.0f); lineTo(h * 0.1f, -h * 0.84f); close()
    }
    outlinedPath(brown, tufts, s)
    outlinedOval(brown, Offset(0f, -h * 0.47f), h * 0.6f, h * 0.86f, s)
    outlinedOval(belly, Offset(0f, -h * 0.32f), h * 0.38f, h * 0.48f, s * 0.7f)
    // пёрышки на животе
    for (i in 0..2) for (j in -1..1) {
        val c = Offset(j * h * 0.08f, -h * (0.42f - i * 0.1f))
        drawArc(Color(0xFFB98B5E), 20f, 140f, false, Offset(c.x - h * 0.03f, c.y - h * 0.02f), Size(h * 0.06f, h * 0.04f), style = Stroke(s * 0.6f))
    }
    val closed = blink(t, 0.5f)
    eye(Offset(-h * 0.12f, -h * 0.68f), h * 0.1f, closed, flip)
    eye(Offset(h * 0.12f, -h * 0.68f), h * 0.1f, closed, flip)
    // очки дедушки
    drawCircle(ink, h * 0.13f, Offset(-h * 0.12f, -h * 0.68f), style = Stroke(s * 0.7f))
    drawCircle(ink, h * 0.13f, Offset(h * 0.12f, -h * 0.68f), style = Stroke(s * 0.7f))
    val beak = Path().apply {
        moveTo(-h * 0.04f, -h * 0.58f); lineTo(h * 0.04f, -h * 0.58f); lineTo(0f, -h * 0.5f); close()
    }
    outlinedPath(Color(0xFFFFA62B), beak, s * 0.6f)
    // лапки
    for (x in listOf(-h * 0.09f, h * 0.09f)) outlinedOval(Color(0xFFFFA62B), Offset(x, -h * 0.03f), h * 0.1f, h * 0.06f, s * 0.6f)
}

private fun DrawScope.drawRaccoon(h: Float, s: Float, t: Float, flip: Float) {
    val gray = Color(0xFF9AA3AE)
    val dark = Color(0xFF4A4F57)
    val light = Color(0xFFE3E7EC)
    // хвост в полоску
    rotate(sin(t * 2.5f) * 12f, pivot = Offset(h * 0.2f * flip, -h * 0.2f)) {
        for (i in 0..4) {
            val c = Offset(flip * (h * 0.28f + i * h * 0.05f), -h * (0.18f + i * 0.08f))
            outlinedOval(if (i % 2 == 0) gray else dark, c, h * 0.16f, h * 0.12f, s * 0.6f)
        }
    }
    // тело и фартук
    outlinedOval(gray, Offset(0f, -h * 0.25f), h * 0.46f, h * 0.48f, s)
    val apron = Path().apply {
        moveTo(-h * 0.15f, -h * 0.38f); lineTo(h * 0.15f, -h * 0.38f)
        lineTo(h * 0.19f, -h * 0.06f); lineTo(-h * 0.19f, -h * 0.06f); close()
    }
    outlinedPath(Color(0xFF5ED6A8), apron, s * 0.7f)
    drawCircle(Color(0xFFFFE27A), h * 0.03f, Offset(0f, -h * 0.22f))
    // ушки
    val ears = Path().apply {
        moveTo(-h * 0.22f, -h * 0.7f); lineTo(-h * 0.24f, -h * 0.9f); lineTo(-h * 0.06f, -h * 0.8f); close()
        moveTo(h * 0.22f, -h * 0.7f); lineTo(h * 0.24f, -h * 0.9f); lineTo(h * 0.06f, -h * 0.8f); close()
    }
    outlinedPath(gray, ears, s)
    // голова
    outlinedOval(gray, Offset(0f, -h * 0.65f), h * 0.52f, h * 0.38f, s)
    outlinedOval(light, Offset(0f, -h * 0.58f), h * 0.3f, h * 0.2f, s * 0.6f)
    // маска
    drawOval(dark, Offset(-h * 0.22f, -h * 0.74f), Size(h * 0.44f, h * 0.13f))
    val closed = blink(t, 1.3f)
    eye(Offset(-h * 0.1f, -h * 0.68f), h * 0.06f, closed, flip)
    eye(Offset(h * 0.1f, -h * 0.68f), h * 0.06f, closed, flip)
    drawCircle(ink, h * 0.035f, Offset(0f, -h * 0.58f))
    drawArc(ink, 20f, 140f, false, Offset(-h * 0.06f, -h * 0.6f), Size(h * 0.12f, h * 0.08f), style = Stroke(s * 0.7f, cap = StrokeCap.Round))
    // лапки
    for (x in listOf(-h * 0.12f, h * 0.12f)) outlinedOval(dark, Offset(x, -h * 0.02f), h * 0.12f, h * 0.07f, s * 0.6f)
}

private fun DrawScope.drawHedgehog(h: Float, s: Float, t: Float, flip: Float) {
    val spikes = Color(0xFF7A5230)
    val face = Color(0xFFF2D2A9)
    // иголки
    val path = Path()
    val n = 11
    val cx = -flip * h * 0.06f
    val cy = -h * 0.36f
    val rx = h * 0.44f
    val ry = h * 0.36f
    for (i in 0..n) {
        val a = Math.PI * (0.95 + 1.1 * i / n)
        val r1 = if (i % 2 == 0) 1.18f else 0.92f
        val x = cx + (kotlin.math.cos(a) * rx * r1).toFloat() * flip
        val y = cy + (kotlin.math.sin(a) * ry * r1).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.lineTo(cx + flip * rx * 0.3f, -h * 0.05f)
    path.lineTo(cx - flip * rx * 0.8f, -h * 0.05f)
    path.close()
    outlinedPath(spikes, path, s)
    // мордочка
    outlinedOval(face, Offset(flip * h * 0.14f, -h * 0.3f), h * 0.46f, h * 0.44f, s)
    val nose = Path().apply {
        moveTo(flip * h * 0.3f, -h * 0.36f); lineTo(flip * h * 0.46f, -h * 0.27f); lineTo(flip * h * 0.3f, -h * 0.2f); close()
    }
    outlinedPath(face, nose, s * 0.8f)
    drawCircle(ink, h * 0.04f, Offset(flip * h * 0.46f, -h * 0.27f))
    eye(Offset(flip * h * 0.18f, -h * 0.38f), h * 0.065f, blink(t, 2.1f), flip)
    drawCircle(Color(0xFFFF8FB8).copy(alpha = 0.7f), h * 0.04f, Offset(flip * h * 0.22f, -h * 0.24f))
    // шапочка доктора
    val capTop = Offset(flip * h * 0.08f, -h * 0.62f)
    drawRoundRect(Color.White, Offset(capTop.x - h * 0.13f, capTop.y - h * 0.08f), Size(h * 0.26f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(h * 0.04f))
    drawRoundRect(ink, Offset(capTop.x - h * 0.13f, capTop.y - h * 0.08f), Size(h * 0.26f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(h * 0.04f), style = Stroke(s * 0.7f))
    drawRect(Color(0xFFFF4D4D), Offset(capTop.x - h * 0.012f, capTop.y - h * 0.06f), Size(h * 0.024f, h * 0.1f))
    drawRect(Color(0xFFFF4D4D), Offset(capTop.x - h * 0.05f, capTop.y - h * 0.022f), Size(h * 0.1f, h * 0.024f))
    // лапки
    for (x in listOf(-h * 0.1f, h * 0.12f)) outlinedOval(face, Offset(x, -h * 0.03f), h * 0.1f, h * 0.06f, s * 0.6f)
}

private fun DrawScope.drawCat(h: Float, s: Float, t: Float, flip: Float) {
    val orange = Color(0xFFFFA94D)
    val cream = Color(0xFFFFE3C2)
    // хвост
    val sway = sin(t * 2f) * h * 0.08f
    val tail = Path().apply {
        moveTo(-flip * h * 0.18f, -h * 0.12f)
        cubicTo(-flip * h * 0.5f, -h * 0.15f, -flip * h * 0.45f + sway, -h * 0.55f, -flip * h * 0.32f + sway, -h * 0.6f)
    }
    drawPath(tail, ink, style = Stroke(h * 0.1f, cap = StrokeCap.Round))
    drawPath(tail, orange, style = Stroke(h * 0.1f - s * 2f, cap = StrokeCap.Round))
    // тело
    outlinedOval(orange, Offset(0f, -h * 0.25f), h * 0.44f, h * 0.5f, s)
    outlinedOval(cream, Offset(0f, -h * 0.22f), h * 0.24f, h * 0.32f, s * 0.6f)
    // ушки
    val ears = Path().apply {
        moveTo(-h * 0.24f, -h * 0.66f); lineTo(-h * 0.22f, -h * 0.92f); lineTo(-h * 0.04f, -h * 0.78f); close()
        moveTo(h * 0.24f, -h * 0.66f); lineTo(h * 0.22f, -h * 0.92f); lineTo(h * 0.04f, -h * 0.78f); close()
    }
    outlinedPath(orange, ears, s)
    // голова
    outlinedOval(orange, Offset(0f, -h * 0.64f), h * 0.54f, h * 0.4f, s)
    // полоски
    for (dx in listOf(-0.06f, 0f, 0.06f)) drawLine(Color(0xFFE07B1F), Offset(h * dx, -h * 0.84f), Offset(h * dx, -h * 0.76f), strokeWidth = s * 0.9f, cap = StrokeCap.Round)
    val closed = blink(t, 0.9f)
    eye(Offset(-h * 0.11f, -h * 0.66f), h * 0.07f, closed, flip)
    eye(Offset(h * 0.11f, -h * 0.66f), h * 0.07f, closed, flip)
    val nose = Path().apply { moveTo(-h * 0.03f, -h * 0.58f); lineTo(h * 0.03f, -h * 0.58f); lineTo(0f, -h * 0.55f); close() }
    drawPath(nose, Color(0xFFFF6F91))
    drawArc(ink, 10f, 160f, false, Offset(-h * 0.06f, -h * 0.58f), Size(h * 0.06f, h * 0.05f), style = Stroke(s * 0.6f))
    drawArc(ink, 10f, 160f, false, Offset(0f, -h * 0.58f), Size(h * 0.06f, h * 0.05f), style = Stroke(s * 0.6f))
    // усы
    for (side in listOf(-1f, 1f)) for (k in listOf(-1f, 0f, 1f)) {
        drawLine(ink, Offset(side * h * 0.1f, -h * 0.56f), Offset(side * h * 0.32f, -h * (0.58f + k * 0.04f)), strokeWidth = s * 0.4f)
    }
    for (x in listOf(-h * 0.11f, h * 0.11f)) outlinedOval(cream, Offset(x, -h * 0.03f), h * 0.12f, h * 0.07f, s * 0.6f)
}

private fun DrawScope.drawFrog(h: Float, s: Float, t: Float) {
    val green = Color(0xFF6DCB5B)
    val belly = Color(0xFFD9F5B0)
    val puff = 1f + sin(t * 3f) * 0.03f
    // лапки
    for (x in listOf(-h * 0.36f, h * 0.36f)) outlinedOval(green, Offset(x, -h * 0.08f), h * 0.3f, h * 0.14f, s * 0.8f)
    outlinedOval(green, Offset(0f, -h * 0.3f), h * 0.8f * puff, h * 0.56f, s)
    outlinedOval(belly, Offset(0f, -h * 0.22f), h * 0.46f * puff, h * 0.3f, s * 0.6f)
    // глаза на макушке
    outlinedOval(green, Offset(-h * 0.2f, -h * 0.6f), h * 0.26f, h * 0.24f, s)
    outlinedOval(green, Offset(h * 0.2f, -h * 0.6f), h * 0.26f, h * 0.24f, s)
    val closed = blink(t, 1.7f)
    eye(Offset(-h * 0.2f, -h * 0.62f), h * 0.085f, closed)
    eye(Offset(h * 0.2f, -h * 0.62f), h * 0.085f, closed)
    drawArc(ink, 15f, 150f, false, Offset(-h * 0.22f, -h * 0.5f), Size(h * 0.44f, h * 0.16f), style = Stroke(s, cap = StrokeCap.Round))
    drawCircle(Color(0xFFFF8FB8).copy(alpha = 0.7f), h * 0.05f, Offset(-h * 0.28f, -h * 0.38f))
    drawCircle(Color(0xFFFF8FB8).copy(alpha = 0.7f), h * 0.05f, Offset(h * 0.28f, -h * 0.38f))
    // корона хозяйки пруда
    val crown = Path().apply {
        moveTo(-h * 0.08f, -h * 0.56f); lineTo(-h * 0.1f, -h * 0.68f); lineTo(-h * 0.04f, -h * 0.62f)
        lineTo(0f, -h * 0.72f); lineTo(h * 0.04f, -h * 0.62f); lineTo(h * 0.1f, -h * 0.68f); lineTo(h * 0.08f, -h * 0.56f); close()
    }
    outlinedPath(Color(0xFFFFD84D), crown, s * 0.6f)
}

private fun DrawScope.drawRobot(h: Float, s: Float, t: Float, flip: Float) {
    val metal = Color(0xFFB8C7D9)
    val dark = Color(0xFF5B6B80)
    val glow = Color(0xFF5CE1FF)
    // колёса-гусеницы
    drawRoundRect(dark, Offset(-h * 0.24f, -h * 0.12f), Size(h * 0.48f, h * 0.12f), androidx.compose.ui.geometry.CornerRadius(h * 0.06f))
    for (k in 0..3) drawCircle(Color(0xFF8A99AD), h * 0.035f, Offset(-h * 0.17f + k * h * 0.113f, -h * 0.06f))
    // руки-манипуляторы
    val wave = sin(t * 3f) * 18f
    rotate(-20f - wave, pivot = Offset(-h * 0.2f, -h * 0.38f)) {
        drawLine(ink, Offset(-h * 0.2f, -h * 0.38f), Offset(-h * 0.36f, -h * 0.26f), strokeWidth = s * 2.4f, cap = StrokeCap.Round)
        drawLine(metal, Offset(-h * 0.2f, -h * 0.38f), Offset(-h * 0.36f, -h * 0.26f), strokeWidth = s * 1.4f, cap = StrokeCap.Round)
        drawCircle(dark, h * 0.035f, Offset(-h * 0.36f, -h * 0.26f))
    }
    rotate(20f + wave * 0.5f, pivot = Offset(h * 0.2f, -h * 0.38f)) {
        drawLine(ink, Offset(h * 0.2f, -h * 0.38f), Offset(h * 0.36f, -h * 0.26f), strokeWidth = s * 2.4f, cap = StrokeCap.Round)
        drawLine(metal, Offset(h * 0.2f, -h * 0.38f), Offset(h * 0.36f, -h * 0.26f), strokeWidth = s * 1.4f, cap = StrokeCap.Round)
        drawCircle(dark, h * 0.035f, Offset(h * 0.36f, -h * 0.26f))
    }
    // корпус
    val bodyTl = Offset(-h * 0.21f, -h * 0.5f)
    drawRoundRect(metal, bodyTl, Size(h * 0.42f, h * 0.38f), androidx.compose.ui.geometry.CornerRadius(h * 0.06f))
    drawRoundRect(ink, bodyTl, Size(h * 0.42f, h * 0.38f), androidx.compose.ui.geometry.CornerRadius(h * 0.06f), style = Stroke(s))
    // экранчик на груди мигает
    val screenOn = (sin(t * 4f) > 0f)
    drawRoundRect(if (screenOn) Color(0xFF7CFFB2) else Color(0xFF2E8A5C), Offset(-h * 0.1f, -h * 0.42f), Size(h * 0.2f, h * 0.12f), androidx.compose.ui.geometry.CornerRadius(h * 0.02f))
    // голова
    val headTl = Offset(-h * 0.17f, -h * 0.8f)
    drawRoundRect(metal, headTl, Size(h * 0.34f, h * 0.27f), androidx.compose.ui.geometry.CornerRadius(h * 0.08f))
    drawRoundRect(ink, headTl, Size(h * 0.34f, h * 0.27f), androidx.compose.ui.geometry.CornerRadius(h * 0.08f), style = Stroke(s))
    // антенна с огоньком
    drawLine(ink, Offset(0f, -h * 0.8f), Offset(0f, -h * 0.92f), strokeWidth = s * 0.8f)
    drawCircle(if ((t % 1.2f) < 0.6f) Color(0xFFFF5C5C) else Color(0xFFFFB3B3), h * 0.035f, Offset(0f, -h * 0.94f))
    // глаза-окуляры
    val closed = blink(t, 0.4f)
    for (dx in listOf(-0.075f, 0.075f)) {
        val c = Offset(h * dx + flip * h * 0.01f, -h * 0.68f)
        if (closed) drawLine(ink, Offset(c.x - h * 0.04f, c.y), Offset(c.x + h * 0.04f, c.y), strokeWidth = s)
        else {
            drawCircle(Color(0xFF1E2A3A), h * 0.05f, c)
            drawCircle(glow, h * 0.032f, c)
            drawCircle(Color.White, h * 0.012f, Offset(c.x + h * 0.01f, c.y - h * 0.012f))
        }
    }
    // рот-решётка
    for (k in 0..3) drawLine(dark, Offset(-h * 0.06f + k * h * 0.04f, -h * 0.6f), Offset(-h * 0.06f + k * h * 0.04f, -h * 0.57f), strokeWidth = s * 0.6f)
}
