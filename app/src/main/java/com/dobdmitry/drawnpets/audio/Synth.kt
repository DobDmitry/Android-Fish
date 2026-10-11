package com.dobdmitry.drawnpets.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Звуковые эффекты игры. */
enum class Sfx {
    TAP, FLIP, POP, COIN, JUMP, BOING, EAT, HAPPY, BUBBLE, SPLASH, HURT, WIN, SQUEAK, MAGIC, WRONG, YAWN
}

/** Фоновые мелодии. */
enum class Track { YARD, POND, GAMES, MARS }

/**
 * Синтезатор: все звуки и музыка генерируются прямо в коде, без аудиофайлов.
 * Результат — 16-битный моно PCM с частотой [RATE].
 */
object Synth {
    const val RATE = 22050
    private const val TWO_PI = (2 * PI).toFloat()

    private enum class Wave { SINE, TRIANGLE, SOFT_SQUARE, BELL, PLUCK }

    private fun osc(wave: Wave, phase: Float): Float {
        val p = phase - kotlin.math.floor(phase)
        return when (wave) {
            Wave.SINE -> sin(TWO_PI * p)
            Wave.TRIANGLE -> 4f * abs(p - 0.5f) - 1f
            Wave.SOFT_SQUARE -> (sin(TWO_PI * p) + sin(3 * TWO_PI * p) / 3f + sin(5 * TWO_PI * p) / 5f) * 0.8f
            Wave.BELL -> sin(TWO_PI * p) + 0.35f * sin(TWO_PI * p * 2.76f) + 0.15f * sin(TWO_PI * p * 5.4f)
            Wave.PLUCK -> sin(TWO_PI * p) + 0.4f * sin(2 * TWO_PI * p)
        }
    }

    private fun midi(note: Int): Float = 440f * 2f.pow((note - 69) / 12f)

    /**
     * Добавляет ноту в буфер. Частота плавно идёт от f0 к f1.
     * decay > 0 — экспоненциальное затухание (сек), иначе ровная громкость с release.
     */
    private fun tone(
        buf: FloatArray, start: Float, dur: Float, f0: Float, f1: Float = f0,
        wave: Wave = Wave.SINE, vol: Float = 0.5f, attack: Float = 0.005f, release: Float = 0.05f,
        decay: Float = 0f, vibrato: Float = 0f, vibratoHz: Float = 6f, wrap: Boolean = false,
    ) {
        val s0 = (start * RATE).toInt()
        val n = (dur * RATE).toInt()
        var phase = 0f
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val k = i / n.toFloat()
            var f = f0 + (f1 - f0) * k
            if (vibrato != 0f) f *= 1f + vibrato * sin(TWO_PI * vibratoHz * t)
            phase += f / RATE
            var env = min(1f, t / attack) * min(1f, (dur - t) / release)
            if (decay > 0f) env *= exp(-t / decay)
            val idx = s0 + i
            val j = if (wrap) ((idx % buf.size) + buf.size) % buf.size else idx
            if (j in buf.indices) buf[j] += osc(wave, phase) * env * vol
        }
    }

    private fun noise(
        buf: FloatArray, start: Float, dur: Float, vol: Float, decay: Float, smooth: Float = 0.5f,
        rnd: Random = Random(1), wrap: Boolean = false, highpass: Boolean = false,
    ) {
        val s0 = (start * RATE).toInt()
        val n = (dur * RATE).toInt()
        var lp = 0f
        for (i in 0 until n) {
            val t = i / RATE.toFloat()
            val white = rnd.nextFloat() * 2f - 1f
            lp += (white - lp) * smooth
            val v = if (highpass) white - lp else lp
            val env = exp(-t / decay) * min(1f, t / 0.002f)
            val idx = s0 + i
            val j = if (wrap) ((idx % buf.size) + buf.size) % buf.size else idx
            if (j in buf.indices) buf[j] += v * env * vol
        }
    }

    private fun toPcm(buf: FloatArray, peak: Float = 0.85f): ShortArray {
        var m = 0.0001f
        for (v in buf) m = max(m, abs(v))
        val g = peak / m
        return ShortArray(buf.size) { (buf[it] * g * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
    }

    fun sfx(s: Sfx): ShortArray {
        fun buf(sec: Float) = FloatArray((sec * RATE).toInt())
        val b: FloatArray
        when (s) {
            Sfx.TAP -> { b = buf(0.08f); tone(b, 0f, 0.07f, 900f, 1100f, decay = 0.025f) }
            Sfx.FLIP -> { b = buf(0.1f); noise(b, 0f, 0.06f, 0.6f, 0.015f, 0.6f); tone(b, 0f, 0.06f, 1500f, 800f, vol = 0.3f, decay = 0.02f) }
            Sfx.POP -> {
                b = buf(0.18f)
                noise(b, 0f, 0.05f, 0.9f, 0.01f, 0.9f)
                tone(b, 0f, 0.14f, 750f, 140f, vol = 0.8f, decay = 0.05f)
            }
            Sfx.COIN -> {
                b = buf(0.4f)
                tone(b, 0f, 0.08f, 988f, wave = Wave.SOFT_SQUARE, vol = 0.4f)
                tone(b, 0.07f, 0.32f, 1319f, wave = Wave.SOFT_SQUARE, vol = 0.4f, decay = 0.12f)
            }
            Sfx.JUMP -> { b = buf(0.22f); tone(b, 0f, 0.2f, 300f, 820f, wave = Wave.TRIANGLE, vol = 0.6f, decay = 0.15f) }
            Sfx.BOING -> {
                b = buf(0.45f)
                tone(b, 0f, 0.42f, 180f, 520f, wave = Wave.SINE, vol = 0.7f, decay = 0.25f, vibrato = 0.12f, vibratoHz = 14f)
            }
            Sfx.EAT -> {
                b = buf(0.42f)
                for (k in 0 until 3) {
                    val st = k * 0.13f
                    noise(b, st, 0.07f, 0.5f, 0.025f, 0.25f, Random(k))
                    tone(b, st, 0.08f, 220f - k * 20f, 140f, wave = Wave.TRIANGLE, vol = 0.6f, decay = 0.03f)
                }
            }
            Sfx.HAPPY -> {
                b = buf(0.55f)
                listOf(72, 76, 79, 84).forEachIndexed { i, n ->
                    tone(b, i * 0.08f, 0.25f, midi(n), wave = Wave.TRIANGLE, vol = 0.45f, decay = 0.12f)
                }
            }
            Sfx.BUBBLE -> {
                b = buf(0.25f)
                tone(b, 0f, 0.09f, 380f, 1150f, vol = 0.6f, decay = 0.05f)
                tone(b, 0.11f, 0.08f, 500f, 1400f, vol = 0.45f, decay = 0.04f)
            }
            Sfx.SPLASH -> {
                b = buf(0.6f)
                noise(b, 0f, 0.55f, 0.7f, 0.12f, 0.35f)
                tone(b, 0.05f, 0.1f, 400f, 1200f, vol = 0.35f, decay = 0.05f)
                tone(b, 0.2f, 0.08f, 600f, 1500f, vol = 0.25f, decay = 0.04f)
            }
            Sfx.HURT -> { b = buf(0.35f); tone(b, 0f, 0.32f, 330f, 120f, wave = Wave.SOFT_SQUARE, vol = 0.45f, decay = 0.15f) }
            Sfx.WIN -> {
                b = buf(1.2f)
                listOf(72, 76, 79).forEachIndexed { i, n -> tone(b, i * 0.12f, 0.16f, midi(n), wave = Wave.SOFT_SQUARE, vol = 0.35f) }
                for (n in listOf(72, 76, 79, 84)) tone(b, 0.38f, 0.75f, midi(n), wave = Wave.TRIANGLE, vol = 0.3f, decay = 0.35f)
            }
            Sfx.SQUEAK -> { b = buf(0.16f); tone(b, 0f, 0.14f, 1300f, 1700f, vol = 0.4f, vibrato = 0.08f, vibratoHz = 30f) }
            Sfx.MAGIC -> {
                b = buf(1.3f)
                tone(b, 0f, 0.9f, 400f, 1600f, wave = Wave.SINE, vol = 0.35f, decay = 0.6f)
                val rnd = Random(5)
                repeat(14) {
                    val st = 0.05f + rnd.nextFloat() * 1.0f
                    tone(b, st, 0.2f, midi(84 + rnd.nextInt(14)), wave = Wave.BELL, vol = 0.25f, decay = 0.07f)
                }
            }
            Sfx.WRONG -> {
                b = buf(0.32f)
                tone(b, 0f, 0.13f, 240f, wave = Wave.SOFT_SQUARE, vol = 0.35f)
                tone(b, 0.15f, 0.15f, 190f, wave = Wave.SOFT_SQUARE, vol = 0.35f)
            }
            Sfx.YAWN -> { b = buf(0.8f); tone(b, 0f, 0.75f, 620f, 260f, wave = Wave.TRIANGLE, vol = 0.45f, attack = 0.1f, release = 0.3f, vibrato = 0.03f, vibratoHz = 5f) }
        }
        return toPcm(b, 0.8f)
    }

    // ------------------------------------------------------------------ музыка

    private class Song(
        val bpm: Float,
        val root: Int,
        /** Аккорды по тактам: смещения корня от тоники. */
        val chords: List<Int>,
        val minor: List<Boolean>,
        val lead: Wave,
        val drums: Boolean,
        val arpeggio: Boolean,
        val busy: Float,
        val seed: Int,
    )

    private fun song(t: Track) = when (t) {
        Track.YARD -> Song(112f, 60, listOf(0, 7, 9, 5, 0, 7, 5, 0), listOf(false, false, true, false, false, false, false, false), Wave.TRIANGLE, true, false, 0.55f, 3)
        Track.POND -> Song(84f, 65, listOf(0, 9, 5, 7, 0, 9, 5, 0), listOf(false, true, false, false, false, true, false, false), Wave.BELL, false, true, 0.35f, 7)
        Track.GAMES -> Song(132f, 67, listOf(0, 7, 9, 5, 0, 5, 7, 0), listOf(false, false, true, false, false, false, false, false), Wave.SOFT_SQUARE, true, false, 0.75f, 11)
        // Космос: медленные колокольчики и арпеджио
        Track.MARS -> Song(76f, 62, listOf(0, 9, 5, 7, 0, 9, 7, 0), listOf(false, true, false, false, false, true, false, false), Wave.BELL, false, true, 0.3f, 21)
    }

    /** Мелодия на 16 тактов, бесшовно зацикленная. */
    fun music(track: Track): ShortArray {
        val sng = song(track)
        val beat = 60f / sng.bpm
        val bars = 16
        val total = beat * 4 * bars
        val b = FloatArray((total * RATE).toInt())
        val rnd = Random(sng.seed)
        val pentatonic = intArrayOf(0, 2, 4, 7, 9)

        // Мелодия: сочиняем 4 такта и повторяем с вариациями (A A' B A'').
        val motif = mutableListOf<Pair<Float, Int>>() // (длительность в долях, ступень)
        var acc = 0f
        var degree = 2
        while (acc < 16f) {
            val dur = when {
                rnd.nextFloat() < sng.busy -> 0.5f
                rnd.nextFloat() < 0.7f -> 1f
                else -> 2f
            }.coerceAtMost(16f - acc)
            degree = (degree + rnd.nextInt(-2, 3)).coerceIn(0, 9)
            motif += dur to (if (rnd.nextFloat() < 0.12f) -1 else degree)
            acc += dur
        }
        for (phrase in 0 until 4) {
            var pos = phrase * 16f
            for ((idx, note) in motif.withIndex()) {
                val (dur, deg0) = note
                val bar = (pos / 4f).toInt() % 8
                var deg = deg0
                if (phrase == 2 && idx % 3 == 0 && deg >= 0) deg = (deg + 2).coerceAtMost(9)
                if (phrase % 2 == 1 && idx == motif.lastIndex) deg = 5 // окончание на тонике
                if (deg >= 0) {
                    val step = pentatonic[deg % 5] + 12 * (deg / 5)
                    val chordRoot = sng.chords[bar]
                    // слегка подтягиваем к аккорду
                    val n = sng.root + 12 + step + (if (step % 12 == (chordRoot + 1) % 12) -1 else 0)
                    tone(
                        b, pos * beat, dur * beat * 0.92f, midi(n), wave = sng.lead, vol = 0.32f,
                        attack = 0.01f, release = 0.06f, decay = if (sng.lead == Wave.BELL) 0.5f else 0.8f, wrap = true,
                    )
                }
                pos += dur
            }
        }

        // Бас и аккорды
        for (bar in 0 until bars) {
            val ch = sng.chords[bar % 8]
            val minor = sng.minor[bar % 8]
            val rootNote = sng.root - 12 + ch
            val t0 = bar * 4 * beat
            for (k in 0 until 2) {
                tone(b, t0 + k * 2 * beat, beat * 1.8f, midi(rootNote), wave = Wave.PLUCK, vol = 0.32f, decay = 0.5f, wrap = true)
            }
            val third = if (minor) 3 else 4
            val chord = listOf(0, third, 7).map { sng.root + ch + it }
            if (sng.arpeggio) {
                for (k in 0 until 8) {
                    val n = chord[k % 3] + if (k >= 3) 12 else 0
                    tone(b, t0 + k * beat * 0.5f, beat * 0.9f, midi(n), wave = Wave.BELL, vol = 0.12f, decay = 0.35f, wrap = true)
                }
            } else {
                for (k in 0 until 4) for (n in chord) {
                    if (k % 2 == 1) tone(b, t0 + k * beat, beat * 0.4f, midi(n), wave = Wave.TRIANGLE, vol = 0.07f, decay = 0.15f, wrap = true)
                }
            }
            if (sng.drums) {
                for (k in 0 until 4) {
                    val tb = t0 + k * beat
                    if (k % 2 == 0) tone(b, tb, 0.18f, 140f, 45f, vol = 0.5f, decay = 0.08f, wrap = true)
                    else noise(b, tb, 0.1f, 0.11f, 0.035f, 0.5f, Random(bar * 4 + k), wrap = true)
                    noise(b, tb + beat * 0.5f, 0.04f, 0.07f, 0.012f, 0.9f, Random(bar * 8 + k), wrap = true, highpass = true)
                }
            }
        }
        return toPcm(b, 0.7f)
    }

    /** Упаковывает PCM в WAV-файл (байты). */
    fun wav(pcm: ShortArray): ByteArray {
        val dataLen = pcm.size * 2
        val out = java.io.ByteArrayOutputStream(44 + dataLen)
        fun i32(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF); out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF) }
        fun i16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        out.write("RIFF".toByteArray()); i32(36 + dataLen); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); i32(16); i16(1); i16(1); i32(RATE); i32(RATE * 2); i16(2); i16(16)
        out.write("data".toByteArray()); i32(dataLen)
        val bytes = ByteArray(dataLen)
        for (i in pcm.indices) {
            val v = pcm[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        out.write(bytes)
        return out.toByteArray()
    }
}
