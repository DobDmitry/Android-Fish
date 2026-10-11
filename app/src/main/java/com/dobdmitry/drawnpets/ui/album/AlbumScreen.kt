package com.dobdmitry.drawnpets.ui.album

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.ScreenHeader

@Composable
fun AlbumScreen(vm: GameViewModel, onBack: () -> Unit, onCare: (String) -> Unit, onCreate: () -> Unit) {
    val state = vm.state
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EEFF))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader("Альбом (${state.creatures.size})", state.coins, onBack)
        if (state.creatures.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Здесь пока пусто 🖍️", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                PillButton("📷 Оживить рисунок", Modifier.padding(top = 16.dp), onClick = onCreate)
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.creatures, key = { it.id }) { c ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(2.dp, Palette.Ink.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onCare(c.id) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val img = vm.bitmap(c.id)
                        if (img != null) Image(img, c.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        else Text("🙂", fontSize = 60.sp)
                        val worn = c.outfit.mapNotNull { Catalog.accessory(it.id)?.emoji }.joinToString("")
                        if (worn.isNotEmpty()) Text(worn, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopEnd))
                    }
                    Text(
                        "${c.name} ${c.moodEmoji}",
                        fontWeight = FontWeight.ExtraBold, color = Palette.Ink, fontSize = 16.sp, textAlign = TextAlign.Center,
                    )
                    Text(
                        "${c.habitat.emoji} ${c.habitat.title} • ур. ${c.level}",
                        fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
