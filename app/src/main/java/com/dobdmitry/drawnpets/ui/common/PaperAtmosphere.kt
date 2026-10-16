package com.dobdmitry.drawnpets.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dobdmitry.drawnpets.data.Habitat
import kotlin.math.sin

/** Sparse atmosphere, drawn behind actors; all motion uses the scene's existing clock. */
internal fun DrawScope.drawPaperAtmosphere(habitat: Habitat, t: Float, pan: ScenePan) {
    val w = pan.worldW
    val h = size.height
    when (habitat) {
        Habitat.HOME -> {
            val beam = Path().apply {
                moveTo(pan.sx(.18f), h*.07f); lineTo(pan.sx(.35f),h*.07f)
                lineTo(pan.sx(.67f),h*.85f); lineTo(pan.sx(.28f),h*.85f); close()
            }
            drawPath(beam,Color(0xFFFFE5AC).copy(alpha=.06f+.015f*sin(t*.7f)))
            repeat(15) { i ->
                val x = (i * .061f + .08f) % .8f
                val y = .17f + ((i*.137f+t*.008f)% .6f)
                drawCircle(Color(0xFFFFF7D9).copy(alpha=.28f),1.4f+i%3,
                    Offset(x*w-pan.offset+sin(t*.6f+i)*7f,y*h))
            }
        }
        Habitat.MARS -> {
            repeat(14) { i ->
                val x = ((i*73)%101)/101f*w-pan.offset
                val y = h*(.035f+((i*31)%59)/220f)
                val a=.18f+.2f*(sin(t*.8f+i)*.5f+.5f)
                drawCircle(Color(0xFFFFF0C8).copy(alpha=a),1.5f+i%2,Offset(x,y))
            }
            repeat(9) { i ->
                val x = ((i*.13f+t*.012f)%1f)*w-pan.offset
                val y = h*(.78f+(i%3)*.045f)+sin(t+i)*3f
                drawOval(Color(0xFFE2B78E).copy(alpha=.16f),Offset(x,y),Size(18f+i%3*8f,3f))
            }
        }
        Habitat.POND -> repeat(7) { i ->
            val phase=(t*.3f+i*.37f)%1f
            val x=pan.sx(.1f+i*.13f)
            val y=h*(.30f+(i%2)*.012f)
            drawOval(Color(0xFFFFF1CE).copy(alpha=.3f*sin(phase*3.14159f)),
                Offset(x-24f-phase*30f,y),Size(48f+phase*60f,6f+phase*8f),style=Stroke(1.5f))
        }
        else -> Unit
    }
}
