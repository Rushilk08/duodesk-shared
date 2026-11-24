package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        AssignmentEntity::class,
        DocumentEntity::class,
        NoteEntity::class,
        NotificationEntity::class,
        NotificationSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DuoDeskDatabase : RoomDatabase() {
    abstract fun duoDeskDao(): DuoDeskDao

    companion object {
        @Volatile
        private var INSTANCE: DuoDeskDatabase? = null

        fun getInstance(context: Context): DuoDeskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DuoDeskDatabase::class.java,
                    "duodesk_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
