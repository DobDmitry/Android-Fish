package com.dobdmitry.drawnpets.ui.npc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Panel
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.rememberFrameTime
import androidx.compose.foundation.Image

private val owlTips = listOf(
    "Ух! Сфотографируй новый рисунок — и он оживёт! Нажми на кнопку с камерой.",
    "Рисунок лучше всего получается на белом листе и при хорошем свете.",
    "Обведи персонажа фломастером потемнее — так я легче его разгляжу.",
    "У каждого питомца есть любимая еда. Попробуй угадать, какая!",
    "Уставших питомцев укладывай спать — они проснутся бодрыми.",
    "Знаешь ли ты, что совы умеют поворачивать голову почти на пол-оборота? Ух-ху!",
    "Играй с Муркой в мини-игры — за них дают монетки.",
    "Каждый день я даю новые задания. Выполнишь — получишь награду!",
)

private val raccoonPhrases = listOf(
    "Свежие яблочки! Хрустящая морковка! Заходи, дружок!",
    "У меня есть шляпы и короны — самые модные во дворе!",
    "Монетки можно заработать в играх у Мурки.",
    "Я мою каждую ягодку перед продажей. Еноты — большие чистюли!",
    "Тортик — дорогой, зато все от него в восторге!",
)

private val catPhrases = listOf(
    "Мяу! Поиграем? У меня целых пять весёлых игр!",
    "Кто поймает больше всех вкусняшек? Мур-р!",
    "Я лучше всех играю в прятки… ну, почти.",
    "После игр питомцы радуются, но устают. Не забудь их покормить!",
)

private val frogPhrases = listOf(
    "Ква! В моём пруду всем хватит места!",
    "Нажми на воду — и рыбки приплывут за кормом.",
    "Нарисуй рыбку, осьминога или кита — и поселим его у меня!",
    "Ква-ква! Пузырик сегодня опять пускал пузыри.",
    "Даже сухопутные рисунки могут научиться плавать. Переселяй их ко мне!",
)

@Composable
fun NpcDialog(
    npc: Npc,
    vm: GameViewModel,
    onClose: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenGames: () -> Unit = {},
    onFeedFish: () -> Unit = {},
) {
    val state = vm.state
    var phraseIndex by remember { mutableIntStateOf((0..100).random()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pickPond by remember { mutableStateOf(false) }

    val sick = state.creatures.filter { it.mood < 60 || it.hunger < 40 || it.cleanliness < 40 }
    val phrase = notice ?: when (npc) {
        Npc.OWL -> owlTips[phraseIndex % owlTips.size]
        Npc.RACCOON -> raccoonPhrases[phraseIndex % raccoonPhrases.size]
        Npc.CAT -> catPhrases[phraseIndex % catPhrases.size]
        Npc.FROG -> frogPhrases[phraseIndex % frogPhrases.size]
        Npc.HEDGEHOG -> if (sick.isEmpty()) {
            "Все здоровы и веселы! Не забывай мыть лапки перед едой."
        } else {
            "Ой-ой! ${sick.joinToString { it.name }} нужна помощь. Давай я полечу!"
        }
    }

    LaunchedEffect(phrase) { Voice.say(phrase, pitch = npc.voicePitch) }
    DisposableEffect(Unit) { onDispose { Voice.stop() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Panel(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .navigationBarsPadding()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val time by rememberFrameTime()
                    Canvas(
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Palette.SkyLight)
                    ) {
                        drawNpc(npc, Offset(size.width / 2, size.height * 0.96f), size.height * 0.85f, time)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(npc.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        Text(npc.role, fontSize = 14.sp, color = Palette.Ink.copy(alpha = 0.7f))
                    }
                    Text("✖️", fontSize = 22.sp, modifier = Modifier.clickable(onClick = onClose).padding(6.dp))
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.Paper, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("«$phrase»", fontSize = 16.sp, color = Palette.Ink)
                }
                Spacer(Modifier.height(12.dp))

                when (npc) {
                    Npc.OWL -> {
                        Text("📜 Задания дня", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                        Spacer(Modifier.height(6.dp))
                        state.quests.forEachIndexed { i, q ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(if (q.claimed) Color(0xFFE8F8EE) else Color(0xFFFFF6E0), RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(q.type.emoji, fontSize = 24.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(q.type.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Palette.Ink)
                                    Text("${q.progress} / ${q.target}  •  награда ${q.reward} 🪙", fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.75f))
                                }
                                when {
                                    q.claimed -> Text("✅", fontSize = 22.sp)
                                    q.done -> PillButton("Забрать", color = Palette.Mint, textColor = Palette.Ink) {
                                        val got = vm.claimQuest(i)
                                        Sounds.play(Sfx.WIN)
                                        notice = "Молодец! Держи $got монеток. Ух-ху!"
                                    }
                                    else -> Text("⏳", fontSize = 22.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillButton(
                                if (vm.giftAvailable) "🎁 Подарок дня" else "🎁 Завтра",
                                enabled = vm.giftAvailable,
                                color = Palette.Pink,
                            ) {
                                vm.claimGift()?.let { (coins, emoji) ->
                                    Sounds.play(Sfx.COIN)
                                    notice = "С добрым днём! Вот тебе $coins монеток и $emoji."
                                }
                            }
                            PillButton("💡 Совет", color = Palette.Lemon, textColor = Palette.Ink) {
                                notice = null; phraseIndex++
                            }
                        }
                    }

                    Npc.RACCOON -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("🏪 В лавку") { onClose(); onOpenShop() }
                        PillButton("💬 Ещё", color = Palette.Lemon, textColor = Palette.Ink) { notice = null; phraseIndex++ }
                    }

                    Npc.HEDGEHOG -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("💊 Вылечить всех", enabled = sick.isNotEmpty(), color = Palette.Mint, textColor = Palette.Ink) {
                            val n = vm.healAll()
                            Sounds.play(Sfx.MAGIC)
                            notice = if (n > 0) "Готово! Вылечил: $n. Теперь все бодрые и сытые!" else "Все и так здоровы!"
                        }
                    }

                    Npc.CAT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("🎮 Играть!") { onClose(); onOpenGames() }
                        PillButton("💬 Ещё", color = Palette.Lemon, textColor = Palette.Ink) { notice = null; phraseIndex++ }
                    }

                    Npc.FROG -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillButton("🐠 Покормить рыбок", color = Palette.Water) { onClose(); onFeedFish() }
                            PillButton("🏊 Переселить", color = Palette.Mint, textColor = Palette.Ink) { pickPond = !pickPond }
                        }
                        if (pickPond) {
                            Spacer(Modifier.height(10.dp))
                            val yard = state.creatures.filter { it.habitat == Habitat.YARD }
                            if (yard.isEmpty()) {
                                Text("Во дворе никого нет — все уже плавают у меня!", color = Palette.Ink)
                            } else {
                                Text("Кого пригласить в пруд?", fontWeight = FontWeight.Bold, color = Palette.Ink)
                                yard.forEach { c ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Palette.SkyLight)
                                            .clickable {
                                                vm.moveTo(c.id, Habitat.POND)
                                                Sounds.play(Sfx.SPLASH)
                                                notice = "Ква! ${c.name} теперь живёт в пруду!"
                                                pickPond = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val img = vm.bitmap(c.id)
                                        if (img != null) {
                                            Image(img, contentDescription = c.name, modifier = Modifier.size(44.dp))
                                        } else {
                                            Text("🙂", fontSize = 30.sp)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(c.name, fontWeight = FontWeight.Bold, color = Palette.Ink)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
