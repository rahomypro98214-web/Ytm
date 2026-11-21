package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["userId", "conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val remoteId: String? = null,
    val userId: String = "local",
    val conversationId: String = DEFAULT_CONVERSATION_ID,
    val role: String,
    val text: String,
    val sourceTitle: String? = null,
    val mode: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
