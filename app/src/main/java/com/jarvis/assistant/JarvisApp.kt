package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.data.preferences.AppPreferences
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.data.repository.ReminderRepository
import com.jarvis.assistant.memory.MemoryManager
import com.jarvis.assistant.memory.MemoryRepository

class JarvisApp : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var memoryRepository: MemoryRepository
        private set

    lateinit var memoryManager: MemoryManager
        private set

    lateinit var reminderRepository: ReminderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)
        chatRepository = ChatRepository(preferences)
        memoryRepository = MemoryRepository(preferences)
        memoryManager = MemoryManager(memoryRepository)
        reminderRepository = ReminderRepository(this, preferences)
    }

    companion object {
        lateinit var instance: JarvisApp
            private set
    }
}
