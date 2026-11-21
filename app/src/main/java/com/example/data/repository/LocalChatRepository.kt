package com.example.data.repository

import com.example.data.local.ChatConversationDao
import com.example.data.local.ChatConversationEntity
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DEFAULT_CONVERSATION_ID
import com.example.data.model.ChatMessage
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalChatRepository(
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ChatConversationDao? = null
) {

    fun observeMessages(userId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeMessages(userId)

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeMessagesForConversation(userId, conversationId)

    fun observeConversations(userId: String): Flow<List<ChatConversationEntity>> =
        conversationDao?.observeConversations(userId) ?: flowOf(emptyList())

    fun createNewConversationId(): String = "conv_${UUID.randomUUID()}"

    suspend fun insertMessage(
        userId: String,
        role: String,
        text: String,
        sourceTitle: String? = null,
        mode: String? = null,
        remoteId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        conversationId: String = DEFAULT_CONVERSATION_ID
    ): Long {
        val insertedId = chatMessageDao.insertMessage(
            ChatMessageEntity(
                remoteId = remoteId,
                userId = userId,
                conversationId = conversationId,
                role = role,
                text = text,
                sourceTitle = sourceTitle,
                mode = mode,
                createdAtMillis = createdAtMillis
            )
        )
        upsertConversationMetadata(
            userId = userId,
            conversationId = conversationId,
            latestRole = role,
            latestText = text,
            timestampMillis = createdAtMillis
        )
        return insertedId
    }

    suspend fun saveConversation(
        userId: String,
        sourceConversationId: String,
        customTitle: String? = null
    ): String {
        val messages = chatMessageDao.getMessagesForConversationOnce(userId, sourceConversationId)
        val existing = conversationDao?.getConversation(userId, sourceConversationId)
        val firstUserText = messages.firstOrNull { it.role == "user" }?.text
            ?: messages.firstOrNull()?.text
            ?: existing?.title
            ?: "محادثة محفوظة"
        val resolvedTitle = customTitle
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(60)
            ?: existing?.title?.takeIf { it.isNotBlank() }
            ?: deriveTitle(firstUserText)
        val latestText = messages.lastOrNull()?.text?.trim()?.take(80)
            ?: existing?.previewText
            ?: ""
        val now = System.currentTimeMillis()
        val createdAt = messages.firstOrNull()?.createdAtMillis ?: existing?.createdAtMillis ?: now
        val updatedAt = maxOf(now, messages.lastOrNull()?.createdAtMillis ?: now)

        val targetConversationId = if (sourceConversationId == DEFAULT_CONVERSATION_ID) {
            createNewConversationId()
        } else {
            sourceConversationId
        }

        if (targetConversationId != sourceConversationId) {
            chatMessageDao.reassignConversation(
                userId = userId,
                fromConversationId = sourceConversationId,
                toConversationId = targetConversationId
            )
            conversationDao?.deleteConversation(userId, sourceConversationId)
        }

        conversationDao?.upsertConversation(
            ChatConversationEntity(
                conversationId = targetConversationId,
                userId = userId,
                title = resolvedTitle,
                previewText = latestText,
                messageCount = messages.size,
                isSaved = true,
                createdAtMillis = createdAt,
                updatedAtMillis = updatedAt
            )
        )
        return targetConversationId
    }

    suspend fun deleteConversation(userId: String, conversationId: String) {
        chatMessageDao.clearMessagesForConversation(userId, conversationId)
        conversationDao?.deleteConversation(userId, conversationId)
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
                conversationId = DEFAULT_CONVERSATION_ID,
                role = msg.role,
                text = msg.text,
                sourceTitle = msg.sourceTitle,
                mode = msg.mode,
                createdAtMillis = msg.createdAt?.toDate()?.time ?: (baseTime + index)
            )
        }
        chatMessageDao.syncFromRemote(userId, entities)

        val syncedMessages = chatMessageDao.getMessagesForConversationOnce(userId, DEFAULT_CONVERSATION_ID)
        if (syncedMessages.isNotEmpty() && conversationDao != null) {
            val existing = conversationDao.getConversation(userId, DEFAULT_CONVERSATION_ID)
            val firstUserText = syncedMessages.firstOrNull { it.role == "user" }?.text
                ?: syncedMessages.first().text
            val latest = syncedMessages.last()
            conversationDao.upsertConversation(
                ChatConversationEntity(
                    conversationId = DEFAULT_CONVERSATION_ID,
                    userId = userId,
                    title = existing?.title?.takeIf { it.isNotBlank() } ?: deriveTitle(firstUserText),
                    previewText = latest.text.trim().take(80),
                    messageCount = syncedMessages.size,
                    isSaved = existing?.isSaved ?: true,
                    createdAtMillis = existing?.createdAtMillis ?: syncedMessages.first().createdAtMillis,
                    updatedAtMillis = maxOf(existing?.updatedAtMillis ?: 0L, latest.createdAtMillis)
                )
            )
        }
    }

    suspend fun clearMessages(userId: String, conversationId: String = DEFAULT_CONVERSATION_ID) {
        chatMessageDao.clearMessagesForConversation(userId, conversationId)
        conversationDao?.deleteConversation(userId, conversationId)
    }

    suspend fun deleteMessage(localId: Long) {
        chatMessageDao.deleteMessageById(localId)
    }

    private suspend fun upsertConversationMetadata(
        userId: String,
        conversationId: String,
        latestRole: String,
        latestText: String,
        timestampMillis: Long
    ) {
        val dao = conversationDao ?: return
        val existing = dao.getConversation(userId, conversationId)
        val count = chatMessageDao.countMessagesForConversation(userId, conversationId)
        val cleanedPreview = latestText.replace('\n', ' ').trim().take(80)
        val title = when {
            existing != null && existing.title.isNotBlank() -> existing.title
            latestRole == "user" -> deriveTitle(latestText)
            else -> deriveTitle(latestText)
        }
        dao.upsertConversation(
            ChatConversationEntity(
                conversationId = conversationId,
                userId = userId,
                title = title,
                previewText = cleanedPreview,
                messageCount = count,
                isSaved = existing?.isSaved ?: true,
                createdAtMillis = existing?.createdAtMillis ?: timestampMillis,
                updatedAtMillis = maxOf(existing?.updatedAtMillis ?: 0L, timestampMillis)
            )
        )
    }

    private fun deriveTitle(rawText: String): String {
        val normalized = rawText.replace('\n', ' ').trim()
        return if (normalized.length <= 44) {
            normalized.ifEmpty { "محادثة مع سنا" }
        } else {
            normalized.take(44).trimEnd() + "…"
        }
    }
}
