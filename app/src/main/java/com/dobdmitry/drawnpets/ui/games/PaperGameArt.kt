package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

internal val GamePaper = Color(0xFFF7EDD8)
internal val GameInk = Color(0xFF655442)
internal val GameSage = Color(0xFFA8B88B)
internal val GameRose = Color(0xFFD98B72)

@Composable
internal fun GameToken(token: String, modifier: Modifier) {
    val art = rememberStoryArtwork()
    Canvas(modifier) { drawGameToken(art, token, center, minOf(size.width,size.height)*.9f) }
}

/** Cut-paper pieces share one outline and palette instead of platform emoji artwork. */
internal fun DrawScope.drawGameToken(token: String, c: Offset, edge: Float, rotation: Float = 0f) {
    translate(c.x,c.y) { rotate(rotation, Offset.Zero) { scale(edge/100f,edge/100f,Offset.Zero) {
        fun shape(path: Path, color: Color) {
            drawPath(path, color)
            drawPath(path, GameInk, style=Stroke(2.6f))
        }
        fun oval(x:Float,y:Float,w:Float,h:Float,color:Color) {
            drawOval(color,Offset(x,y),Size(w,h))
            drawOval(GameInk,Offset(x,y),Size(w,h),style=Stroke(2.6f))
        }
        fun polygon(points: List<Offset>): Path = Path().apply {
            moveTo(points[0].x,points[0].y); points.drop(1).forEach { lineTo(it.x,it.y) }; close()
        }
        val cream=Color(0xFFFFF1D0)
        val gold=Color(0xFFE0B45C)
        val green=Color(0xFF899F72)
        when(token) {
            "⭐", "🌟", "✨", "🏆" -> {
                val p=Path()
                repeat(10) { i -> val a=i*PI/5-PI/2; val r=if(i%2==0) 44f else 21f
                    val x=cos(a).toFloat()*r;val y=sin(a).toFloat()*r
                    if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
                };p.close();shape(p,gold)
            }
            "🪙" -> { oval(-39f,-42f,78f,84f,gold); drawOval(cream,Offset(-28f,-31f),Size(56f,62f),style=Stroke(3f)); drawLine(GameInk,Offset(0f,-21f),Offset(0f,21f),5f) }
            "🪨" -> shape(polygon(listOf(Offset(-44f,24f),Offset(-28f,-23f),Offset(4f,-38f),Offset(34f,-18f),Offset(45f,29f),Offset(9f,39f))),Color(0xFFABA69A))
            "🌵" -> {
                drawLine(GameInk,Offset(0f,40f),Offset(0f,-35f),24f)
                drawLine(green,Offset(0f,40f),Offset(0f,-35f),19f)
                for(side in listOf(-1,1)) { val p=Path().apply { moveTo(0f,8f);lineTo(side*28f,8f);lineTo(side*28f,-20f) };drawPath(p,GameInk,style=Stroke(14f));drawPath(p,green,style=Stroke(9f)) }
                repeat(4){drawLine(cream,Offset(-5f,it*15f-25),Offset(5f,it*15f-25),2f)}
            }
            "🍄" -> { oval(-12f,-8f,24f,49f,cream); val p=Path().apply{moveTo(-43f,0f);cubicTo(-37f,-55f,37f,-55f,43f,0f);close()};shape(p,GameRose);for(x in listOf(-20f,0f,20f))drawCircle(cream,5f,Offset(x,-16f-abs(x)*.3f)) }
            "🐝" -> { oval(-32f,-37f,31f,35f,cream);oval(1f,-37f,31f,35f,cream);oval(-37f,-16f,74f,43f,gold); for(x in listOf(-15f,2f,19f))drawLine(GameInk,Offset(x,-12f),Offset(x,22f),5f);drawCircle(GameInk,3f,Offset(28f,-1f)) }
            "⛈️", "⚡" -> { oval(-42f,-28f,84f,48f,Color(0xFF9AABA9)); shape(polygon(listOf(Offset(0f,5f),Offset(-18f,31f),Offset(-1f,29f),Offset(-8f,48f),Offset(23f,15f),Offset(8f,17f))),gold) }
            "🧺" -> { drawArc(GameInk,180f,180f,false,Offset(-29f,-42f),Size(58f,65f),style=Stroke(5f)); shape(polygon(listOf(Offset(-43f,-10f),Offset(43f,-10f),Offset(30f,38f),Offset(-30f,38f))),Color(0xFFC49A70));repeat(3){drawLine(cream,Offset(-31f,it*12f),Offset(31f,it*12f),2f)} }
            "🍌" -> { val p=Path().apply {moveTo(-34f,-32f);cubicTo(-39f,42f,31f,55f,39f,-16f);cubicTo(12f,22f,-17f,16f,-34f,-32f);close()};shape(p,gold) }
            "🥕" -> { shape(polygon(listOf(Offset(-23f,-20f),Offset(24f,-20f),Offset(-8f,44f))),GameRose);repeat(3){drawLine(green,Offset(0f,-20f),Offset((it-1)*18f,-43f),6f)} }
            "🍪" -> { oval(-38f,-38f,76f,76f,Color(0xFFC99D6E));repeat(6){val a=it*1.1f;drawCircle(GameInk,4f,Offset(cos(a)*25f,sin(a)*24f))} }
            "🧀", "🍰" -> { shape(polygon(listOf(Offset(-39f,27f),Offset(-39f,-14f),Offset(20f,-35f),Offset(41f,27f))),gold); if(token=="🍰")drawLine(cream,Offset(-34f,7f),Offset(32f,7f),12f) else repeat(3){drawCircle(cream,6f,Offset(-22f+it*20f,10f-it%2*18f))} }
            "🥛" -> { shape(polygon(listOf(Offset(-29f,-38f),Offset(29f,-38f),Offset(23f,39f),Offset(-23f,39f))),cream);drawLine(Color(0xFF9CB9BF),Offset(-24f,-17f),Offset(24f,-17f),5f) }
            "🌼" -> { repeat(6){val a=it*PI/3;oval(cos(a).toFloat()*25-13,sin(a).toFloat()*25-13,26f,26f,cream)};drawCircle(gold,13f,Offset.Zero) }
            "🎈" -> { drawLine(GameInk,Offset.Zero,Offset(4f,48f),2f);oval(-29f,-43f,58f,70f,GameRose) }
            "🃏", "?" -> { drawRoundRect(cream,Offset(-31f,-41f),Size(62f,82f),CornerRadius(8f));drawRoundRect(GameInk,Offset(-31f,-41f),Size(62f,82f),CornerRadius(8f),style=Stroke(3f));drawGameToken("⭐",Offset.Zero,44f) }
            "🌳" -> { drawLine(GameInk,Offset(0f,40f),Offset(0f,-10f),8f);oval(-37f,-43f,74f,65f,green) }
            "🐦" -> { oval(-35f,-17f,65f,43f,Color(0xFF9CB9BF));shape(polygon(listOf(Offset(-12f,3f),Offset(-28f,-36f),Offset(15f,-3f))),cream);shape(polygon(listOf(Offset(27f,-9f),Offset(44f,0f),Offset(28f,8f))),gold);drawCircle(GameInk,3f,Offset(19f,-4f)) }
            else -> { // Orchard fruit: each silhouette and leaf remains recognizable.
                val pear=token=="🍐"
                oval(if(pear)-27f else -35f,if(pear)-12f else -27f,if(pear)54f else 70f,if(pear)54f else 65f,if(pear)green else GameRose)
                if(pear)oval(-17f,-32f,34f,36f,green)
                drawLine(GameInk,Offset(0f,-24f),Offset(4f,-43f),4f)
                oval(3f,-43f,25f,12f,green)
                if(token=="🍓")repeat(5){drawCircle(cream,2f,Offset((it%3-1)*14f,(it/3)*18f-5f))}
            }
        }
    } } }
}
