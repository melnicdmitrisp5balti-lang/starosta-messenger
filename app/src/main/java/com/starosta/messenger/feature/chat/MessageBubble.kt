package com.starosta.messenger.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.starosta.messenger.core.ui.theme.BubbleReceived
import com.starosta.messenger.core.ui.theme.BubbleSent
import com.starosta.messenger.core.util.TimeUtils
import com.starosta.messenger.data.model.Message
import com.starosta.messenger.data.model.MessageStatus
import com.starosta.messenger.data.model.MessageType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onLongClick: (Message) -> Unit,
    onReply: (Message) -> Unit,
    onDelete: (Message) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        showMenu = true
                        onLongClick(message)
                    }
                ),
            shape = RoundedCornerShape(
                topStart = if (isOwnMessage) 16.dp else 4.dp,
                topEnd = if (isOwnMessage) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isOwnMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Reply preview
                if (message.replyToText != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = message.replyToText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp),
                            maxLines = 2
                        )
                    }
                }

                // Message content
                when (message.type) {
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        if (message.text.isNotEmpty()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Meta row
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.edited) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edited",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Text(
                        text = TimeUtils.formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Reply") },
                onClick = { onReply(message); showMenu = false }
            )
            if (isOwnMessage) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { onLongClick(message); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { onDelete(message); showMenu = false }
                )
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: String) {
    val tint = when (status) {
        MessageStatus.READ -> MaterialTheme.colorScheme.primary
        MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Icon(
        imageVector = Icons.Default.DoneAll,
        contentDescription = status,
        modifier = Modifier.size(14.dp),
        tint = tint
    )
}
