package com.dobdmitry.drawnpets.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.data.Creature

/** Короткая карточка выбранного персонажа на сцене. */
@Composable
fun CreatureInfoCard(c: Creature, image: ImageBitmap?, onCare: () -> Unit, onClose: () -> Unit) {
    Panel(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (image != null) Image(image, c.name, Modifier.size(56.dp)) else Text("🙂", fontSize = 40.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${c.name} ${c.moodEmoji}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Text(c.wish() ?: "Ур. ${c.level} • Мне хорошо!", fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.8f))
            }
            PillButton("Ухаживать", onClick = onCare)
            Text(
                "✖️", fontSize = 18.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable(onClick = onClose)
                    .padding(4.dp)
            )
        }
    }
}
