package com.starosta.messenger.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.starosta.messenger.core.util.TimeUtils
import com.starosta.messenger.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImageMessage(it) }
    }

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // Load more messages when scrolled to top
    val shouldLoadMore by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            firstVisible == 0 && uiState.canLoadMore && !uiState.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.messages.isNotEmpty()) {
            viewModel.loadMoreMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.chat?.title?.ifEmpty { "Chat" } ?: "Chat")
                        if (uiState.typingUsers.isNotEmpty()) {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: show options */ }) {
                        Icon(Icons.Default.MoreVert, "Options")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                replyTo = uiState.replyToMessage,
                editingMessage = uiState.editingMessage,
                onSend = viewModel::sendTextMessage,
                onTextChanged = viewModel::onTextChanged,
                onCancelReply = viewModel::cancelReply,
                onCancelEdit = viewModel::cancelEdit,
                onPickImage = { imagePickerLauncher.launch("image/*") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    val grouped = uiState.messages.groupBy { message ->
                        TimeUtils.formatChatTime(message.createdAt).let { label ->
                            if (label == "Yesterday" || label.length <= 5) label else label
                        }
                    }

                    grouped.forEach { (dateLabel, messages) ->
                        item(key = "date_$dateLabel") {
                            DateHeader(label = dateLabel)
                        }
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == viewModel.currentUserId,
                                onLongClick = { msg ->
                                    if (msg.senderId == viewModel.currentUserId) {
                                        viewModel.startEdit(msg)
                                    }
                                },
                                onReply = viewModel::replyTo,
                                onDelete = { msg ->
                                    if (msg.senderId == viewModel.currentUserId) {
                                        viewModel.deleteMessage(msg)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
