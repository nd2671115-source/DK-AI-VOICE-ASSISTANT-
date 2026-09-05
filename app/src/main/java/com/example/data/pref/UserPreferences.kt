package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dk_assistant_preferences", Context.MODE_PRIVATE)

    private val _assistantName = MutableStateFlow(prefs.getString(KEY_ASSISTANT_NAME, "DK") ?: "DK")
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _favouriteSong = MutableStateFlow(prefs.getString(KEY_FAVOURITE_SONG, "Kesariya") ?: "Kesariya")
    val favouriteSong: StateFlow<String> = _favouriteSong.asStateFlow()

    private val _preferredMusicApp = MutableStateFlow(prefs.getString(KEY_PREFERRED_MUSIC_APP, "YouTube") ?: "YouTube")
    val preferredMusicApp: StateFlow<String> = _preferredMusicApp.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat(KEY_SPEECH_PITCH, 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _voiceName = MutableStateFlow(prefs.getString(KEY_VOICE_NAME, "") ?: "")
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false))
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _autoSpeak = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SPEAK, true))
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    fun setAssistantName(name: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, name).apply()
        _assistantName.value = name
    }

    fun setFavouriteSong(song: String) {
        prefs.edit().putString(KEY_FAVOURITE_SONG, song).apply()
        _favouriteSong.value = song
    }

    fun setPreferredMusicApp(app: String) {
        prefs.edit().putString(KEY_PREFERRED_MUSIC_APP, app).apply()
        _preferredMusicApp.value = app
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _speechRate.value = rate
    }

    fun setSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
        _speechPitch.value = pitch
    }

    fun setVoiceName(voiceName: String) {
        prefs.edit().putString(KEY_VOICE_NAME, voiceName).apply()
        _voiceName.value = voiceName
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        _wakeWordEnabled.value = enabled
    }

    fun setAutoSpeak(autoSpeak: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, autoSpeak).apply()
        _autoSpeak.value = autoSpeak
    }

    companion object {
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        private const val KEY_FAVOURITE_SONG = "favourite_song"
        private const val KEY_PREFERRED_MUSIC_APP = "preferred_music_app"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_AUTO_SPEAK = "auto_speak"
    }
}
