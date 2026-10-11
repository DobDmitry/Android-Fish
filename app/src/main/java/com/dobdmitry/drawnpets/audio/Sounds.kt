package com.dobdmitry.drawnpets.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Звуки и музыка. Звуки синтезируются один раз при первом запуске
 * ([Synth]) и сохраняются в кэш как WAV-файлы.
 */
object Sounds {
    private const val VERSION = 2
    private const val MUSIC_VOLUME = 0.35f

    private var prefs: SharedPreferences? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<Sfx, Int>()
    private val loaded = HashSet<Int>()
    private var dir: File? = null
    private var ready = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: MediaPlayer? = null
    private var playingTrack: Track? = null
    private var wantedTrack: Track? = null
    private var paused = false
    private var ducked = false

    var musicOn by mutableStateOf(true)
        private set
    var soundOn by mutableStateOf(true)
        private set
    var voiceOn by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("sound", Context.MODE_PRIVATE).also {
            musicOn = it.getBoolean("music", true)
            soundOn = it.getBoolean("sound", true)
            voiceOn = it.getBoolean("voice", false)
        }
        val d = File(app.cacheDir, "audio_v$VERSION")
        dir = d
        val sp = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        sp.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        pool = sp
        scope.launch {
            withContext(Dispatchers.Default) {
                d.mkdirs()
                for (s in Sfx.entries) {
                    val f = File(d, "${s.name}.wav")
                    if (!f.exists()) f.writeBytes(Synth.wav(Synth.sfx(s)))
                }
            }
            for (s in Sfx.entries) ids[s] = sp.load(File(d, "${s.name}.wav").absolutePath, 1)
            withContext(Dispatchers.Default) {
                for (t in Track.entries) {
                    val f = File(d, "${t.name}.wav")
                    if (!f.exists()) f.writeBytes(Synth.wav(Synth.music(t)))
                }
            }
            ready = true
            applyMusic()
        }
    }

    fun play(s: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!soundOn) return
        val p = pool ?: return
        val id = ids[s] ?: return
        if (id !in loaded) return
        val v = volume.coerceIn(0f, 1f) * 0.8f
        p.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** Какую мелодию играть на текущем экране (null — тишина). */
    fun music(track: Track?) {
        wantedTrack = track
        applyMusic()
    }

    fun pause() {
        paused = true
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        paused = false
        applyMusic()
    }

    /** Приглушить музыку, пока говорит голос. */
    fun duck(on: Boolean) {
        ducked = on
        val v = if (on) MUSIC_VOLUME * 0.3f else MUSIC_VOLUME
        try { player?.setVolume(v, v) } catch (e: IllegalStateException) {}
    }

    private fun applyMusic() {
        val track = wantedTrack
        if (!ready || paused || !musicOn || track == null) {
            if (!musicOn || track == null) stopPlayer() else player?.takeIf { it.isPlaying }?.pause()
            return
        }
        if (playingTrack == track && player != null) {
            if (player?.isPlaying == false) player?.start()
            return
        }
        stopPlayer()
        val f = File(dir ?: return, "${track.name}.wav")
        if (!f.exists()) return
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(f.absolutePath)
            mp.isLooping = true
            mp.prepare()
            val v = if (ducked) MUSIC_VOLUME * 0.3f else MUSIC_VOLUME
            mp.setVolume(v, v)
            mp.start()
            player = mp
            playingTrack = track
        } catch (e: Exception) {
            player = null
            playingTrack = null
        }
    }

    private fun stopPlayer() {
        player?.let {
            try { it.stop() } catch (e: IllegalStateException) {}
            it.release()
        }
        player = null
        playingTrack = null
    }

    fun setMusic(on: Boolean) {
        musicOn = on
        prefs?.edit()?.putBoolean("music", on)?.apply()
        applyMusic()
    }

    fun setSound(on: Boolean) {
        soundOn = on
        prefs?.edit()?.putBoolean("sound", on)?.apply()
    }

    fun setVoice(on: Boolean) {
        voiceOn = on
        prefs?.edit()?.putBoolean("voice", on)?.apply()
        if (!on) Voice.stop()
    }
}
