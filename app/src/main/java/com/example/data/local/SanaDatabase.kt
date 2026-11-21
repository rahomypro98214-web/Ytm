package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, ChatConversationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SanaDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatConversationDao(): ChatConversationDao

    companion object {
        @Volatile
        private var INSTANCE: SanaDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT '$DEFAULT_CONVERSATION_ID'"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_userId_conversationId " +
                        "ON chat_messages (userId, conversationId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        conversationId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        previewText TEXT NOT NULL,
                        messageCount INTEGER NOT NULL,
                        isSaved INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(userId, conversationId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_conversations_userId_updatedAtMillis " +
                        "ON chat_conversations (userId, updatedAtMillis)"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO chat_conversations (
                        conversationId, userId, title, previewText, messageCount, isSaved, createdAtMillis, updatedAtMillis
                    )
                    SELECT
                        '$DEFAULT_CONVERSATION_ID',
                        userId,
                        'محادثة سابقة',
                        (SELECT m2.text FROM chat_messages m2 WHERE m2.userId = m1.userId ORDER BY m2.createdAtMillis DESC, m2.localId DESC LIMIT 1),
                        COUNT(*),
                        1,
                        MIN(createdAtMillis),
                        MAX(createdAtMillis)
                    FROM chat_messages m1
                    GROUP BY userId
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SanaDatabase::class.java,
                    "sana_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
