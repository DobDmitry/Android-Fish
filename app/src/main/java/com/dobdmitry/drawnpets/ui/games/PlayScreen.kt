package com.dobdmitry.drawnpets.ui.games

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.audio.Voice
import com.dobdmitry.drawnpets.ui.npc.Npc
import com.dobdmitry.drawnpets.ui.common.AnimatedCreature
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Panel
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import androidx.compose.foundation.layout.size

private data class GameResult(val score: Int, val coins: Int, val record: Boolean)

@Composable
fun PlayScreen(vm: GameViewModel, game: MiniGame, creatureId: String?, onExit: () -> Unit) {
    val creature = creatureId?.let { vm.state.creature(it) }
    val sprite = creatureId?.let { vm.sprite(it) }
    var round by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<GameResult?>(null) }

    val finish: (Int) -> Unit = { score ->
        val prevBest = vm.state.bestScores[game.id] ?: 0
        val coins = when (game) {
            MiniGame.CATCH -> score / 2 + 2
            MiniGame.JUMP -> score / 2 + 2
            MiniGame.MEMORY -> score / 2
            MiniGame.BALLOONS -> score / 2 + 1
            MiniGame.HIDE -> score * 3 + 1
            MiniGame.FLY -> score / 2 + 1
        }
        val got = vm.finishGame(game.id, score, coins, creatureId)
        val record = score > prevBest && score > 0
        result = GameResult(score, got, record)
        Sounds.play(Sfx.WIN)
        Voice.say(
            if (record) "Ура! Новый рекорд! Ты заработал монеток: $got!" else "Молодец! Ты заработал монеток: $got!",
            pitch = Npc.CAT.voicePitch,
        )
    }
    LaunchedEffect(round) { Voice.say(game.voice, pitch = Npc.CAT.voicePitch) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFBDE6FF))
    ) {
        key(round) {
            when (game) {
                MiniGame.CATCH -> CatchGame(sprite, creature?.outfit ?: emptyList(), finish)
                MiniGame.JUMP -> JumpGame(sprite, creature?.outfit ?: emptyList(), finish)
                MiniGame.MEMORY -> MemoryGame(vm, finish)
                MiniGame.BALLOONS -> BalloonGame(vm, finish)
                MiniGame.FLY -> FlyGame(sprite, creature?.outfit ?: emptyList(), vm.state.bestScores[game.id] ?: 0, finish)
                MiniGame.HIDE -> HideSeekGame(sprite, creature?.outfit ?: emptyList(), finish)
            }
        }
        Column(Modifier.statusBarsPadding()) {
            ScreenHeader(game.title, null, onExit)
        }

        result?.let { r ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                Panel(
                    Modifier
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (r.record) "🏆 Новый рекорд!" else "🎉 Игра окончена!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        if (creature != null) {
                            AnimatedCreature(creature, sprite, Modifier.size(130.dp), seed = 3, action = Action.HAPPY)
                        }
                        Text("Очки: ${r.score}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                        Text("Монетки: +${r.coins} 🪙", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Coral)
                        if (creature != null) {
                            Text(
                                "${creature.name} повеселился! 😊 +радость",
                                fontSize = 14.sp, color = Palette.Ink.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("🔄 Ещё раз") { Sounds.play(Sfx.TAP); result = null; round++ }
                            PillButton("🏠 Выход", color = Palette.Mint, textColor = Palette.Ink, onClick = onExit)
                        }
                    }
                }
            }
        }
    }
}

/** Плашка счёта, которую игры рисуют под заголовком. */
@Composable
fun GameHud(left: String, right: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HudChip(left)
        HudChip(right)
    }
}

@Composable
private fun HudChip(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Palette.Ink,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.85f), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
