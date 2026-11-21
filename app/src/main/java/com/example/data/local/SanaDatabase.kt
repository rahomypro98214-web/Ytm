package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class SanaDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: SanaDatabase? = null

        fun getInstance(context: Context): SanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SanaDatabase::class.java,
                    "sana_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
