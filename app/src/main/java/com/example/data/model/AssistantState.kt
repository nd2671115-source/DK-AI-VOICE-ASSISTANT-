package com.example.data.model

sealed interface AssistantMode {
    object Idle : AssistantMode
    data class Listening(val rmsDb: Float = 0f) : AssistantMode
    object Thinking : AssistantMode
    data class Speaking(val progress: Float = 0f) : AssistantMode
    data class Error(val message: String) : AssistantMode
}

data class AssistantStatus(
    val mode: AssistantMode = AssistantMode.Idle,
    val statusText: String = "DK is ready",
    val activeQuery: String = "",
    val isOnline: Boolean = true,
    val ttsAvailable: Boolean = true,
    val sttAvailable: Boolean = true
)
