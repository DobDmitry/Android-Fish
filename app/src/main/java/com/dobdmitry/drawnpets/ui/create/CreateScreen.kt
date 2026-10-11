package com.dobdmitry.drawnpets.ui.create

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.dobdmitry.drawnpets.GameViewModel
import com.dobdmitry.drawnpets.data.Catalog
import com.dobdmitry.drawnpets.data.Habitat
import com.dobdmitry.drawnpets.image.DrawingExtractor
import com.dobdmitry.drawnpets.ui.common.Palette
import com.dobdmitry.drawnpets.ui.common.Panel
import com.dobdmitry.drawnpets.ui.common.PillButton
import com.dobdmitry.drawnpets.ui.common.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3_600_000) it.delete() }
    val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
    val k = maxSide.toFloat() / max(src.width, src.height)
    if (k >= 1f) return src
    return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
}

@Composable
fun CreateScreen(vm: GameViewModel, onBack: () -> Unit, onCreated: (String, Habitat) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var processing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(0.5f) }
    var rawMode by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var habitat by rememberSaveable { mutableStateOf(Habitat.YARD) }

    fun load(uri: Uri) {
        loading = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { DrawingExtractor.loadBitmap(context, uri) }
            loading = false
            if (bmp == null) {
                Toast.makeText(context, "Не получилось открыть фото 😢", Toast.LENGTH_SHORT).show()
            } else {
                source = bmp
                result = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) load(Uri.parse(uri))
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) load(uri)
    }

    fun openCamera() {
        try {
            val uri = newPhotoUri(context)
            pendingUri = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Камера не найдена. Выбери фото из галереи.", Toast.LENGTH_LONG).show()
        }
    }

    fun openGallery() {
        try {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Галерея не найдена 😢", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(source, sensitivity, rawMode) {
        val src = source ?: return@LaunchedEffect
        delay(180)
        processing = true
        result = withContext(Dispatchers.Default) {
            if (rawMode) scaleDown(src, 520) else DrawingExtractor.extract(src, sensitivity)
        }
        processing = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader("Новый рисунок", null, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val src = source
            if (src == null) {
                Panel(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🖍️ ➡️ 📷 ➡️ ✨", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Как оживить рисунок", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "1. Нарисуй персонажа на белом листе.\n" +
                                "2. Обведи его тёмным фломастером.\n" +
                                "3. Сфотографируй сверху при хорошем свете.\n" +
                                "4. Дай имя — и он оживёт!",
                            fontSize = 16.sp, color = Palette.Ink,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (loading) {
                    CircularProgressIndicator(color = Palette.Coral)
                } else {
                    PillButton("📷 Сфотографировать", Modifier.fillMaxWidth(), onClick = ::openCamera)
                    Spacer(Modifier.height(12.dp))
                    PillButton("🖼️ Выбрать из галереи", Modifier.fillMaxWidth(), color = Palette.Mint, textColor = Palette.Ink, onClick = ::openGallery)
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.5.dp, Palette.Ink.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkerboard(Modifier.fillMaxSize())
                    val res = result
                    if (res != null) {
                        Image(
                            res.asImageBitmap(), "Рисунок",
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else if (!processing) {
                        Text(
                            "Не нашли рисунок 🤔\nПопробуй подвинуть ползунок\nили включи «фото целиком»",
                            fontSize = 16.sp, color = Palette.Ink, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        )
                    }
                    if (processing) CircularProgressIndicator(color = Palette.Coral)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("↻ Повернуть", color = Color.White, textColor = Palette.Ink) {
                        source = DrawingExtractor.rotate(src, 90f)
                    }
                    PillButton("📷 Заново", color = Color.White, textColor = Palette.Ink, onClick = ::openCamera)
                    PillButton("🖼️", color = Color.White, textColor = Palette.Ink, onClick = ::openGallery)
                }
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        if (!rawMode) {
                            Text("Чувствительность", fontWeight = FontWeight.Bold, color = Palette.Ink)
                            Slider(
                                value = sensitivity,
                                onValueChange = { sensitivity = it },
                                colors = SliderDefaults.colors(thumbColor = Palette.Coral, activeTrackColor = Palette.Coral),
                            )
                            Row {
                                Text("только яркие линии", fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                                Text("даже бледные", fontSize = 12.sp, color = Palette.Ink.copy(alpha = 0.7f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Фото целиком (без вырезания)", color = Palette.Ink, modifier = Modifier.weight(1f))
                            Switch(checked = rawMode, onCheckedChange = { rawMode = it })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Как зовут персонажа?", fontWeight = FontWeight.Bold, color = Palette.Ink)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { if (it.length <= 20) name = it },
                                singleLine = true,
                                placeholder = { Text("Например, Каляка") },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Palette.Lemon)
                                    .clickable { name = Catalog.randomNames.random() },
                                contentAlignment = Alignment.Center,
                            ) { Text("🎲", fontSize = 26.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Где он будет жить?", fontWeight = FontWeight.Bold, color = Palette.Ink)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Habitat.entries.forEach { hb ->
                                val sel = hb == habitat
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) Palette.Mint else Color(0xFFF1EDE4))
                                        .border(2.dp, if (sel) Palette.Ink else Color.Transparent, RoundedCornerShape(16.dp))
                                        .clickable { habitat = hb }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${hb.emoji} ${hb.title}", fontWeight = FontWeight.Bold, color = Palette.Ink, fontSize = 17.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PillButton(
                    "✨ Оживить!",
                    Modifier.fillMaxWidth(),
                    enabled = result != null && !processing,
                ) {
                    val res = result ?: return@PillButton
                    val id = vm.addCreature(res, name, habitat)
                    onCreated(id, habitat)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Checkerboard(modifier: Modifier) {
    Canvas(modifier) {
        val cell = 24.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        drawRect(Color.White)
        for (y in 0 until rows) for (x in 0 until cols) {
            if ((x + y) % 2 == 0) drawRect(Color(0xFFEDEDED), Offset(x * cell, y * cell), Size(cell, cell))
        }
    }
}
