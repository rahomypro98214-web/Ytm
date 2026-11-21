package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query(
        "SELECT * FROM chat_messages WHERE userId = :userId AND conversationId = '$DEFAULT_CONVERSATION_ID' " +
            "ORDER BY createdAtMillis ASC, localId ASC"
    )
    fun observeMessages(userId: String): Flow<List<ChatMessageEntity>>

    @Query(
        "SELECT * FROM chat_messages WHERE userId = :userId AND conversationId = :conversationId " +
            "ORDER BY createdAtMillis ASC, localId ASC"
    )
    fun observeMessagesForConversation(userId: String, conversationId: String): Flow<List<ChatMessageEntity>>

    @Query(
        "SELECT * FROM chat_messages WHERE userId = :userId AND conversationId = '$DEFAULT_CONVERSATION_ID' " +
            "ORDER BY createdAtMillis ASC, localId ASC"
    )
    suspend fun getMessagesOnce(userId: String): List<ChatMessageEntity>

    @Query(
        "SELECT * FROM chat_messages WHERE userId = :userId AND conversationId = :conversationId " +
            "ORDER BY createdAtMillis ASC, localId ASC"
    )
    suspend fun getMessagesForConversationOnce(userId: String, conversationId: String): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE userId = :userId AND conversationId = :conversationId")
    suspend fun countMessagesForConversation(userId: String, conversationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE userId = :userId AND conversationId = :conversationId")
    suspend fun clearMessagesForConversation(userId: String, conversationId: String)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearMessagesForUser(userId: String)

    @Query("DELETE FROM chat_messages WHERE localId = :localId")
    suspend fun deleteMessageById(localId: Long)

    @Query("UPDATE chat_messages SET remoteId = :remoteId WHERE localId = :localId")
    suspend fun updateRemoteId(localId: Long, remoteId: String)

    @Query(
        "UPDATE chat_messages SET conversationId = :toConversationId " +
            "WHERE userId = :userId AND conversationId = :fromConversationId"
    )
    suspend fun reassignConversation(
        userId: String,
        fromConversationId: String,
        toConversationId: String
    )

    @Transaction
    suspend fun syncFromRemote(userId: String, remoteMessages: List<ChatMessageEntity>) {
        val currentLocal = getMessagesForConversationOnce(userId, DEFAULT_CONVERSATION_ID)
        val pendingLocal = currentLocal.filter { it.remoteId.isNullOrBlank() }.toMutableList()

        val entitiesToSave = remoteMessages.map { remote ->
            val matchedPendingIndex = pendingLocal.indexOfFirst { local ->
                local.role == remote.role && local.text == remote.text
            }
            if (matchedPendingIndex >= 0) {
                val matched = pendingLocal.removeAt(matchedPendingIndex)
                remote.copy(
                    localId = matched.localId,
                    conversationId = DEFAULT_CONVERSATION_ID,
                    createdAtMillis = minOf(matched.createdAtMillis, remote.createdAtMillis)
                )
            } else {
                val existingSynced = currentLocal.firstOrNull {
                    !it.remoteId.isNullOrBlank() && it.remoteId == remote.remoteId
                }
                if (existingSynced != null) {
                    remote.copy(
                        localId = existingSynced.localId,
                        conversationId = DEFAULT_CONVERSATION_ID
                    )
                } else {
                    remote.copy(conversationId = DEFAULT_CONVERSATION_ID)
                }
            }
        }

        val merged = entitiesToSave + pendingLocal
        clearMessagesForConversation(userId, DEFAULT_CONVERSATION_ID)
        if (merged.isNotEmpty()) {
            insertAll(merged)
        }
    }

    @Transaction
    suspend fun replaceForUser(userId: String, messages: List<ChatMessageEntity>) {
        clearMessagesForUser(userId)
        if (messages.isNotEmpty()) {
            insertAll(messages)
        }
    }
}
