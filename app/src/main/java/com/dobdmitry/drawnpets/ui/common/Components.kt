package com.dobdmitry.drawnpets.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobdmitry.drawnpets.anim.Action
import com.dobdmitry.drawnpets.audio.Sfx
import com.dobdmitry.drawnpets.audio.Sounds
import com.dobdmitry.drawnpets.anim.CreatureSprite
import com.dobdmitry.drawnpets.data.Creature

@Composable
fun CoinBadge(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .background(Palette.Lemon, RoundedCornerShape(50))
            .border(2.dp, Palette.Ink.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 18.sp)
        Spacer(Modifier.width(4.dp))
        Text("$coins", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Palette.Ink)
    }
}

/** Круглая «детская» кнопка с эмодзи и подписью. */
@Composable
fun EmojiButton(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 58.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { Sounds.play(Sfx.TAP); onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(size)
                .shadow(4.dp, CircleShape)
                .background(if (enabled) color else Color(0xFFDDDDDD), CircleShape)
                .border(2.5.dp, Palette.Ink.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = (size.value * 0.45f).sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Большая прямоугольная кнопка. */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.Coral,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else Color(0xFFCCCCCC))
            .border(2.5.dp, Palette.Ink.copy(alpha = if (enabled) 0.8f else 0.3f), RoundedCornerShape(50))
            .clickable(enabled = enabled) { Sounds.play(Sfx.TAP); onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatBar(emoji: String, label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEDE6DA))
                    .border(1.5.dp, Palette.Ink.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((value / 100f).coerceIn(0.02f, 1f))
                        .height(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (value < 30) Color(0xFFFF5A5A) else color)
                )
            }
        }
    }
}

/** Верхняя панель экрана: «назад», заголовок, монетки. */
@Composable
fun ScreenHeader(title: String, coins: Int?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Palette.Ink.copy(alpha = 0.8f), CircleShape)
                .clip(CircleShape)
                .clickable { Sounds.play(Sfx.TAP); onBack() },
            contentAlignment = Alignment.Center,
        ) { Text("⬅️", fontSize = 20.sp) }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, modifier = Modifier.weight(1f))
        if (coins != null) CoinBadge(coins)
    }
}

/** Анимированный персонаж в отдельной области (для карточек и экранов ухода). */
@Composable
fun AnimatedCreature(
    creature: Creature?,
    sprite: CreatureSprite?,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    swimming: Boolean = false,
    action: Action? = null,
) {
    val time by rememberFrameTime()
    Canvas(modifier) {
        drawCreature(
            sprite = sprite,
            action = action,
            actionTime = time,
            feet = Offset(size.width / 2, size.height * 0.95f),
            height = size.height * 0.8f,
            time = time,
            seed = seed,
            sleeping = creature?.sleeping == true,
            sad = (creature?.mood ?: 100f) < 30f,
            accessory = creature?.accessory,
            swimming = swimming,
        )
    }
}

/** Белая карточка с тенью. */
@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(color, RoundedCornerShape(24.dp))
            .border(2.5.dp, Palette.Ink.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(2.dp, Palette.Ink.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 15.sp, color = Palette.Ink, fontWeight = FontWeight.SemiBold)
    }
}

val SpacedRow = Arrangement.spacedBy(8.dp)
