package com.example.data.repository

import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class LocalChatRepository(private val chatMessageDao: ChatMessageDao) {

    fun observeMessages(userId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeMessages(userId)

    suspend fun insertMessage(
        userId: String,
        role: String,
        text: String,
        sourceTitle: String? = null,
        mode: String? = null,
        remoteId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis()
    ): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                remoteId = remoteId,
                userId = userId,
                role = role,
                text = text,
                sourceTitle = sourceTitle,
                mode = mode,
                createdAtMillis = createdAtMillis
            )
        )
    }

    suspend fun markSynced(localId: Long, remoteId: String) {
        if (remoteId.isNotBlank()) {
            chatMessageDao.updateRemoteId(localId, remoteId)
        }
    }

    suspend fun syncFromRemote(userId: String, remoteMessages: List<ChatMessage>) {
        val baseTime = System.currentTimeMillis()
        val entities = remoteMessages.mapIndexed { index, msg ->
            ChatMessageEntity(
                remoteId = msg.id.takeIf { it.isNotBlank() },
                userId = userId,
                role = msg.role,
                text = msg.text,
                sourceTitle = msg.sourceTitle,
                mode = msg.mode,
                createdAtMillis = msg.createdAt?.toDate()?.time ?: (baseTime + index)
            )
        }
        chatMessageDao.syncFromRemote(userId, entities)
    }

    suspend fun clearMessages(userId: String) {
        chatMessageDao.clearMessagesForUser(userId)
    }

    suspend fun deleteMessage(localId: Long) {
        chatMessageDao.deleteMessageById(localId)
    }
}
