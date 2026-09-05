package com.example.data.repository

import com.example.data.db.ChatDao
import com.example.data.model.ActionStatus
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message)
    }

    suspend fun updateActionStatus(id: Long, status: ActionStatus) {
        chatDao.updateActionStatus(id, status)
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessage(id)
    }
}
