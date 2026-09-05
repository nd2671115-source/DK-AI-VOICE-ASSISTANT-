package com.example.viewmodel

import android.app.Application
import android.provider.Settings
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DkApplication
import com.example.ai.AIService
import com.example.ai.CommandParser
import com.example.ai.ParsedCommand
import com.example.data.model.ActionStatus
import com.example.data.model.ActionType
import com.example.data.model.AppInfo
import com.example.data.model.AssistantMode
import com.example.data.model.AssistantStatus
import com.example.data.model.ChatMessage
import com.example.data.model.ContactInfo
import com.example.data.model.MessageSender
import com.example.data.pref.UserPreferences
import com.example.data.repository.ChatRepository
import com.example.engine.DeviceActionsManager
import com.example.service.DkWakeWordService
import com.example.speech.SpeechRecognizerManager
import com.example.speech.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DkApplication
    val repository: ChatRepository = app.chatRepository
    val userPreferences: UserPreferences = app.userPreferences
    private val aiService = AIService()

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _assistantStatus = MutableStateFlow(
        AssistantStatus(
            mode = AssistantMode.Idle,
            statusText = "DK is ready",
            isOnline = true
        )
    )
    val assistantStatus: StateFlow<AssistantStatus> = _assistantStatus.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ChatMessage?>(null)
    val pendingConfirmation: StateFlow<ChatMessage?> = _pendingConfirmation.asStateFlow()

    lateinit var speechRecognizerManager: SpeechRecognizerManager
    lateinit var textToSpeechManager: TextToSpeechManager

    init {
        initSpeechAndTTS()
        refreshInstalledApps()
    }

    private fun initSpeechAndTTS() {
        textToSpeechManager = TextToSpeechManager(getApplication()) { success ->
            _assistantStatus.value = _assistantStatus.value.copy(ttsAvailable = success)
            if (success) {
                applyCurrentTtsSettings()
            }
        }

        speechRecognizerManager = SpeechRecognizerManager(
            context = getApplication(),
            onResult = { recognizedText ->
                _assistantStatus.value = _assistantStatus.value.copy(
                    mode = AssistantMode.Idle,
                    statusText = "DK is ready"
                )
                processUserQuery(recognizedText)
            },
            onError = { errorMsg ->
                _assistantStatus.value = _assistantStatus.value.copy(
                    mode = AssistantMode.Idle,
                    statusText = errorMsg
                )
            }
        )

        // Monitor listening state for live RMS dB orb glow
        viewModelScope.launch {
            speechRecognizerManager.isListening.collect { listening ->
                if (listening) {
                    _assistantStatus.value = _assistantStatus.value.copy(
                        mode = AssistantMode.Listening(0f),
                        statusText = "DK is listening..."
                    )
                } else if (_assistantStatus.value.mode is AssistantMode.Listening) {
                    _assistantStatus.value = _assistantStatus.value.copy(
                        mode = AssistantMode.Idle,
                        statusText = "DK is ready"
                    )
                }
            }
        }

        viewModelScope.launch {
            speechRecognizerManager.rmsDb.collect { rms ->
                if (_assistantStatus.value.mode is AssistantMode.Listening) {
                    _assistantStatus.value = _assistantStatus.value.copy(
                        mode = AssistantMode.Listening(rms)
                    )
                }
            }
        }

        // Monitor TTS speaking state
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _assistantStatus.value = _assistantStatus.value.copy(
                        mode = AssistantMode.Speaking(1.0f),
                        statusText = "DK is speaking..."
                    )
                } else if (_assistantStatus.value.mode is AssistantMode.Speaking) {
                    _assistantStatus.value = _assistantStatus.value.copy(
                        mode = AssistantMode.Idle,
                        statusText = "DK is ready"
                    )
                }
            }
        }
    }

    fun applyCurrentTtsSettings() {
        textToSpeechManager.applySettings(
            rate = userPreferences.speechRate.value,
            pitch = userPreferences.speechPitch.value,
            voiceName = userPreferences.voiceName.value
        )
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = DeviceActionsManager.getInstalledApps(getApplication())
            _installedApps.value = apps
        }
    }

    fun startListening() {
        textToSpeechManager.stop()
        _assistantStatus.value = _assistantStatus.value.copy(
            mode = AssistantMode.Listening(0f),
            statusText = "DK is listening..."
        )
        speechRecognizerManager.startListening()
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
        _assistantStatus.value = _assistantStatus.value.copy(
            mode = AssistantMode.Idle,
            statusText = "DK is ready"
        )
    }

    fun toggleListening() {
        if (speechRecognizerManager.isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun processUserQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // 1. Record User Message
            val userMsg = ChatMessage(
                text = trimmed,
                sender = MessageSender.USER
            )
            repository.insertMessage(userMsg)

            // 2. Set Status to Thinking
            _assistantStatus.value = _assistantStatus.value.copy(
                mode = AssistantMode.Thinking,
                statusText = "DK is thinking...",
                activeQuery = trimmed
            )

            // 3. Parse command locally with high-precision engine
            val parsed = CommandParser.parse(trimmed)
            executeParsedCommand(parsed, trimmed)
        }
    }

    private suspend fun executeParsedCommand(command: ParsedCommand, rawQuery: String) {
        val context = getApplication<Application>()

        when (command) {
            is ParsedCommand.LaunchApp -> {
                val app = DeviceActionsManager.findAppByName(context, command.appQuery)
                if (app != null) {
                    val reply = "Opening ${app.appName}..."
                    addAssistantResponse(
                        text = reply,
                        actionType = ActionType.APP_LAUNCH,
                        actionTitle = "Open ${app.appName}",
                        actionPayload = app.packageName,
                        actionStatus = ActionStatus.EXECUTED
                    )
                    speak(reply)
                    DeviceActionsManager.launchApp(context, app.packageName)
                } else {
                    val reply = "Mujhe '${command.appQuery}' app nahi mila. Kya aap ise Play Store par search karna chahte hain?"
                    addAssistantResponse(
                        text = reply,
                        actionType = ActionType.PLAY_STORE_SEARCH,
                        actionTitle = "Search on Play Store",
                        actionPayload = command.appQuery,
                        actionStatus = ActionStatus.PENDING_CONFIRMATION
                    )
                    speak(reply)
                }
            }

            is ParsedCommand.SearchPlayStore -> {
                val reply = "Searching '${command.appName}' on Google Play Store..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.PLAY_STORE_SEARCH,
                    actionTitle = "Open Play Store",
                    actionPayload = command.appName,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.searchPlayStore(context, command.appName)
            }

            is ParsedCommand.SearchYouTube -> {
                val reply = "YouTube par search kar raha hoon: ${command.query}"
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.YOUTUBE_SEARCH,
                    actionTitle = "Search on YouTube",
                    actionPayload = command.query,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openYouTube(context, command.query)
            }

            is ParsedCommand.OpenYouTube -> {
                val reply = "YouTube khol raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.APP_LAUNCH,
                    actionTitle = "Open YouTube",
                    actionPayload = "com.google.android.youtube",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openYouTube(context, null)
            }

            is ParsedCommand.SetFavouriteSong -> {
                userPreferences.setFavouriteSong(command.songName)
                val reply = "Aapka favourite song '${command.songName}' save kar liya gaya hai."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.SET_FAVOURITE_SONG,
                    actionTitle = "Favourite Song: ${command.songName}",
                    actionPayload = command.songName,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
            }

            is ParsedCommand.PlayFavouriteSong -> {
                val favSong = userPreferences.favouriteSong.value
                val musicApp = userPreferences.preferredMusicApp.value
                val reply = "Aapka favourite song '$favSong' $musicApp par play kar raha hoon."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.PLAY_FAVOURITE_SONG,
                    actionTitle = "Playing $favSong on $musicApp",
                    actionPayload = "$musicApp|$favSong",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.playMusic(context, musicApp, favSong)
            }

            is ParsedCommand.OpenChatGPT -> {
                val reply = "ChatGPT khol raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.CHATGPT,
                    actionTitle = "Open ChatGPT",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openChatGPT(context)
            }

            is ParsedCommand.MakeCall -> {
                val contacts = DeviceActionsManager.searchContacts(context, command.target)
                val targetContact = contacts.firstOrNull()
                val targetNumber = targetContact?.phoneNumber ?: if (command.target.any { it.isDigit() }) command.target else ""

                if (targetNumber.isNotBlank()) {
                    val displayName = targetContact?.name ?: command.target
                    val reply = "$displayName ko call lagana hai? Confirm karne ke liye dial karein."
                    val msg = ChatMessage(
                        text = reply,
                        sender = MessageSender.DK,
                        actionType = ActionType.PHONE_CALL,
                        actionTitle = "Call $displayName",
                        actionPayload = targetNumber,
                        actionStatus = ActionStatus.PENDING_CONFIRMATION
                    )
                    val id = repository.insertMessage(msg)
                    _pendingConfirmation.value = msg.copy(id = id)
                    speak(reply)
                } else {
                    val reply = "Contacts me '${command.target}' nahi mila. Kripya number ya sahi naam bolein."
                    addAssistantResponse(
                        text = reply,
                        actionType = ActionType.NONE,
                        actionStatus = ActionStatus.FAILED
                    )
                    speak(reply)
                }
            }

            is ParsedCommand.SendMessage -> {
                val contacts = DeviceActionsManager.searchContacts(context, command.target)
                val targetContact = contacts.firstOrNull()
                val targetNumber = targetContact?.phoneNumber ?: if (command.target.any { it.isDigit() }) command.target else ""
                val displayName = targetContact?.name ?: command.target
                val appName = if (command.isWhatsApp) "WhatsApp" else "SMS"

                val reply = "$displayName ko $appName message bhejna hai: '${command.messageText}'. Send karein?"
                val msg = ChatMessage(
                    text = reply,
                    sender = MessageSender.DK,
                    actionType = if (command.isWhatsApp) ActionType.SEND_WHATSAPP else ActionType.SEND_SMS,
                    actionTitle = "Send $appName to $displayName",
                    actionPayload = "$targetNumber|${command.messageText}",
                    actionStatus = ActionStatus.PENDING_CONFIRMATION
                )
                val id = repository.insertMessage(msg)
                _pendingConfirmation.value = msg.copy(id = id)
                speak(reply)
            }

            is ParsedCommand.GoogleSearch -> {
                val reply = "Google par search kar raha hoon: ${command.query}"
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.GOOGLE_SEARCH,
                    actionTitle = "Search: ${command.query}",
                    actionPayload = command.query,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.searchGoogle(context, command.query)
            }

            is ParsedCommand.OpenCamera -> {
                val reply = "Camera khol raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.CAMERA,
                    actionTitle = "Open Camera",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openCamera(context)
            }

            is ParsedCommand.OpenGallery -> {
                val reply = "Gallery khol raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.GALLERY,
                    actionTitle = "Open Gallery",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openGallery(context)
            }

            is ParsedCommand.OpenSettings -> {
                val action = when (command.settingsType) {
                    ActionType.SETTINGS_WIFI -> Settings.ACTION_WIFI_SETTINGS
                    ActionType.SETTINGS_BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
                    ActionType.SETTINGS_APPS -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
                val reply = "Settings open kar raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = command.settingsType,
                    actionTitle = "Open Settings",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openSettings(context, action)
            }

            is ParsedCommand.SetTimer -> {
                val totalSec = command.minutes * 60 + command.seconds
                val reply = "${command.minutes} minute ka timer set kar raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.TIMER,
                    actionTitle = "Set ${command.minutes}m Timer",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.setTimer(context, totalSec, command.label)
            }

            is ParsedCommand.SetAlarm -> {
                val timeStr = String.format("%02d:%02d", command.hour, command.minute)
                val reply = "$timeStr baje ka alarm set kar raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.ALARM,
                    actionTitle = "Set Alarm for $timeStr",
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.setAlarm(context, command.hour, command.minute, command.label)
            }

            is ParsedCommand.OpenMaps -> {
                val reply = "${command.query} ke liye maps khol raha hoon..."
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.MAPS_SEARCH,
                    actionTitle = "Open Maps: ${command.query}",
                    actionPayload = command.query,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
                DeviceActionsManager.openMaps(context, command.query)
            }

            is ParsedCommand.DeviceInfo -> {
                val info = DeviceActionsManager.getDeviceStatusInfo(context)
                val reply = when (command.queryType) {
                    "battery" -> "Battery ${info.batteryPercent}% hai" + if (info.isCharging) " (Charging ho raha hai)." else "."
                    "time" -> "Abhi time ${info.timeFormatted} hua hai."
                    "date" -> "Aaj ${info.dateFormatted} hai."
                    "network" -> if (info.isNetworkConnected) "Phone ${info.networkType} se connected hai." else "Phone offline hai, internet se connect nahi hai."
                    else -> "Battery: ${info.batteryPercent}%, Time: ${info.timeFormatted}, Network: ${info.networkType}."
                }
                addAssistantResponse(
                    text = reply,
                    actionType = ActionType.DEVICE_INFO,
                    actionTitle = "Device Status",
                    actionPayload = reply,
                    actionStatus = ActionStatus.EXECUTED
                )
                speak(reply)
            }

            is ParsedCommand.ConversationalQuery -> {
                // Query Gemini AI with full bilingual conversational power
                val recent = chatMessages.value
                val aiReply = aiService.generateResponse(command.rawText, recent)
                addAssistantResponse(
                    text = aiReply,
                    actionType = ActionType.NONE,
                    actionStatus = ActionStatus.NONE
                )
                speak(aiReply)
            }
        }

        _assistantStatus.value = _assistantStatus.value.copy(
            mode = AssistantMode.Idle,
            statusText = "DK is ready"
        )
    }

    private suspend fun addAssistantResponse(
        text: String,
        actionType: ActionType = ActionType.NONE,
        actionTitle: String? = null,
        actionPayload: String? = null,
        actionStatus: ActionStatus = ActionStatus.NONE
    ) {
        val msg = ChatMessage(
            text = text,
            sender = MessageSender.DK,
            actionType = actionType,
            actionTitle = actionTitle,
            actionPayload = actionPayload,
            actionStatus = actionStatus
        )
        repository.insertMessage(msg)
    }

    fun confirmPendingAction(message: ChatMessage) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            repository.updateActionStatus(message.id, ActionStatus.EXECUTED)
            _pendingConfirmation.value = null

            when (message.actionType) {
                ActionType.PHONE_CALL -> {
                    message.actionPayload?.let { phone ->
                        DeviceActionsManager.openDialer(context, phone)
                    }
                }
                ActionType.SEND_WHATSAPP -> {
                    val parts = message.actionPayload?.split("|") ?: emptyList()
                    val phone = parts.getOrNull(0)
                    val text = parts.getOrNull(1) ?: ""
                    DeviceActionsManager.prepareWhatsAppMessage(context, phone, text)
                }
                ActionType.SEND_SMS -> {
                    val parts = message.actionPayload?.split("|") ?: emptyList()
                    val phone = parts.getOrNull(0)
                    val text = parts.getOrNull(1) ?: ""
                    DeviceActionsManager.sendSms(context, phone, text)
                }
                ActionType.PLAY_STORE_SEARCH -> {
                    message.actionPayload?.let { appName ->
                        DeviceActionsManager.searchPlayStore(context, appName)
                    }
                }
                else -> {}
            }
        }
    }

    fun cancelPendingAction(message: ChatMessage) {
        viewModelScope.launch {
            repository.updateActionStatus(message.id, ActionStatus.CANCELLED)
            _pendingConfirmation.value = null
            speak("Action cancel kar diya gaya.")
        }
    }

    private fun speak(text: String) {
        if (userPreferences.autoSpeak.value) {
            textToSpeechManager.speak(text)
        }
    }

    fun testTtsVoice(sample: String = "Namaste! Main DK Assistant hoon. Main aapki kya madad kar sakta hoon?") {
        textToSpeechManager.speak(sample)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            speak("Chat history clear kar di gayi hai.")
        }
    }

    fun toggleWakeWordService(enabled: Boolean) {
        userPreferences.setWakeWordEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            DkWakeWordService.start(context)
        } else {
            DkWakeWordService.stop(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
        textToSpeechManager.destroy()
    }
}
