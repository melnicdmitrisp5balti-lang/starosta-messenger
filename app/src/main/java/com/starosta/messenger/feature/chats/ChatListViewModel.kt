package com.starosta.messenger.feature.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.data.model.Chat
import com.starosta.messenger.data.repository.AuthRepository
import com.starosta.messenger.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadChats()
    }

    private fun loadChats() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            chatRepository.observeChats(userId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { chats ->
                    val sorted = chats.sortedWith(
                        compareByDescending<Chat> { it.pinnedBy.contains(userId) }
                            .thenByDescending { it.lastMessageAt }
                    )
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            chats = sorted,
                            filteredChats = filterChats(sorted, state.searchQuery)
                        )
                    }
                }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredChats = filterChats(state.chats, query)
            )
        }
    }

    private fun filterChats(chats: List<Chat>, query: String): List<Chat> {
        if (query.isBlank()) return chats
        val lowerQuery = query.lowercase()
        return chats.filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.lastMessageText.lowercase().contains(lowerQuery)
        }
    }

    fun togglePinChat(chatId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            chatRepository.togglePinChat(chatId, userId)
        }
    }
}
