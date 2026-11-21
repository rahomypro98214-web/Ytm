package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatConversationDao {
    @Query("SELECT * FROM chat_conversations WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeConversations(userId: String): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    suspend fun getConversationsOnce(userId: String): List<ChatConversationEntity>

    @Query("SELECT * FROM chat_conversations WHERE userId = :userId AND conversationId = :conversationId LIMIT 1")
    suspend fun getConversation(userId: String, conversationId: String): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ChatConversationEntity)

    @Query(
        "UPDATE chat_conversations SET title = :title, isSaved = 1, updatedAtMillis = :updatedAtMillis " +
            "WHERE userId = :userId AND conversationId = :conversationId"
    )
    suspend fun renameConversation(
        userId: String,
        conversationId: String,
        title: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM chat_conversations WHERE userId = :userId AND conversationId = :conversationId")
    suspend fun deleteConversation(userId: String, conversationId: String)

    @Query("DELETE FROM chat_conversations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
