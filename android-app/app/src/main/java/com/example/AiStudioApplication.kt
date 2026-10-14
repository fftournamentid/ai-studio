package com.example

import android.app.Application
import com.example.audio.AudioPlayerManager
import com.example.data.local.AppDatabase
import com.example.data.repository.GenerationRepository
import com.example.data.repository.ProjectRepository
import com.example.data.security.SecureStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AiStudioApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var secureStorage: SecureStorageManager
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var generationRepository: GenerationRepository
        private set

    lateinit var audioPlayerManager: AudioPlayerManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        secureStorage = SecureStorageManager.getInstance(this)
        projectRepository = ProjectRepository(this, database)
        generationRepository = GenerationRepository(this, database, secureStorage)
        audioPlayerManager = AudioPlayerManager(this)

        // Seed sample project and assets so first launch is immediate and fully functional
        applicationScope.launch {
            projectRepository.seedInitialProjectIfEmpty()
        }
    }

    companion object {
        lateinit var instance: AiStudioApplication
            private set
    }
}
