package com.dobdmitry.drawnpets.ui.album

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.R
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Creature
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.*

@Composable
fun AlbumScreen(vm: GameViewModel, onBack: () -> Unit, onCare: (String) -> Unit, onCreate: () -> Unit) {
    val state = vm.state
    val atlas = ImageBitmap.imageResource(R.drawable.paper_icons)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    PaperArtwork(atlas) {
        Column(Modifier.fillMaxSize().background(PaperCream).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Назад" }, contentAlignment = Alignment.Center) {
                    Text("‹", fontSize = 36.sp, color = Palette.Ink, modifier = Modifier.clearAndSetSemantics { })
                }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text("Мой альбом", fontFamily = PaperType, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                    Text("Рисунков: ${state.creatures.size}", fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink)
                }
                Row(Modifier.paperSurface(PaperMint, 16.dp).padding(8.dp)
                    .clearAndSetSemantics { contentDescription = "Баланс: ${state.coins} монет" }, verticalAlignment = Alignment.CenterVertically) {
                    PaperIconView(PaperIcon.COIN, Modifier.size(26.dp))
                    Text("${state.coins}", fontFamily = PaperType, fontWeight = FontWeight.Bold, color = Palette.Ink)
                }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive((156 * fontScale).dp), contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)) {
                item(key = "intro", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().paperSurface(Color(0xFFF3E5CA)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PaperIconView(PaperIcon.ALBUM, Modifier.size(64.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(if (state.creatures.isEmpty()) "Первая страница ждёт" else "Каждый рисунок — друг",
                                    fontFamily = PaperType, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                                Text(if (state.creatures.isEmpty()) "Нарисуй героя на бумаге и помоги ему ожить."
                                    else "Коснись карточки, чтобы навестить питомца.", fontFamily = PaperType, fontSize = 14.sp, color = Palette.Ink)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 14.dp).paperSurface(PaperCoral, 14.dp)
                            .clickable(role = Role.Button) { Sounds.play(Sfx.TAP); onCreate() }
                            .heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            PaperIconView(PaperIcon.CREATE, Modifier.size(34.dp))
                            Text("Оживить рисунок", Modifier.weight(1f).padding(start = 10.dp), fontFamily = PaperType,
                                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (state.creatures.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().paperSurface(Color(0xFFFFFCF5)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PaperIconView(PaperIcon.CREATE, Modifier.size(100.dp))
                            Text("Здесь будет твоя коллекция", fontFamily = PaperType, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Palette.Ink, textAlign = TextAlign.Center)
                            Text("Созданные питомцы появятся здесь вместе с именами и уровнями.", Modifier.padding(top = 8.dp),
                                fontFamily = PaperType, fontSize = 15.sp, color = Palette.Ink, textAlign = TextAlign.Center)
                        }
                    }
                }
                items(state.creatures, key = { "creature:${it.id}" }) { creature ->
                    AlbumCard(creature, vm.bitmap(creature.id)) { Sounds.play(Sfx.TAP); onCare(creature.id) }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(creature: Creature, bitmap: ImageBitmap?, onClick: () -> Unit) {
    val icon = when (creature.habitat) {
        Habitat.YARD -> PaperIcon.YARD; Habitat.HOME -> PaperIcon.HOME
        Habitat.POND -> PaperIcon.POND; Habitat.MARS -> PaperIcon.MARS
    }
    val tint = when (creature.habitat) {
        Habitat.YARD -> Color(0xFFE4EBCF); Habitat.HOME -> Color(0xFFF4E5CB)
        Habitat.POND -> Color(0xFFDDECEF); Habitat.MARS -> Color(0xFFF1DDDA)
    }
    val mood = when {
        creature.sleeping -> "Сладко спит"
        creature.mood >= 75 -> "Очень счастлив"
        creature.mood >= 50 -> "В хорошем настроении"
        creature.mood >= 30 -> "Немного грустит"
        else -> "Нужна забота"
    }
    val wish = if (creature.sleeping) null else creature.wish()
    val accessories = creature.outfit.mapNotNull { Catalog.accessory(it.id) }
    Column(Modifier.fillMaxWidth().paperSurface(Color(0xFFFFFCF5), 16.dp)
        .clickable(role = Role.Button, onClickLabel = "Навестить ${creature.name}", onClick = onClick)
        .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(tint)
                .padding(12.dp), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else PaperIconView(PaperIcon.CREATE, Modifier.size(64.dp))
            }
            // Translucent tape makes the drawing look pasted onto a paper page.
            Box(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).rotate(-4f)
                .size(width = 52.dp, height = 14.dp).background(Color(0xFFD5BB86).copy(alpha = 0.65f)))
        }
        Text(creature.name, Modifier.padding(top = 12.dp), fontFamily = PaperType, fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PaperIconView(icon, Modifier.size(26.dp))
            Text("${creature.habitat.title} · ур. ${creature.level}", Modifier.weight(1f).padding(start = 4.dp),
                fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink)
        }
        Text(mood, fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink, textAlign = TextAlign.Center)
        if (accessories.isNotEmpty()) Text(accessories.joinToString(" ") { it.emoji },
            Modifier.padding(top = 4.dp).clearAndSetSemantics { contentDescription = "Наряды: ${accessories.joinToString { it.name }}" },
            fontSize = 20.sp, textAlign = TextAlign.Center)
        Text(wish ?: "Навестить", Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp))
            .background(if (wish != null) Color(0xFFF6DECA) else PaperMint).padding(10.dp),
            fontFamily = PaperType, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, textAlign = TextAlign.Center)
    }
}
