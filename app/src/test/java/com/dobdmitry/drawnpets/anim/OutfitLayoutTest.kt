package com.dobdmitry.drawnpets.anim

import com.dobdmitry.drawnpets.data.*
import com.dobdmitry.drawnpets.ui.common.outfitPaths
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.*

class OutfitLayoutTest {
    private fun rig(w: Int = 200, h: Int = 280, eyes: List<Eye> = listOf(Eye(75f,65f,12f,12f,0), Eye(125f,75f,12f,12f,0))) =
        Rig(w,h,20f,h-20f,20f,w-20f,h*.68f,w/2f,20f,
            listOf(Limb(LimbKind.ARM,w-55f,120f,w-10f,170f,1,0)),eyes,IntArray(0),FloatArray(0))
    private fun near(a: Float, b: Float) = assertEquals(a,b,.002f)
    @Test fun lensesMatchTiltedEyes() {
        val r=rig(); val p=OutfitLayout.defaultPlacement("specs",r)
        val a=Math.toRadians(p.rot.toDouble())
        near(r.eyes[0].cx,p.x-p.size*.25f*cos(a).toFloat())
        near(r.eyes[0].cy,p.y-p.size*.25f*sin(a).toFloat())
        near(r.eyes[1].cx,p.x+p.size*.25f*cos(a).toFloat())
        near(r.eyes[1].cy,p.y+p.size*.25f*sin(a).toFloat())
    }
    @Test fun eyeOrderDoesNotChangePlacement() {
        val r=rig()
        assertEquals(OutfitLayout.defaultPlacement("specs",r),OutfitLayout.defaultPlacement("specs",r.copy(eyes=r.eyes.reversed())))
    }
    @Test fun handAnchorsLandOnHandAndBindToArm() {
        val r=rig()
        Catalog.accessories.filter { it.slot==Slot.HAND }.forEach {
            val item=WornItem(it.id);val p=OutfitLayout.placement(item,r)!!
            val (x,y)=OutfitLayout.attachment(item,p)
            near(r.limbs[0].tx,x);near(r.limbs[0].ty,y)
            assertEquals(0,OutfitLayout.limbFor(item,p,r))
        }
    }
    @Test fun savedManualFitIsPreserved() {
        val w=WornItem("cap",81f,23f,72f,34f)
        assertEquals(Placement(81f,23f,72f,34f),OutfitLayout.placement(w,rig()))
    }
    @Test fun hatsSitAtCrownRatherThanCoveringFace() {
        val r=rig();val p=OutfitLayout.defaultPlacement("cap",r)
        assertTrue(p.y < r.headY)
        val (_,y)=OutfitLayout.attachment(WornItem("cap"),p)
        assertTrue(y < r.eyes.minOf { it.cy-it.ry })
    }
    @Test fun allItemsHaveArtAndFiniteFallbackFits() {
        assertEquals(Catalog.accessories.map { it.id }.toSet(),outfitPaths.keys)
        listOf(rig(),rig(280,170,emptyList()),rig(160,300,listOf(Eye(80f,70f,18f,18f,0)))).forEach { r ->
            Catalog.accessories.forEach {
                val p=OutfitLayout.defaultPlacement(it.id,r)
                assertTrue(p.size>0 && p.size.isFinite() && p.x.isFinite() && p.y.isFinite() && p.rot.isFinite())
                assertTrue(outfitPaths.getValue(it.id).isNotEmpty())
            }
        }
    }
    @Test fun wingsExtendBeyondBody() {
        val r=rig();val p=OutfitLayout.defaultPlacement("butterfly",r)
        assertTrue(p.x-p.size*.45f<r.left)
        assertTrue(p.x+p.size*.45f>r.right)
    }
    @Test fun animatedGripFollowsHandThroughWalkAndWave() {
        val r=rig();val animator=Animator(r);val m=FloatArray(9);val expected=FloatArray(2)
        val hand=r.limbs[0]
        for (action in listOf(Action.WALK,Action.WAVE,Action.HAPPY)) for (frame in 0..30) {
            animator.action=action;animator.t=frame/10f;animator.actionT=frame/10f;animator.update()
            animator.attachmentTransform(hand.tx,hand.ty,0,m)
            animator.mapOnLimb(0,hand.tx,hand.ty,expected)
            near(expected[0],m[0]*hand.tx+m[1]*hand.ty+m[2])
            near(expected[1],m[3]*hand.tx+m[4]*hand.ty+m[5])
            animator.mapOnLimb(0,hand.tx,hand.ty+1f,expected)
            near(expected[0],m[0]*hand.tx+m[1]*(hand.ty+1f)+m[2])
            near(expected[1],m[3]*hand.tx+m[4]*(hand.ty+1f)+m[5])
        }
    }
    @Test fun exportActualFittingFixtures() {
        val rows=mutableListOf<String>()
        listOf(rig(),rig(280,170,listOf(Eye(115f,55f,12f,12f,0),Eye(165f,55f,12f,12f,0))),
            rig(160,300,listOf(Eye(60f,70f,10f,10f,0),Eye(100f,70f,10f,10f,0))),rig(210,240,emptyList())).forEachIndexed { i,r ->
            val ids=listOf("butterfly", "cap", "specs", "scarf", "balloon")
            ids.forEach { id ->
                val p=OutfitLayout.defaultPlacement(id,r)
                rows += listOf(i,r.w,r.h,id,p.x,p.y,p.size,p.rot).joinToString("\t")
            }
        }
        File("build/outfit-fit-fixtures.tsv").apply { parentFile?.mkdirs();writeText(rows.joinToString("\n")) }
    }
}
