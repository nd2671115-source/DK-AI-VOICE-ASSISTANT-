package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageSender {
    USER,
    DK,
    SYSTEM
}

enum class ActionType {
    NONE,
    APP_LAUNCH,
    PLAY_STORE_SEARCH,
    YOUTUBE_SEARCH,
    PLAY_FAVOURITE_SONG,
    CHATGPT,
    PHONE_CALL,
    SEND_SMS,
    SEND_WHATSAPP,
    GOOGLE_SEARCH,
    CAMERA,
    GALLERY,
    SETTINGS_SYSTEM,
    SETTINGS_WIFI,
    SETTINGS_BLUETOOTH,
    SETTINGS_APPS,
    TIMER,
    ALARM,
    MAPS_SEARCH,
    DEVICE_INFO,
    SET_FAVOURITE_SONG
}

enum class ActionStatus {
    NONE,
    PENDING_CONFIRMATION,
    CONFIRMED,
    EXECUTED,
    CANCELLED,
    FAILED
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType = ActionType.NONE,
    val actionTitle: String? = null,
    val actionPayload: String? = null, // e.g. package name, query, target phone number, message body
    val actionStatus: ActionStatus = ActionStatus.NONE
)
