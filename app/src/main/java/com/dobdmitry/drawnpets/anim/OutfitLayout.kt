package com.dobdmitry.drawnpets.anim

import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Slot
import com.dobdmitry.drawnpets.data.WornItem
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Где и какого размера рисовать наряд (в пикселях картинки персонажа). */
data class Placement(val x: Float, val y: Float, val size: Float, val rot: Float)

/**
 * Расстановка нарядов по «скелету» рисунка: шляпа — на макушку, очки — на глаза,
 * шарф — под рот, шарик — в руку, рюкзак — за спину.
 */
object OutfitLayout {

    fun defaultPlacement(slot: Slot, rig: Rig): Placement {
        val bodyW = max(1f, rig.right - rig.left)
        val bodyH = max(1f, rig.bottom - rig.top)
        val eyes = rig.eyes
        val eyeX = if (eyes.isNotEmpty()) eyes.map { it.cx }.average().toFloat() else rig.headX
        val eyeY = if (eyes.isNotEmpty()) eyes.map { it.cy }.average().toFloat() else rig.top + bodyH * 0.25f
        val eyeSpan = if (eyes.size >= 2) abs(eyes.last().cx - eyes.first().cx) else 0f
        return when (slot) {
            Slot.HEAD -> {
                // ширина головы — по глазам, если они есть
                val head = if (eyeSpan > 0f) eyeSpan * 2.2f else min(bodyW, bodyH * 0.7f) * 0.6f
                val size = head.coerceIn(bodyW * 0.25f, bodyW * 0.75f)
                Placement(rig.headX, rig.headY - size * 0.18f, size, 0f)
            }
            Slot.FACE -> {
                val size = if (eyeSpan > 0f) eyeSpan * 1.9f else bodyW * 0.35f
                Placement(eyeX, eyeY, size, 0f)
            }
            Slot.NECK -> {
                val r = eyes.maxOfOrNull { it.ry } ?: 0f
                val y = if (eyes.isNotEmpty()) min(rig.hipY - bodyH * 0.05f, eyeY + r + max(eyeSpan, bodyH * 0.12f) * 1.15f)
                else rig.top + bodyH * 0.45f
                val size = if (eyeSpan > 0f) eyeSpan * 1.1f else bodyW * 0.28f
                Placement(eyeX, y, size.coerceAtLeast(bodyW * 0.15f), 0f)
            }
            Slot.HAND -> {
                val size = (bodyH * 0.3f).coerceAtMost(bodyW * 0.6f)
                val arm = handLimb(rig)
                if (arm != null) Placement(arm.tx, arm.ty - size * 0.25f, size, 0f)
                else Placement(rig.right - bodyW * 0.05f, rig.top + bodyH * 0.5f, size, 0f)
            }
            Slot.BACK -> {
                // ширину берём без раскинутых рук
                val size = min(bodyW * 0.8f, bodyH * 0.6f)
                Placement(rig.cx, rig.top + bodyH * 0.42f, size, 0f)
            }
        }
    }

    /** Рука, в которую по умолчанию кладём предмет: правая, если есть. */
    private fun handLimb(rig: Rig): Limb? {
        val arms = rig.limbs.filter { it.kind == LimbKind.ARM }
        return arms.filter { it.side > 0 }.maxByOrNull { it.length } ?: arms.maxByOrNull { it.length }
    }

    /** Место надетой вещи: своё (если подгоняли) или по умолчанию. */
    fun placement(item: WornItem, rig: Rig): Placement? {
        val acc = Catalog.accessory(item.id) ?: return null
        val x = item.x; val y = item.y; val size = item.size
        return if (x != null && y != null && size != null) Placement(x, y, size, item.rot)
        else defaultPlacement(acc.slot, rig).copy(rot = item.rot)
    }

    /**
     * Предмет «в руку» двигается вместе с ближайшей конечностью, если лежит
     * у её кончика. Остальные наряды двигаются вместе с телом.
     */
    fun limbFor(item: WornItem, p: Placement, rig: Rig): Int {
        if (Catalog.accessory(item.id)?.slot != Slot.HAND) return -1
        var best = -1
        var bestD = max(rig.right - rig.left, rig.bottom - rig.top) * 0.3f
        rig.limbs.forEachIndexed { i, l ->
            if (l.kind == LimbKind.LEG) return@forEachIndexed
            val d = hypot(l.tx - p.x, l.ty - p.y)
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }
}
