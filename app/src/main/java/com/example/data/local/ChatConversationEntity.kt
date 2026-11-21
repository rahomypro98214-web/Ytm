package com.example.data.local

import androidx.room.Entity
import androidx.room.Index

const val DEFAULT_CONVERSATION_ID = "default"

@Entity(
    tableName = "chat_conversations",
    primaryKeys = ["userId", "conversationId"],
    indices = [Index(value = ["userId", "updatedAtMillis"])]
)
data class ChatConversationEntity(
    val conversationId: String,
    val userId: String = "local",
    val title: String,
    val previewText: String = "",
    val messageCount: Int = 0,
    val isSaved: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
