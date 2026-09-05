package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.pref.UserPreferences
import com.example.data.repository.ChatRepository

class DkApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }
    val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
