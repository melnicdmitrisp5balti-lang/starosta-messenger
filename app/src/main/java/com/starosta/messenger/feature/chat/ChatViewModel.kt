package com.starosta.messenger.feature.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.core.util.Resource
import com.starosta.messenger.data.model.Chat
import com.starosta.messenger.data.model.Message
import com.starosta.messenger.data.model.User
import com.starosta.messenger.data.repository.AuthRepository
import com.starosta.messenger.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = true,
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val typingUsers: List<String> = emptyList(),
    val error: String? = null,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val replyToMessage: Message? = null,
    val editingMessage: Message? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    private var typingJob: Job? = null

    init {
        loadChat()
        observeMessages()
        observeTyping()
    }

    private fun loadChat() {
        viewModelScope.launch {
            val chat = chatRepository.getChatById(chatId)
            _uiState.update { it.copy(isLoading = false, chat = chat) }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(chatId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                    // Mark unread messages
                    val unread = messages.filter {
                        it.senderId != currentUserId && it.status != "read"
                    }
                    chatRepository.markMessagesAsRead(chatId, unread.map { it.id })
                }
        }
    }

    private fun observeTyping() {
        viewModelScope.launch {
            chatRepository.observeTyping(chatId, currentUserId)
                .catch { /* ignore */ }
                .collect { typingUsers ->
                    _uiState.update { it.copy(typingUsers = typingUsers) }
                }
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val editingMessage = _uiState.value.editingMessage
        val replyTo = _uiState.value.replyToMessage

        if (editingMessage != null) {
            viewModelScope.launch {
                chatRepository.editMessage(chatId, editingMessage.id, text)
                _uiState.update { it.copy(editingMessage = null) }
            }
            return
        }

        viewModelScope.launch {
            chatRepository.sendTextMessage(
                chatId = chatId,
                senderId = currentUserId,
                text = text,
                replyToMessageId = replyTo?.id,
                replyToText = replyTo?.text
            )
            _uiState.update { it.copy(replyToMessage = null) }
            setTyping(false)
        }
    }

    fun sendImageMessage(uri: Uri) {
        viewModelScope.launch {
            chatRepository.sendImageMessage(chatId, currentUserId, uri)
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, message.id)
        }
    }

    fun startEdit(message: Message) {
        _uiState.update { it.copy(editingMessage = message, replyToMessage = null) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingMessage = null) }
    }

    fun replyTo(message: Message) {
        _uiState.update { it.copy(replyToMessage = message, editingMessage = null) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyToMessage = null) }
    }

    fun onTextChanged() {
        setTyping(true)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            setTyping(false)
        }
    }

    private fun setTyping(isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.setTyping(chatId, currentUserId, isTyping)
        }
    }

    fun loadMoreMessages() {
        val firstTimestamp = _uiState.value.messages.firstOrNull()?.createdAt ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val older = chatRepository.loadMoreMessages(chatId, firstTimestamp)
            _uiState.update { state ->
                state.copy(
                    isLoadingMore = false,
                    canLoadMore = older.isNotEmpty(),
                    messages = older + state.messages
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            chatRepository.setTyping(chatId, currentUserId, false)
        }
    }
}
