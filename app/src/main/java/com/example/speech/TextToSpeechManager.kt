package com.example.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f
    private var selectedVoiceName: String = ""

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupDefaultVoice()
                onInitComplete(true)
            } else {
                isInitialized = false
                onInitComplete(false)
            }
        }
    }

    private fun setupDefaultVoice() {
        val ttsEngine = tts ?: return
        try {
            // Set listeners
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })

            // Populate available voices
            val voices = ttsEngine.voices?.toList() ?: emptyList()
            _availableVoices.value = voices

            // Select preferred Hindi/Indian voice if available
            val hindiVoice = voices.firstOrNull { voice ->
                val loc = voice.locale
                loc.language == "hi" && loc.country == "IN" && !voice.isNetworkConnectionRequired
            } ?: voices.firstOrNull { voice ->
                voice.locale.language == "hi"
            } ?: voices.firstOrNull { voice ->
                voice.locale.language == "en" && voice.locale.country == "IN"
            }

            if (hindiVoice != null) {
                ttsEngine.voice = hindiVoice
                selectedVoiceName = hindiVoice.name
            } else {
                val hindiLocale = Locale("hi", "IN")
                val res = ttsEngine.setLanguage(hindiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsEngine.setLanguage(Locale.getDefault())
                }
            }

            ttsEngine.setSpeechRate(speechRate)
            ttsEngine.setPitch(speechPitch)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applySettings(rate: Float, pitch: Float, voiceName: String) {
        this.speechRate = rate
        this.speechPitch = pitch
        this.selectedVoiceName = voiceName

        val ttsEngine = tts ?: return
        if (!isInitialized) return

        ttsEngine.setSpeechRate(rate)
        ttsEngine.setPitch(pitch)

        if (voiceName.isNotBlank()) {
            val matchingVoice = ttsEngine.voices?.firstOrNull { it.name == voiceName }
            if (matchingVoice != null) {
                ttsEngine.voice = matchingVoice
            }
        }
    }

    fun speak(text: String, onFinish: (() -> Unit)? = null) {
        val ttsEngine = tts
        if (ttsEngine == null || !isInitialized) {
            onFinish?.invoke()
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        _isSpeaking.value = true
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
