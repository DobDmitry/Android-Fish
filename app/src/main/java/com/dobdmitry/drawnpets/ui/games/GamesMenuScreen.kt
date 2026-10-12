package com.dobdmitry.drawnpets.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import com.dobdmitry.drawnpets.ui.common.SpeechBubble
import com.dobdmitry.drawnpets.ui.common.rememberFrameTime
import com.dobdmitry.drawnpets.ui.npc.Npc
import com.dobdmitry.drawnpets.ui.npc.drawNpc

enum class MiniGame(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val color: Color,
    /** Что Мурка говорит вслух перед игрой. */
    val voice: String,
) {
    CATCH("catch", "Ловим вкусняшки", "🍎", "Води питомца пальцем и лови еду. Камни и пчёлы — мимо!", Color(0xFFFFE3D1),
        "Лови вкусняшки! Води пальцем по экрану. Камни и пчёл не лови!"),
    JUMP("jump", "Прыг-скок", "🌵", "Нажимай на экран, чтобы перепрыгивать кактусы. Можно прыгнуть дважды!", Color(0xFFE2F6D5),
        "Нажимай на экран, чтобы прыгать через кактусы! Можно прыгнуть два раза."),
    MEMORY("memory", "Найди пару", "🃏", "Открывай карточки и ищи одинаковых персонажей.", Color(0xFFE5E1FF),
        "Открывай карточки и ищи одинаковые картинки!"),
    BALLOONS("balloons", "Шарики", "🎈", "Лопай шарики, пока не улетели. Золотые — самые ценные, тучки не трогай!", Color(0xFFFFE0EC),
        "Лопай шарики! Золотые — самые лучшие. А тучки не трогай!"),
    FLY("fly", "Полёт", "🐦", "Нажимай на экран, чтобы взлетать, и пролетай между колоннами. Звёздочки — бонус!", Color(0xFFDDEEFF),
        "Нажимай на экран, чтобы взлететь! Пролетай между колоннами и не задевай их."),
    HIDE("hide", "Прятки", "🌳", "Смотри внимательно, за какой куст спрятался питомец. Кусты умеют меняться местами!", Color(0xFFD8F5E6),
        "Смотри внимательно, за какой куст я спрячусь!"),
}

@Composable
fun GamesMenuScreen(
    vm: GameViewModel,
    preselected: String?,
    onBack: () -> Unit,
    onPlay: (MiniGame, String?) -> Unit,
) {
    val state = vm.state
    var selected by remember { mutableStateOf(preselected ?: state.creatures.firstOrNull()?.id) }
    val player = selected?.let { state.creature(it) }
    val catSays = when {
        player == null -> "Мяу! Сначала оживи рисунок — играть веселее вместе!"
        player.energy < 20 -> "${player.name} очень устал. Лучше уложи его спать!"
        else -> "Мяу! Выбери друга и игру!"
    }
    LaunchedEffect(Unit) { Voice.say(catSays, pitch = Npc.CAT.voicePitch) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader("Игры с Муркой", state.coins, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val time by rememberFrameTime()
                Canvas(Modifier.size(96.dp)) {
                    drawNpc(Npc.CAT, Offset(size.width / 2, size.height * 0.98f), size.height * 0.9f, time)
                }
                Spacer(Modifier.width(8.dp))
                SpeechBubble(catSays, Modifier.weight(1f))
            }
            if (state.creatures.isNotEmpty()) {
                Text("Кто играет:", fontWeight = FontWeight.Bold, color = Palette.Ink, modifier = Modifier.padding(top = 8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.creatures.forEach { c ->
                        val sel = c.id == selected
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) Palette.Lemon else Color.White)
                                .border(2.dp, Palette.Ink.copy(alpha = if (sel) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                                .clickable { selected = c.id; Sounds.play(Sfx.TAP) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val img = vm.bitmap(c.id)
                            if (img != null) Image(img, c.name, Modifier.size(56.dp)) else Text("🙂", fontSize = 40.sp)
                            Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                            Text("⚡${c.energy.toInt()}", fontSize = 11.sp, color = Palette.Ink.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            MiniGame.entries.forEach { g ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(g.color)
                        .border(2.5.dp, Palette.Ink.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        .clickable { Sounds.play(Sfx.TAP); onPlay(g, selected) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .background(Color.White, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(g.emoji, fontSize = 34.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        Text(g.description, fontSize = 13.sp, color = Palette.Ink.copy(alpha = 0.8f))
                        val best = state.bestScores[g.id]
                        if (best != null) Text("🏆 Рекорд: $best", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Coral)
                    }
                    Text("▶️", fontSize = 26.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
