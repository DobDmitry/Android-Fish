package com.dobdmitry.drawnpets.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Озвучка фраз синтезатором речи Android — для детей, которые ещё не читают.
 * Если русского голоса на телефоне нет, просто молчит.
 */
object Voice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var counter = 0
    private val main = Handler(Looper.getMainLooper())

    /** Говорит ли сейчас кто-нибудь (для анимации рта). */
    @Volatile
    var speaking = false
        private set

    private val emoji = Regex("[\\p{So}\\p{Cs}\\p{Sk}\\uFE0F\\u200D«»…*]")

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            val res = engine.setLanguage(Locale("ru", "RU"))
            ready = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speaking = true
                    main.post { Sounds.duck(true) }
                }

                override fun onDone(utteranceId: String?) {
                    speaking = false
                    main.post { Sounds.duck(false) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speaking = false
                    main.post { Sounds.duck(false) }
                }
            })
        }
    }

    /** Произнести фразу. [pitch] — высота голоса: у каждого героя своя. */
    fun say(text: String, pitch: Float = 1f, rate: Float = 0.95f) {
        if (!ready || !Sounds.voiceOn) return
        val engine = tts ?: return
        val clean = emoji.replace(text, " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)
        engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "u${counter++}")
    }

    fun stop() {
        tts?.stop()
        speaking = false
        Sounds.duck(false)
    }
}
