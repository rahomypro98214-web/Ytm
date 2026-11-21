package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChatConversationEntity
import com.example.ui.theme.LocalSanaColors
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistorySheet(
    conversations: List<ChatConversationEntity>,
    activeConversationId: String,
    hasActiveMessages: Boolean,
    onSelectConversation: (String) -> Unit,
    onStartNewConversation: () -> Unit,
    onRequestSaveCurrent: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("conversation_history_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = sanaColors.tealDeep
                )
                Text(
                    text = stringResource(R.string.chat_history_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onStartNewConversation()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_conversation_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddComment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.new_conversation),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onRequestSaveCurrent,
                    enabled = hasActiveMessages,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_current_conversation_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdded,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.save_conversation),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = sanaColors.cardBorder)

            if (conversations.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = sanaColors.chatCanvas,
                    border = BorderStroke(1.dp, sanaColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conversation_history_empty")
                ) {
                    Text(
                        text = stringResource(R.string.chat_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .testTag("conversation_history_list"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { "${it.userId}_${it.conversationId}" }) { conv ->
                        val isActive = conv.conversationId == activeConversationId
                        Card(
                            onClick = {
                                onSelectConversation(conv.conversationId)
                                onDismiss()
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) sanaColors.tealTint else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) sanaColors.tealAccent else sanaColors.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_item_${conv.conversationId}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isActive) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = sanaColors.tealDeep,
                                                modifier = Modifier.testTag("active_conversation_badge")
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.conversation_saved_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = sanaColors.heroOnBackground,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (conv.previewText.isNotBlank()) {
                                        Text(
                                            text = conv.previewText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "${conv.messageCount} رسائل · ${dateFormat.format(Date(conv.updatedAtMillis))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = sanaColors.tealDeep
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteConversation(conv.conversationId) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_conversation_${conv.conversationId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_conversation_cd),
                                        tint = sanaColors.errorText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveConversationDialog(
    initialTitle: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleInput by remember(initialTitle) { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.save_conversation_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { if (it.length <= 60) titleInput = it },
                label = { Text(stringResource(R.string.save_conversation_label)) },
                placeholder = { Text(stringResource(R.string.save_conversation_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversation_title_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(titleInput)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_save_conversation_button")
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("save_conversation_dialog")
    )
}
