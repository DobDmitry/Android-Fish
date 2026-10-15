package com.dobdmitry.drawnpets.ui.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.ui.care.CareIcon
import com.dobdmitry.drawnpets.ui.care.CareIconView
import com.dobdmitry.drawnpets.ui.common.*
import com.dobdmitry.drawnpets.ui.npc.Npc
import com.dobdmitry.drawnpets.ui.npc.drawNpc
import kotlinx.coroutines.delay

private data class ShopEntry(
    val id: String,
    val emoji: String,
    val name: String,
    val price: Int,
    val subtitle: String,
    val owned: Boolean,
    val isFood: Boolean,
    val count: Int = 0,
    val minLevel: Int = 1,
)

@Composable
fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val state = vm.state
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var receipt by remember { mutableStateOf<String?>(null) }
    var purchaseKey by remember { mutableIntStateOf(0) }
    val foodScroll = rememberLazyGridState()
    val outfitScroll = rememberLazyGridState()
    val foodAtlas = ImageBitmap.imageResource(R.drawable.care_icons)
    val paperAtlas = ImageBitmap.imageResource(R.drawable.paper_icons)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)

    LaunchedEffect(purchaseKey) {
        if (purchaseKey == 0) Voice.say("Добро пожаловать в мою лавку!", pitch = Npc.RACCOON.voicePitch)
        else {
            receipt?.let { Voice.say(it, pitch = Npc.RACCOON.voicePitch) }
            delay(5000)
            receipt = null
        }
    }

    val entries = if (tab == 0) Catalog.foods.map {
        ShopEntry(it.id, it.emoji, it.name, it.price, "+${it.hunger} сытости", false, true,
            count = state.inventory[it.id] ?: 0)
    } else Catalog.accessories.sortedWith(compareBy({ it.minLevel }, { it.slot.ordinal })).map {
        ShopEntry(it.id, it.emoji, it.name, it.price, it.slot.title,
            it.id in state.ownedAccessories, false, minLevel = it.minLevel)
    }

    PaperArtwork(paperAtlas) {
        Column(Modifier.fillMaxSize().background(PaperCream).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = "Вернуться назад", onClick = onBack)
                    .semantics { contentDescription = "Назад" }, contentAlignment = Alignment.Center) {
                    Text("‹", fontSize = 36.sp, color = Palette.Ink, modifier = Modifier.clearAndSetSemantics { })
                }
                Text("Лавка Енота", Modifier.weight(1f), fontFamily = PaperType,
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Row(Modifier.paperSurface(PaperMint, 16.dp).padding(horizontal = 10.dp, vertical = 8.dp)
                    .clearAndSetSemantics { contentDescription = "Баланс: ${state.coins} монет" },
                    verticalAlignment = Alignment.CenterVertically) {
                    PaperIconView(PaperIcon.COIN, Modifier.size(26.dp))
                    Text("${state.coins}", fontFamily = PaperType, fontWeight = FontWeight.Bold, color = Palette.Ink)
                }
            }
            Row(Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Еда", "Наряды").forEachIndexed { index, title ->
                    Row(Modifier.weight(1f).paperSurface(if (tab == index) PaperMint else Color.White, 16.dp)
                        .selectable(selected = tab == index, role = Role.Tab,
                            onClick = { tab = index; Sounds.play(Sfx.TAP) })
                        .heightIn(min = 52.dp).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CareIconView(foodAtlas, if (index == 0) CareIcon.APPLE else CareIcon.OUTFIT, Modifier.size(30.dp))
                        Text(title, Modifier.padding(start = 8.dp), fontFamily = PaperType, fontWeight = FontWeight.ExtraBold,
                            color = Palette.Ink, fontSize = 16.sp)
                    }
                }
            }
            receipt?.let { message ->
                Text(message, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .paperSurface(PaperMint, 14.dp).padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    fontFamily = PaperType, color = Palette.Ink, fontSize = 14.sp)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive((156 * fontScale).dp),
                state = if (tab == 0) foodScroll else outfitScroll,
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(key = "welcome", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().paperSurface(Color(0xFFF4E4C8)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Shopkeeper(Modifier.size(84.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(if (tab == 0) "Вкусности для друзей" else "Маленькие сокровища",
                                fontFamily = PaperType, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                            Text(if (tab == 0) "Выбирай угощение — оно появится в запасах питомцев."
                                else "Купленный наряд можно надеть в комнате питомца.",
                                fontFamily = PaperType, fontSize = 14.sp, color = Palette.Ink.copy(alpha = 0.75f))
                        }
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    ShopCard(entry, state.coins, vm.maxLevel, foodAtlas) {
                        val bought = if (entry.isFood) vm.buyFood(entry.id) else vm.buyAccessory(entry.id)
                        Sounds.play(if (bought) Sfx.COIN else Sfx.WRONG)
                        receipt = when {
                            bought && entry.isFood -> "${entry.name} — в запасах! Всего: ${vm.state.inventory[entry.id] ?: 0}."
                            bought -> "${entry.name} — твой! Надень в разделе «Наряд»."
                            else -> "Покупка не прошла. Проверь баланс и уровень питомца."
                        }
                        purchaseKey++
                    }
                }
                item(key = "hint", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Монетки можно заработать в играх с Муркой.", Modifier.fillMaxWidth().padding(8.dp),
                        fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun Shopkeeper(modifier: Modifier) {
    // Reading frame time here keeps the product grid out of the animation's recompositions.
    val time = rememberFrameTime()
    Canvas(modifier) {
        drawNpc(Npc.RACCOON, Offset(size.width / 2, size.height * 0.98f), size.height * 0.9f, time.value)
    }
}

@Composable
private fun ShopCard(entry: ShopEntry, coins: Int, level: Int, atlas: ImageBitmap, onBuy: () -> Unit) {
    val locked = entry.minLevel > level
    val canBuy = !entry.owned && !locked && coins >= entry.price
    val status = when {
        entry.owned -> "Уже в гардеробе"
        locked -> "Нужен питомец ур. ${entry.minLevel}"
        coins < entry.price -> "Не хватает ${entry.price - coins} монет"
        entry.isFood -> "Пополнить запасы"
        else -> "Можно примерить после покупки"
    }
    Column(Modifier.fillMaxWidth().paperSurface(Color(0xFFFFFCF5), 18.dp).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
            .background(if (entry.isFood) Color(0xFFF4E9D2) else Color(0xFFE9E5EF)), contentAlignment = Alignment.Center) {
            val icon = if (entry.isFood) CareIcon.food(entry.id) else null
            if (icon != null) CareIconView(atlas, icon, Modifier.size(80.dp))
            else OutfitIcon(entry.id, Modifier.size(80.dp))
        }
        Text(entry.name, Modifier.padding(top = 10.dp), fontFamily = PaperType, fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
        Text(entry.subtitle, fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.75f))
        if (entry.isFood) Text("В запасах: ${entry.count}", fontFamily = PaperType, fontSize = 13.sp, color = Palette.Ink)
        Text(status, Modifier.padding(vertical = 8.dp), fontFamily = PaperType, fontSize = 13.sp,
            color = Palette.Ink, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(when { entry.owned -> PaperMint; canBuy -> PaperCoral; else -> Color(0xFFE9E3D8) })
            .clickable(enabled = canBuy, role = Role.Button, onClickLabel = "Купить ${entry.name} за ${entry.price} монет", onClick = onBuy)
            .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(when { entry.owned -> "Куплено"; locked -> "${entry.price} монет · закрыто"; else -> "${entry.price} монет${if (canBuy) " · Купить" else ""}" },
                fontFamily = PaperType, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink, textAlign = TextAlign.Center)
        }
    }
}
