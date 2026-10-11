package com.dobdmitry.drawnpets.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.ui.common.Palette
import kotlinx.coroutines.delay

private class Face(val image: ImageBitmap?, val emoji: String)

private const val PAIRS = 6
private val fillerEmoji = listOf("🐶", "🐱", "🦊", "🐼", "🐸", "🐵", "🦄", "🐙", "🐞", "🐢")

@Composable
fun MemoryGame(vm: GameViewModel, onFinish: (Int) -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val faces = remember {
        val list = mutableListOf<Face>()
        vm.state.creatures.shuffled().take(PAIRS).forEach { c -> list += Face(vm.bitmap(c.id), "🙂") }
        fillerEmoji.shuffled().take(PAIRS - list.size).forEach { list += Face(null, it) }
        list
    }
    val deck = remember { (0 until PAIRS).flatMap { listOf(it, it) }.shuffled() }
    val faceUp = remember { mutableStateListOf<Boolean>().apply { repeat(deck.size) { add(false) } } }
    val matched = remember { mutableStateListOf<Boolean>().apply { repeat(deck.size) { add(false) } } }
    var first by remember { mutableStateOf<Int?>(null) }
    var second by remember { mutableStateOf<Int?>(null) }
    var moves by remember { mutableIntStateOf(0) }
    var pairsFound by remember { mutableIntStateOf(0) }

    LaunchedEffect(second) {
        val a = first
        val b = second
        if (a != null && b != null) {
            delay(750)
            if (deck[a] == deck[b]) {
                Sounds.play(Sfx.HAPPY)
                matched[a] = true
                matched[b] = true
                pairsFound++
            } else {
                Sounds.play(Sfx.WRONG, volume = 0.5f)
                faceUp[a] = false
                faceUp[b] = false
            }
            first = null
            second = null
            if (pairsFound == PAIRS) {
                delay(400)
                finish((40 - moves * 2).coerceAtLeast(6))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE5E1FF), Color(0xFFFFE0EC))))
    ) {
        GameHud("🃏 Пары: $pairsFound/$PAIRS", "👆 Ходы: $moves")
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 120.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (row in 0 until 4) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 3) {
                        val i = row * 3 + col
                        MemoryCard(
                            face = faces[deck[i]],
                            up = faceUp[i] || matched[i],
                            matched = matched[i],
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            if (faceUp[i] || matched[i] || second != null) return@MemoryCard
                            faceUp[i] = true
                            Sounds.play(Sfx.FLIP)
                            if (first == null) {
                                first = i
                            } else {
                                second = i
                                moves++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(face: Face, up: Boolean, matched: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (up) 180f else 0f, tween(350), label = "flip")
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    rotation <= 90f -> Palette.Coral
                    matched -> Color(0xFFD8F5E6)
                    else -> Color.White
                }
            )
            .border(2.5.dp, Palette.Ink.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            Text("❓", fontSize = 34.sp)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = -1f }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (face.image != null) {
                    Image(face.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Text(face.emoji, fontSize = 40.sp)
                }
            }
        }
    }
}
