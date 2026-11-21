package com.example.data.local

data class ChatHistorySearchMatch(
    val conversationId: String,
    val conversationTitle: String,
    val matchedMessageId: Long,
    val matchedRole: String,
    val matchedText: String,
    val matchedSourceTitle: String?,
    val matchedAtMillis: Long,
    val messageCount: Int,
    val matchedInMessageBody: Boolean = true
)
