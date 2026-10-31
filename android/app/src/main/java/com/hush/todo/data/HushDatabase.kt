package com.hush.todo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hush.todo.data.entities.ExtractionEntity
import com.hush.todo.data.entities.MessageEntity
import com.hush.todo.data.entities.ProjectEntity
import com.hush.todo.data.entities.TaskEntity
import com.hush.todo.data.entities.TranscriptEntity

@Database(
    entities = [ProjectEntity::class, TaskEntity::class, MessageEntity::class, TranscriptEntity::class, ExtractionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HushDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao
    abstract fun tasks(): TaskDao
    abstract fun messages(): MessageDao
    abstract fun transcripts(): TranscriptDao
    abstract fun extractions(): ExtractionDao

    companion object {
        @Volatile private var INSTANCE: HushDatabase? = null
        fun get(ctx: Context): HushDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    HushDatabase::class.java,
                    "hush.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
