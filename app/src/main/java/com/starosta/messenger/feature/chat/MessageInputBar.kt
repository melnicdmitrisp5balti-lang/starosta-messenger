package com.starosta.messenger.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starosta.messenger.data.model.Message

@Composable
fun MessageInputBar(
    replyTo: Message?,
    editingMessage: Message?,
    onSend: (String) -> Unit,
    onTextChanged: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onPickImage: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    // Pre-fill when editing
    LaunchedEffect(editingMessage) {
        text = editingMessage?.text ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Reply/edit header
        if (replyTo != null) {
            ReplyHeader(
                text = replyTo.text,
                label = "Reply",
                onCancel = onCancelReply
            )
        } else if (editingMessage != null) {
            ReplyHeader(
                text = editingMessage.text,
                label = "Edit",
                onCancel = {
                    text = ""
                    onCancelEdit()
                }
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach")
            }

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    if (it.isNotEmpty()) onTextChanged()
                },
                placeholder = { Text("Message...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(modifier = Modifier.width(4.dp))

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun ReplyHeader(text: String, label: String, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
        }
    }
}
