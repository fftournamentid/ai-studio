package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GenerationLogEntity
import com.example.data.model.ProjectAssetEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoTaskEntity
import com.example.data.model.VoiceTaskEntity

@Database(
    entities = [
        ProjectEntity::class,
        ProjectAssetEntity::class,
        VideoTaskEntity::class,
        VoiceTaskEntity::class,
        GenerationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectAssetDao(): ProjectAssetDao
    abstract fun videoTaskDao(): VideoTaskDao
    abstract fun voiceTaskDao(): VoiceTaskDao
    abstract fun generationLogDao(): GenerationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_studio_generator.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
