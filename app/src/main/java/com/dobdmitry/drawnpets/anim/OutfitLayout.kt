package com.dobdmitry.drawnpets.anim

import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Slot
import com.dobdmitry.drawnpets.data.WornItem
import kotlin.math.*

data class Placement(val x: Float, val y: Float, val size: Float, val rot: Float)

/** Coordinates remain artwork centres, preserving existing manually fitted saves. */
object OutfitLayout {
    /** Attachment point in the 100x100 artwork: brim, lens centre or handle. */
    fun anchor(id: String): Pair<Float, Float> = when (id) {
        "flower", "blossom", "bow", "star", "specs", "glasses", "mask", "ball", "butterfly", "rainbow" -> .5f to .5f
        "bell", "scarf", "beads", "tie" -> .5f to .28f
        "ribbon", "medal" -> .5f to .2f
        "balloon", "sunflower", "lollipop", "umbrella" -> .5f to .9f
        "party" -> .5f to .86f
        "guitar" -> .55f to .38f
        "crystal", "trophy" -> .5f to .87f
        "backpack" -> .27f to .45f
        else -> .5f to .82f
    }

    private fun handLimb(rig: Rig): Limb? = rig.limbs.filter { it.kind == LimbKind.ARM }.let { arms ->
        arms.filter { it.side > 0 }.maxByOrNull { it.length } ?: arms.maxByOrNull { it.length }
    }

    fun defaultPlacement(id: String, rig: Rig): Placement {
        val slot = Catalog.accessory(id)?.slot ?: Slot.HEAD
        val w = max(1f, rig.right - rig.left); val h = max(1f, rig.bottom - rig.top)
        val eyes = rig.eyes.sortedBy { it.cx }
        val left = eyes.firstOrNull(); val right = eyes.lastOrNull()
        val span = if (eyes.size >= 2) hypot(right!!.cx - left!!.cx, right.cy - left.cy) else 0f
        val eyeX = if (eyes.size >= 2) (left!!.cx + right!!.cx) / 2 else left?.cx ?: rig.headX
        val eyeY = if (eyes.size >= 2) (left!!.cy + right!!.cy) / 2 else left?.cy ?: (rig.top + h * .25f)
        val angle = if (span > 0f) Math.toDegrees(atan2((right!!.cy - left!!.cy).toDouble(), (right.cx - left.cx).toDouble())).toFloat() else 0f
        val headWidth = (if (span > 0f) span * 2.2f else min(w, h * .7f) * .7f).coerceIn(w * .2f, w * .9f)
        var size: Float
        var x: Float
        var y: Float
        var rot = 0f
        when (slot) {
            Slot.HEAD -> {
                size = headWidth
                x = rig.headX; y = rig.headY + headWidth * .04f
                if (id in listOf("flower", "blossom", "bow")) { size *= .55f; x += headWidth * .25f }
                if (id == "star") { size *= .55f; y -= size * .35f }
            }
            Slot.FACE -> {
                size = if (span > 0f) span * 2f else max((left?.rx ?: 0f) * 5f, w * .35f)
                x = eyeX; y = eyeY; rot = angle
            }
            Slot.NECK -> {
                size = headWidth * .65f; x = eyeX
                y = if (eyes.isNotEmpty()) min(rig.hipY - h * .05f, eyeY + (eyes.maxOfOrNull { it.ry } ?: 0f) + max(span, h * .12f) * 1.15f)
                    else rig.top + h * .45f
            }
            Slot.HAND -> {
                size = min(h * .4f, w * .7f)
                val hand = handLimb(rig)
                x = hand?.tx ?: (rig.right - w * .05f); y = hand?.ty ?: (rig.top + h * .55f)
            }
            Slot.BACK -> {
                size = when (id) { "backpack" -> min(w * .6f, h * .6f); else -> max(w * 1.18f, h * .75f) }
                x = if (id == "backpack") rig.right - size * .25f else rig.cx
                y = rig.top + h * .43f
            }
        }
        val (ax, ay) = anchor(id)
        val a = Math.toRadians(rot.toDouble()); val dx = (.5f - ax) * size; val dy = (.5f - ay) * size
        return Placement(x + dx * cos(a).toFloat() - dy * sin(a).toFloat(),
            y + dx * sin(a).toFloat() + dy * cos(a).toFloat(), size, rot)
    }

    fun placement(item: WornItem, rig: Rig): Placement? {
        if (Catalog.accessory(item.id) == null) return null
        return if (item.custom) Placement(item.x!!, item.y!!, item.size!!, item.rot)
        else defaultPlacement(item.id, rig).let { it.copy(rot = it.rot + item.rot) }
    }

    fun attachment(item: WornItem, p: Placement): Pair<Float, Float> {
        val (ax, ay) = anchor(item.id)
        val dx = (ax - .5f) * p.size; val dy = (ay - .5f) * p.size
        val a = Math.toRadians(p.rot.toDouble())
        return (p.x + dx * cos(a).toFloat() - dy * sin(a).toFloat()) to
            (p.y + dx * sin(a).toFloat() + dy * cos(a).toFloat())
    }

    fun limbFor(item: WornItem, p: Placement, rig: Rig): Int {
        if (Catalog.accessory(item.id)?.slot != Slot.HAND) return -1
        val (x, y) = attachment(item, p)
        return rig.limbs.indices.filter { rig.limbs[it].kind == LimbKind.ARM }
            .minByOrNull { hypot(rig.limbs[it].tx - x, rig.limbs[it].ty - y) }
            ?.takeIf { hypot(rig.limbs[it].tx - x, rig.limbs[it].ty - y) < max(rig.right-rig.left, rig.bottom-rig.top) * .3f } ?: -1
    }
}
