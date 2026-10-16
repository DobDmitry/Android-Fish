package com.dobdmitry.drawnpets.ui.pond

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.ui.games.trimSprite
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun rememberStoryCrab(): ImageBitmap {
    val context = LocalContext.current
    return remember(context) {
        val image = BitmapFactory.decodeResource(context.resources, R.drawable.story_crab)
        trimSprite(image,0,0,image.width,image.height).asImageBitmap()
    }
}

internal fun DrawScope.drawStoryCrab(image: ImageBitmap, feet: Offset, width: Float, time: Float) {
    val height = width * image.height / image.width
    val lift = abs(sin(time*8f))*width*.012f
    val center = Offset(feet.x,feet.y-height/2-lift)
    rotate(sin(time*8f)*1.5f,center) {
        drawImage(image,IntOffset.Zero,IntSize(image.width,image.height),
            IntOffset((feet.x-width/2).roundToInt(),(feet.y-height-lift).roundToInt()),
            IntSize(width.roundToInt().coerceAtLeast(1),height.roundToInt().coerceAtLeast(1)))
    }
}
