package com.dobdmitry.drawnpets.ui.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import com.dobdmitry.drawnpets.ui.common.SpeechBubble
import com.dobdmitry.drawnpets.ui.common.rememberFrameTime
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
    val minLevel: Int = 1,
)

@Composable
fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val state = vm.state
    var tab by remember { mutableIntStateOf(0) }
    var say by remember { mutableStateOf("Добро пожаловать в мою лавку! Что купим?") }
    var sayKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(say) { Voice.say(say, pitch = Npc.RACCOON.voicePitch) }
    LaunchedEffect(sayKey) {
        if (sayKey > 0) {
            delay(3000)
            say = "Что-нибудь ещё?"
        }
    }

    val entries = if (tab == 0) {
        Catalog.foods.map {
            ShopEntry(it.id, it.emoji, it.name, it.price, "+${it.hunger} сытости • у тебя ${state.inventory[it.id] ?: 0}", false, true)
        }
    } else {
        Catalog.accessories.sortedWith(compareBy({ it.minLevel }, { it.slot.ordinal })).map {
            ShopEntry(it.id, it.emoji, it.name, it.price, "${it.slot.title} • с ${it.minLevel}-го уровня", it.id in state.ownedAccessories, false, it.minLevel)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFEFFAF3))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader("Лавка Енота", state.coins, onBack)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val time by rememberFrameTime()
            Canvas(Modifier.size(110.dp)) {
                drawNpc(Npc.RACCOON, Offset(size.width / 2, size.height * 0.98f), size.height * 0.9f, time)
            }
            Spacer(Modifier.width(8.dp))
            SpeechBubble(say, Modifier.weight(1f))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("🍎 Еда", "🎩 Наряды").forEachIndexed { i, title ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (tab == i) Palette.Mint else Color.White)
                        .border(2.dp, Palette.Ink.copy(alpha = if (tab == i) 0.9f else 0.3f), RoundedCornerShape(50))
                        .clickable { tab = i }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(title, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, fontSize = 16.sp) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(entries, key = { it.id }) { e ->
                val locked = e.minLevel > vm.maxLevel
                val canBuy = !e.owned && !locked && state.coins >= e.price
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(2.dp, Palette.Ink.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable(enabled = !e.owned) {
                            val ok = if (e.isFood) vm.buyFood(e.id) else vm.buyAccessory(e.id)
                            Sounds.play(if (ok) Sfx.COIN else Sfx.WRONG)
                            say = when {
                                ok && e.isFood -> "Держи ${e.emoji}! Приятного аппетита!"
                                ok -> "Отличный выбор! Надень ${e.emoji} питомцу в разделе «Наряд»."
                                locked -> "Это для опытных! Нужен питомец ${e.minLevel}-го уровня."
                                else -> "Не хватает монеток. Поиграй с Муркой — заработаешь!"
                            }
                            sayKey++
                        }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(e.emoji, fontSize = 44.sp)
                    Text(e.name, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, fontSize = 16.sp)
                    Text(e.subtitle, fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.7f))
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    e.owned -> Palette.Mint
                                    canBuy -> Palette.Lemon
                                    else -> Color(0xFFEAEAEA)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            when {
                                e.owned -> "✅ Есть"
                                locked -> "🔒 ур. ${e.minLevel}"
                                else -> "🪙 ${e.price}"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Palette.Ink,
                        )
                    }
                }
            }
        }
    }
}
